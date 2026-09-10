package com.lumina.clinic.booking;

import java.time.*;
import java.util.*;

public record AvailabilityResponse(LocalDate date, String timeZone, List<Slot> slots) {
    public record Slot(OffsetDateTime startsAt, OffsetDateTime endsAt, UUID therapistId, String therapistName) {}
}
