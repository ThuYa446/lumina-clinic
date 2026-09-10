package com.lumina.clinic.payment;

import com.lumina.clinic.booking.BookingResponse;

public record PaymentResponse(BookingResponse booking, String paymentReference, String mode) {}
