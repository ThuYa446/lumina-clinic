package com.lumina.clinic.booking;

import com.lumina.clinic.catalog.*;
import com.lumina.clinic.payment.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@Transactional(noRollbackFor = DomainException.class)
public class BookingService {
    private final BranchRepository branches;
    private final TreatmentRepository treatments;
    private final TherapistRepository therapists;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final PaymentTransactionRepository payments;
    private final BookingPolicy policy;
    private final ResourceAvailability availability;
    private final MembershipVerifier memberships;
    private final BookingTokenService tokens;
    private final PaymentGateway gateway;
    private final Clock clock;

    public BookingService(BranchRepository branches, TreatmentRepository treatments, TherapistRepository therapists,
            RoomRepository rooms, BookingRepository bookings, PaymentTransactionRepository payments,
            BookingPolicy policy, ResourceAvailability availability, MembershipVerifier memberships,
            BookingTokenService tokens, PaymentGateway gateway, Clock clock) {
        this.branches = branches;
        this.treatments = treatments;
        this.therapists = therapists;
        this.rooms = rooms;
        this.bookings = bookings;
        this.payments = payments;
        this.policy = policy;
        this.availability = availability;
        this.memberships = memberships;
        this.tokens = tokens;
        this.gateway = gateway;
        this.clock = clock;
    }

    public AvailabilityResponse availability(UUID branchId, UUID treatmentId, LocalDate date, UUID therapistId) {
        policy.validateDate(date, clock.instant());
        lockBranch(branchId);
        Instant now = clock.instant();
        expireHolds(branchId, now);
        Treatment treatment = requireTreatment(treatmentId);
        List<Therapist> eligible = therapists.findByBranchIdOrderByName(branchId).stream()
            .filter(t -> t.isQualifiedFor(treatmentId))
            .filter(t -> therapistId == null || t.getId().equals(therapistId)).toList();
        if (therapistId != null && eligible.isEmpty()) {
            throw new DomainException(422, "THERAPIST_UNAVAILABLE", "This therapist does not offer the selected treatment at this branch.");
        }
        if (!policy.isOpen(date)) return new AvailabilityResponse(date, BookingPolicy.ZONE.getId(), List.of());
        List<Room> branchRooms = rooms.findByBranchIdOrderByName(branchId);
        List<Booking> reservations = reservationsForDate(branchId, date);
        List<AvailabilityResponse.Slot> slots = new ArrayList<>();
        for (LocalTime time = BookingPolicy.OPENING; time.isBefore(BookingPolicy.CLOSING); time = time.plusMinutes(BookingPolicy.SLOT_MINUTES)) {
            Instant startsAt = date.atTime(time).atZone(BookingPolicy.ZONE).toInstant();
            if (!startsAt.isAfter(now)) continue;
            for (Therapist therapist : eligible) {
                if (policy.fitsBusinessHours(startsAt, treatment.getDurationMinutes(), therapist.getTurnaroundMinutes())
                    && availability.findRoom(branchRooms, reservations, therapist, startsAt, treatment.getDurationMinutes()).isPresent()) {
                    slots.add(new AvailabilityResponse.Slot(local(startsAt),
                        local(startsAt.plus(Duration.ofMinutes(treatment.getDurationMinutes()))), therapist.getId(), therapist.getName()));
                }
            }
        }
        return new AvailabilityResponse(date, BookingPolicy.ZONE.getId(), List.copyOf(slots));
    }

    public BookingResponse create(CreateBookingRequest request, UUID idempotencyKey) {
        Branch branch = lockBranch(request.branchId());
        Instant now = clock.instant();
        expireHolds(branch.getId(), now);
        String fingerprint = fingerprint(request);
        Optional<Booking> existing = bookings.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                throw new DomainException(409, "IDEMPOTENCY_KEY_REUSED", "This request key was already used for a different booking.");
            }
            return response(existing.get(), now);
        }
        Treatment treatment = requireTreatment(request.treatmentId());
        Therapist therapist = therapists.findById(request.therapistId())
            .orElseThrow(() -> new DomainException(404, "THERAPIST_NOT_FOUND", "Therapist not found."));
        if (!therapist.getBranch().getId().equals(branch.getId()) || !therapist.isQualifiedFor(treatment.getId())) {
            throw new DomainException(422, "THERAPIST_UNAVAILABLE", "This therapist does not offer the selected treatment at this branch.");
        }
        Instant startsAt = request.startsAt().toInstant();
        policy.validateStart(startsAt, treatment.getDurationMinutes(), therapist.getTurnaroundMinutes(), now);
        ClientDetails client = new ClientDetails(request.client().name(), request.client().email(), request.client().phone());
        boolean member = memberships.verify(client.getEmail(), request.membershipCode());
        Room room = availability.findRoom(rooms.findByBranchIdOrderByName(branch.getId()),
                reservationsForDate(branch.getId(), startsAt.atZone(BookingPolicy.ZONE).toLocalDate()),
                therapist, startsAt, treatment.getDurationMinutes())
            .orElseThrow(() -> new DomainException(409, "SLOT_UNAVAILABLE", "This time was just reserved. Please choose another appointment."));
        Booking booking = Booking.reserve(branch, treatment, therapist, room, client, member, startsAt, now, idempotencyKey, fingerprint);
        bookings.saveAndFlush(booking);
        return response(booking, now);
    }

    public BookingResponse get(UUID bookingId, String token) {
        Booking booking = requireManagedBooking(bookingId, token);
        return response(booking, clock.instant());
    }

    public PaymentResponse pay(UUID bookingId, String token, UUID idempotencyKey) {
        Booking booking = requireManagedBooking(bookingId, token);
        Instant now = clock.instant();
        Optional<PaymentTransaction> byKey = payments.findByIdempotencyKey(idempotencyKey);
        if (byKey.isPresent() && !byKey.get().getBookingId().equals(bookingId)) {
            throw new DomainException(409, "IDEMPOTENCY_KEY_REUSED", "This payment request key was already used for another booking.");
        }
        Optional<PaymentTransaction> existing = payments.findByBookingId(bookingId);
        if (existing.isPresent()) {
            return new PaymentResponse(response(booking, now), existing.get().getReference(), existing.get().getMode());
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new DomainException(409, "BOOKING_NOT_PAYABLE", "This booking is no longer awaiting payment.");
        }
        PaymentGateway.Receipt receipt = gateway.collectDeposit(bookingId, booking.getDepositAmount(), booking.getCurrency(), idempotencyKey);
        booking.confirmPayment(clock.instant());
        PaymentTransaction transaction = new PaymentTransaction(booking, idempotencyKey, receipt, now);
        payments.saveAndFlush(transaction);
        return new PaymentResponse(response(booking, clock.instant()), receipt.reference(), receipt.mode());
    }

    public BookingResponse cancel(UUID bookingId, String token) {
        Booking booking = requireManagedBooking(bookingId, token);
        Instant now = clock.instant();
        booking.cancel(now, policy);
        bookings.flush();
        return response(booking, now);
    }

    public StaffBookingResponse staffDay(LocalDate date, UUID branchId) {
        // Consistent lock ordering also keeps a staff request across branches deadlock-free.
        List<UUID> branchIds = branchId == null ? branches.findAll().stream().map(Branch::getId).sorted().toList() : List.of(branchId);
        for (UUID id : branchIds) {
            lockBranch(id);
            expireHolds(id, clock.instant());
        }
        return new StaffBookingResponse(bookings.findStaffDay(branchId, date.atStartOfDay(BookingPolicy.ZONE).toInstant(),
            date.plusDays(1).atStartOfDay(BookingPolicy.ZONE).toInstant()).stream().map(this::staffItem).toList());
    }

    public StaffBookingResponse.Item recordRefund(UUID bookingId, String reference) {
        Booking booking = requireLockedBooking(bookingId);
        booking.recordManualRefund(reference.strip());
        bookings.flush();
        return staffItem(booking);
    }

    private Booking requireManagedBooking(UUID bookingId, String token) {
        tokens.verify(bookingId, token);
        return requireLockedBooking(bookingId);
    }

    private Booking requireLockedBooking(UUID bookingId) {
        // Fetch only the foreign key before locking, avoiding a stale entity in the persistence context.
        UUID branchId = bookings.findBranchIdById(bookingId)
            .orElseThrow(() -> new DomainException(404, "BOOKING_NOT_FOUND", "Booking not found or the management link is invalid."));
        lockBranch(branchId);
        expireHolds(branchId, clock.instant());
        return bookings.findById(bookingId)
            .orElseThrow(() -> new DomainException(404, "BOOKING_NOT_FOUND", "Booking not found or the management link is invalid."));
    }

    private Branch lockBranch(UUID branchId) {
        return branches.findLockedById(branchId)
            .orElseThrow(() -> new DomainException(404, "BRANCH_NOT_FOUND", "Branch not found."));
    }

    private Treatment requireTreatment(UUID treatmentId) {
        return treatments.findById(treatmentId)
            .orElseThrow(() -> new DomainException(404, "TREATMENT_NOT_FOUND", "Treatment not found."));
    }

    private void expireHolds(UUID branchId, Instant now) {
        List<Booking> expired = bookings.findExpiredHolds(branchId, now);
        expired.forEach(booking -> booking.expireIfNecessary(now));
        // Release exclusion-constraint ranges before inserting a replacement reservation.
        if (!expired.isEmpty()) bookings.flush();
    }

    private List<Booking> reservationsForDate(UUID branchId, LocalDate date) {
        return bookings.findActiveInWindow(branchId, date.atStartOfDay(BookingPolicy.ZONE).toInstant(),
            date.plusDays(1).atStartOfDay(BookingPolicy.ZONE).toInstant());
    }

    private BookingResponse response(Booking booking, Instant now) {
        boolean active = booking.getStatus() == BookingStatus.PENDING_PAYMENT || booking.getStatus() == BookingStatus.CONFIRMED;
        return new BookingResponse(booking.getId(), tokens.issue(booking.getId()), booking.getStatus(),
            booking.getBranch().getName(), booking.getTreatment().getName(), booking.getTherapist().getName(),
            local(booking.getStartsAt()), local(booking.getEndsAt()), local(booking.getHoldExpiresAt()),
            booking.getDepositAmount(), booking.getCurrency(), booking.getClient().getName(), booking.isMember(),
            booking.getPaymentStatus(), active && policy.cancellationAllowed(booking.getStartsAt(), now));
    }

    private StaffBookingResponse.Item staffItem(Booking booking) {
        return new StaffBookingResponse.Item(booking.getId(), booking.getClient().getName(), booking.getBranch().getName(),
            booking.getTreatment().getName(), booking.getTherapist().getName(), booking.getRoom().getName(),
            local(booking.getStartsAt()), local(booking.getEndsAt()), booking.getStatus(), booking.getPaymentStatus(),
            booking.getDepositAmount(), booking.getCurrency(), booking.isMember(), booking.getRefundReference());
    }

    private static OffsetDateTime local(Instant instant) { return instant == null ? null : instant.atZone(BookingPolicy.ZONE).toOffsetDateTime(); }

    private static String fingerprint(CreateBookingRequest request) {
        List<String> fields = List.of(request.branchId().toString(), request.treatmentId().toString(), request.therapistId().toString(),
            request.startsAt().toInstant().toString(), request.client().name().strip(),
            request.client().email().strip().toLowerCase(Locale.ROOT), request.client().phone().strip(),
            request.membershipCode() == null ? "" : request.membershipCode().strip());
        String canonical = fields.stream().map(value -> value.length() + ":" + value).reduce("", String::concat);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
