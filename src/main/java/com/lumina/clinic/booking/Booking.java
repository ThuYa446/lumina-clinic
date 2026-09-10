package com.lumina.clinic.booking;

import com.lumina.clinic.catalog.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id private UUID id;
    @Version private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "branch_id") private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "treatment_id") private Treatment treatment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "therapist_id") private Therapist therapist;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id") private Room room;
    @Embedded private ClientDetails client;
    @Column(nullable = false) private boolean member;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(name = "room_occupied_until", nullable = false) private Instant roomOccupiedUntil;
    @Column(name = "therapist_occupied_until", nullable = false) private Instant therapistOccupiedUntil;
    @Column(name = "hold_expires_at") private Instant holdExpiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private BookingStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 30) private PaymentStatus paymentStatus;
    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2) private BigDecimal depositAmount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "idempotency_key", nullable = false, unique = true) private UUID idempotencyKey;
    @Column(name = "request_fingerprint", nullable = false, length = 64) private String requestFingerprint;
    @Column(name = "refund_reference", length = 120) private String refundReference;

    protected Booking() {}

    public static Booking reserve(Branch branch, Treatment treatment, Therapist therapist, Room room,
            ClientDetails client, boolean member, Instant startsAt, Instant now, UUID idempotencyKey, String fingerprint) {
        Booking booking = new Booking();
        booking.id = UUID.randomUUID();
        booking.branch = branch;
        booking.treatment = treatment;
        booking.therapist = therapist;
        booking.room = room;
        booking.client = client;
        booking.member = member;
        booking.startsAt = startsAt;
        booking.endsAt = startsAt.plus(Duration.ofMinutes(treatment.getDurationMinutes()));
        booking.roomOccupiedUntil = booking.endsAt.plus(Duration.ofMinutes(BookingPolicy.ROOM_TURNAROUND_MINUTES));
        booking.therapistOccupiedUntil = booking.endsAt.plus(Duration.ofMinutes(therapist.getTurnaroundMinutes()));
        booking.createdAt = now;
        booking.holdExpiresAt = member ? null : now.plus(Duration.ofMinutes(BookingPolicy.HOLD_MINUTES));
        booking.status = member ? BookingStatus.CONFIRMED : BookingStatus.PENDING_PAYMENT;
        booking.paymentStatus = member ? PaymentStatus.NOT_REQUIRED : PaymentStatus.UNPAID;
        booking.depositAmount = member ? BigDecimal.ZERO.setScale(2) : BookingPolicy.DEPOSIT;
        booking.currency = BookingPolicy.CURRENCY;
        booking.idempotencyKey = idempotencyKey;
        booking.requestFingerprint = fingerprint;
        return booking;
    }

    public boolean expireIfNecessary(Instant now) {
        if (status == BookingStatus.PENDING_PAYMENT && !holdExpiresAt.isAfter(now)) {
            status = BookingStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public void confirmPayment(Instant now) {
        expireIfNecessary(now);
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new DomainException(409, "BOOKING_NOT_PAYABLE", "This booking is no longer awaiting payment.");
        }
        status = BookingStatus.CONFIRMED;
        paymentStatus = PaymentStatus.PAID;
    }

    public void cancel(Instant now, BookingPolicy policy) {
        if (status == BookingStatus.CANCELLED) return;
        expireIfNecessary(now);
        if (status != BookingStatus.PENDING_PAYMENT && status != BookingStatus.CONFIRMED) {
            throw new DomainException(409, "BOOKING_NOT_CANCELLABLE", "This booking cannot be cancelled.");
        }
        if (!policy.cancellationAllowed(startsAt, now)) {
            throw new DomainException(409, "CANCELLATION_WINDOW_CLOSED", "Cancellations require at least 24 hours' notice.");
        }
        status = BookingStatus.CANCELLED;
        if (paymentStatus == PaymentStatus.PAID) paymentStatus = PaymentStatus.REFUND_PENDING;
    }

    public void recordManualRefund(String reference) {
        if (paymentStatus == PaymentStatus.REFUNDED && reference.equals(refundReference)) return;
        if (status != BookingStatus.CANCELLED || paymentStatus != PaymentStatus.REFUND_PENDING) {
            throw new DomainException(409, "REFUND_NOT_PENDING", "Only a cancelled booking awaiting a refund can be marked refunded.");
        }
        refundReference = reference;
        paymentStatus = PaymentStatus.REFUNDED;
    }

    public UUID getId() { return id; }
    public Branch getBranch() { return branch; }
    public Treatment getTreatment() { return treatment; }
    public Therapist getTherapist() { return therapist; }
    public Room getRoom() { return room; }
    public ClientDetails getClient() { return client; }
    public boolean isMember() { return member; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getRoomOccupiedUntil() { return roomOccupiedUntil; }
    public Instant getTherapistOccupiedUntil() { return therapistOccupiedUntil; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public BookingStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public String getCurrency() { return currency; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public String getRefundReference() { return refundReference; }
}
