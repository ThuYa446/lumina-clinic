package com.lumina.clinic.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingRequest(
    @NotNull UUID branchId,
    @NotNull UUID treatmentId,
    @NotNull UUID therapistId,
    @NotNull OffsetDateTime startsAt,
    @NotNull @Valid ClientRequest client,
    @Size(max = 120) String membershipCode
) {
    public record ClientRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "[+0-9() .-]{7,30}", message = "must be a valid phone number") String phone
    ) {}
}
