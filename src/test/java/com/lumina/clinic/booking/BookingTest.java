package com.lumina.clinic.booking;

import com.lumina.clinic.catalog.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingTest {
    private final Instant createdAt = Instant.parse("2026-09-10T01:00:00Z");
    private final Instant startsAt = createdAt.plus(Duration.ofDays(3));
    private final BookingPolicy policy = new BookingPolicy();

    @Test void nonMemberHasTemporaryHoldAndServerCalculatedDeposit() {
        Booking booking = reserve(false, 15);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(booking.getDepositAmount()).isEqualByComparingTo("300.00");
        assertThat(booking.getHoldExpiresAt()).isEqualTo(createdAt.plusSeconds(900));
        assertThat(booking.getEndsAt()).isEqualTo(startsAt.plusSeconds(3600));
        assertThat(booking.getRoomOccupiedUntil()).isEqualTo(startsAt.plusSeconds(4500));
    }

    @Test void verifiedMemberIsImmediatelyConfirmedWithoutDepositOrHold() {
        Booking booking = reserve(true, 15);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_REQUIRED);
        assertThat(booking.getDepositAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(booking.getHoldExpiresAt()).isNull();
        assertThat(booking.expireIfNecessary(createdAt.plusSeconds(900))).isFalse();
    }

    @Test void seniorTherapistHasNoTurnaroundButRoomStillNeedsCleaning() {
        Booking booking = reserve(true, 0);
        assertThat(booking.getTherapistOccupiedUntil()).isEqualTo(booking.getEndsAt());
        assertThat(booking.getRoomOccupiedUntil()).isEqualTo(booking.getEndsAt().plusSeconds(900));
    }

    @Test void holdExpiresExactlyAtDeadlineAndCanNeverBePaidAfterwards() {
        Booking booking = reserve(false, 15);
        assertThat(booking.expireIfNecessary(createdAt.plusSeconds(900).minusNanos(1))).isFalse();
        assertThat(booking.expireIfNecessary(createdAt.plusSeconds(900))).isTrue();
        assertThat(booking.expireIfNecessary(createdAt.plusSeconds(901))).isFalse();
        assertThatThrownBy(() -> booking.confirmPayment(createdAt.plusSeconds(900))).isInstanceOf(DomainException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test void paidBookingNeverExpires() {
        Booking booking = reserve(false, 15);
        booking.confirmPayment(createdAt.plusSeconds(899));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(booking.expireIfNecessary(createdAt.plusSeconds(1000))).isFalse();
    }

    @Test void paidCancellationQueuesManualRefundAndRepeatedCancellationIsHarmless() {
        Booking booking = reserve(false, 15);
        booking.confirmPayment(createdAt.plusSeconds(20));
        booking.cancel(startsAt.minus(Duration.ofHours(24)), policy);
        booking.cancel(startsAt, policy);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        booking.recordManualRefund("DEMO-MANUAL-001");
        booking.recordManualRefund("DEMO-MANUAL-001");
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(booking.getRefundReference()).isEqualTo("DEMO-MANUAL-001");
        assertThatThrownBy(() -> booking.recordManualRefund("different-reference")).isInstanceOf(DomainException.class);
    }

    @Test void cancellationInsideTwentyFourHoursPreservesReservation() {
        Booking booking = reserve(true, 15);
        assertThatThrownBy(() -> booking.cancel(startsAt.minus(Duration.ofHours(24)).plusNanos(1), policy))
            .isInstanceOf(DomainException.class).hasMessageContaining("24 hours");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test void memberCancellationDoesNotCreateRefundAndUnpaidBookingsCannotBeRefunded() {
        Booking booking = reserve(true, 15);
        booking.cancel(createdAt, policy);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_REQUIRED);
        assertThatThrownBy(() -> booking.recordManualRefund("invalid")).isInstanceOf(DomainException.class);
    }

    @Test void unpaidCancellationReleasesHoldWithoutRefund() {
        Booking booking = reserve(false, 15);
        booking.cancel(createdAt.plusSeconds(1), policy);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    private Booking reserve(boolean member, int turnaroundMinutes) {
        Treatment treatment = mock(Treatment.class);
        when(treatment.getDurationMinutes()).thenReturn(60);
        Therapist therapist = mock(Therapist.class);
        when(therapist.getTurnaroundMinutes()).thenReturn(turnaroundMinutes);
        return Booking.reserve(mock(Branch.class), treatment, therapist, mock(Room.class),
            new ClientDetails("Test Client", "TEST@example.com", "+95 9 123 456 789"), member,
            startsAt, createdAt, UUID.randomUUID(), "fingerprint");
    }
}
