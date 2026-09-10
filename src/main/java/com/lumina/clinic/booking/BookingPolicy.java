package com.lumina.clinic.booking;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.*;

/** Business time is explicit and independent of the server's default time zone. */
@Component
public class BookingPolicy {
    public static final ZoneId ZONE = ZoneId.of("Asia/Yangon");
    public static final LocalTime OPENING = LocalTime.of(9, 0);
    public static final LocalTime CLOSING = LocalTime.of(18, 0);
    public static final BigDecimal DEPOSIT = new BigDecimal("300.00");
    public static final String CURRENCY = "MMK";
    public static final int HOLD_MINUTES = 15;
    public static final int ROOM_TURNAROUND_MINUTES = 15;
    public static final int SLOT_MINUTES = 30;
    public static final int MAX_ADVANCE_DAYS = 90;

    public void validateDate(LocalDate date, Instant now) {
        LocalDate today = now.atZone(ZONE).toLocalDate();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new DomainException(400, "DATE_OUT_OF_RANGE", "Choose a date from today through the next 90 days.");
        }
    }

    public boolean isOpen(LocalDate date) { return date.getDayOfWeek() != DayOfWeek.SUNDAY; }

    public void validateStart(Instant startsAt, int durationMinutes, int therapistTurnaroundMinutes, Instant now) {
        ZonedDateTime local = startsAt.atZone(ZONE);
        validateDate(local.toLocalDate(), now);
        if (!startsAt.isAfter(now)) {
            throw new DomainException(409, "SLOT_IN_PAST", "This appointment time has already passed.");
        }
        if (!isOpen(local.toLocalDate()) || local.getMinute() % SLOT_MINUTES != 0 || local.getSecond() != 0 || local.getNano() != 0
            || !fitsBusinessHours(startsAt, durationMinutes, therapistTurnaroundMinutes)) {
            throw new DomainException(400, "OUTSIDE_BUSINESS_HOURS", "Choose a listed appointment time, Monday to Saturday, 09:00–18:00 Myanmar time (UTC+06:30).");
        }
    }

    public boolean fitsBusinessHours(Instant startsAt, int durationMinutes, int therapistTurnaroundMinutes) {
        ZonedDateTime local = startsAt.atZone(ZONE);
        Instant opening = local.toLocalDate().atTime(OPENING).atZone(ZONE).toInstant();
        Instant closing = local.toLocalDate().atTime(CLOSING).atZone(ZONE).toInstant();
        Instant fullyReady = startsAt.plus(Duration.ofMinutes(durationMinutes + Math.max(ROOM_TURNAROUND_MINUTES, therapistTurnaroundMinutes)));
        return !startsAt.isBefore(opening) && !fullyReady.isAfter(closing);
    }

    public boolean cancellationAllowed(Instant startsAt, Instant now) {
        return !startsAt.isBefore(now.plus(Duration.ofHours(24)));
    }

    /** Half-open ranges permit adjacent reservations once cleanup has completed. */
    public static boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }
}
