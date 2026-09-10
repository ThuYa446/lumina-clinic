package com.lumina.clinic.booking;

import com.lumina.clinic.payment.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @GetMapping("/availability")
    public AvailabilityResponse availability(@RequestParam UUID branchId, @RequestParam UUID treatmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID therapistId) {
        return bookings.availability(branchId, treatmentId, date, therapistId);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody CreateBookingRequest request) {
        return bookings.create(request, key);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse get(@PathVariable UUID id, @RequestHeader("X-Booking-Token") String token) {
        return bookings.get(id, token);
    }

    @PostMapping("/bookings/{id}/payments")
    public PaymentResponse pay(@PathVariable UUID id, @RequestHeader("X-Booking-Token") String token,
            @RequestHeader("Idempotency-Key") UUID key) {
        return bookings.pay(id, token, key);
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id, @RequestHeader("X-Booking-Token") String token) {
        return bookings.cancel(id, token);
    }
}
