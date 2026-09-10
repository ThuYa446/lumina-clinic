package com.lumina.clinic.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(UUID id, String managementToken, BookingStatus status,
    String branchName, String treatmentName, String therapistName,
    OffsetDateTime startsAt, OffsetDateTime endsAt, OffsetDateTime holdExpiresAt,
    BigDecimal depositAmount, String currency, String clientName, boolean member,
    PaymentStatus paymentStatus, boolean cancellationAllowed) {}
