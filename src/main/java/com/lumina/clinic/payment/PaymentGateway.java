package com.lumina.clinic.payment;

import java.math.BigDecimal;
import java.util.UUID;

/** Payment port. An actual provider must implement capture and verify payment server-side. */
public interface PaymentGateway {
    Receipt collectDeposit(UUID bookingId, BigDecimal amount, String currency, UUID idempotencyKey);
    record Receipt(String reference, String mode) {}
}
