package com.lumina.clinic.booking;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class BookingPolicyTest {
    private final BookingPolicy policy = new BookingPolicy();
    private final Instant now = Instant.parse("2026-09-10T01:30:00Z"); // Thursday, 08:00 Myanmar

    @Test void cancellationAllowsExactlyTwentyFourHoursButNotOneNanosecondLess() {
        assertThat(policy.cancellationAllowed(now.plus(Duration.ofHours(24)), now)).isTrue();
        assertThat(policy.cancellationAllowed(now.plus(Duration.ofHours(24)).minusNanos(1), now)).isFalse();
    }

    @Test void businessHoursIncludeRoomCleanupEvenForSeniorTherapists() {
        assertThat(policy.fitsBusinessHours(Instant.parse("2026-09-10T10:30:00Z"), 30, 0)).isTrue(); // 17:00
        assertThat(policy.fitsBusinessHours(Instant.parse("2026-09-10T11:00:00Z"), 30, 0)).isFalse(); // 17:30 + cleanup
        assertThat(policy.fitsBusinessHours(Instant.parse("2026-09-10T02:00:00Z"), 30, 0)).isFalse(); // 08:30
    }

    @Test void latestReadyTimeMayEqualClosingButMayNotExceedIt() {
        assertThat(policy.fitsBusinessHours(Instant.parse("2026-09-10T10:45:00Z"), 30, 15)).isTrue(); // 17:15 + 45 minutes
        assertThat(policy.fitsBusinessHours(Instant.parse("2026-09-10T10:45:01Z"), 30, 15)).isFalse();
    }

    @Test void startsAreOnlyOnTheHourOrHalfHour() {
        assertThatCode(() -> policy.validateStart(Instant.parse("2026-09-10T02:30:00Z"), 30, 15, now)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validateStart(Instant.parse("2026-09-10T02:15:00Z"), 30, 15, now))
            .isInstanceOf(DomainException.class).hasMessageContaining("listed appointment");
        assertThatThrownBy(() -> policy.validateStart(Instant.parse("2026-09-10T02:30:01Z"), 30, 15, now))
            .isInstanceOf(DomainException.class);
    }

    @Test void sundaysAreClosed() {
        assertThat(policy.isOpen(LocalDate.of(2026, 9, 13))).isFalse();
        assertThatThrownBy(() -> policy.validateStart(Instant.parse("2026-09-13T02:00:00Z"), 30, 15, now))
            .isInstanceOf(DomainException.class);
    }

    @Test void horizonUsesMyanmarMidnightAndIncludesNinetiethDay() {
        Instant midnightMyanmar = Instant.parse("2026-09-09T17:30:00Z");
        assertThatCode(() -> policy.validateDate(LocalDate.of(2026, 9, 9), midnightMyanmar.minusSeconds(1))).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateDate(LocalDate.of(2026, 9, 10), midnightMyanmar)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateDate(LocalDate.of(2026, 9, 10).plusDays(90), now)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validateDate(LocalDate.of(2026, 9, 9), midnightMyanmar)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> policy.validateDate(LocalDate.of(2026, 9, 10).plusDays(91), now)).isInstanceOf(DomainException.class);
    }

    @Test void elapsedOrCurrentStartIsRejected() {
        assertThatThrownBy(() -> policy.validateStart(now, 30, 15, now)).isInstanceOf(DomainException.class).hasMessageContaining("passed");
    }

    @Test void halfOpenRangesAllowAdjacencyInBothOrdersButRejectPartialOrContainingOverlap() {
        Instant end = now.plusSeconds(60);
        assertThat(BookingPolicy.overlaps(now, end, end, end.plusSeconds(60))).isFalse();
        assertThat(BookingPolicy.overlaps(end, end.plusSeconds(60), now, end)).isFalse();
        assertThat(BookingPolicy.overlaps(now, end, end.minusNanos(1), end.plusSeconds(60))).isTrue();
        assertThat(BookingPolicy.overlaps(now, end, now.minusSeconds(1), end.plusSeconds(1))).isTrue();
    }
}
