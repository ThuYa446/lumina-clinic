package com.lumina.clinic.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

/** Authentication and role checks are applied centrally to every /api/staff route. */
@RestController
@RequestMapping("/api/staff/bookings")
public class StaffBookingController {
    private final BookingService bookings;

    public StaffBookingController(BookingService bookings) { this.bookings = bookings; }

    @GetMapping
    public StaffBookingResponse list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID branchId) {
        return bookings.staffDay(date, branchId);
    }

    @PostMapping("/{id}/refund")
    public StaffBookingResponse.Item refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return bookings.recordRefund(id, request.reference());
    }

    public record RefundRequest(@NotBlank @Size(max = 120) String reference) {}
}
