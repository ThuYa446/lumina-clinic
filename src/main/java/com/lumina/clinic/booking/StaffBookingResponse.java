package com.lumina.clinic.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public record StaffBookingResponse(List<Item> items) {
    public record Item(UUID id, String clientName, String branchName, String treatmentName,
        String therapistName, String roomName, OffsetDateTime startsAt, OffsetDateTime endsAt,
        BookingStatus status, PaymentStatus paymentStatus, BigDecimal depositAmount,
        String currency, boolean member, String refundReference) {}
}
