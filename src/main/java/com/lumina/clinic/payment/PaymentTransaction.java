package com.lumina.clinic.payment;

import com.lumina.clinic.booking.Booking;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", unique = true) private Booking booking;
    @Column(name = "idempotency_key", nullable = false, unique = true) private UUID idempotencyKey;
    @Column(nullable = false, unique = true, length = 120) private String reference;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 16) private String mode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PaymentTransaction() {}
    public PaymentTransaction(Booking booking, UUID idempotencyKey, PaymentGateway.Receipt receipt, Instant now) {
        this.id = UUID.randomUUID();
        this.booking = booking;
        this.idempotencyKey = idempotencyKey;
        this.reference = receipt.reference();
        this.amount = booking.getDepositAmount();
        this.currency = booking.getCurrency();
        this.mode = receipt.mode();
        this.createdAt = now;
    }
    public UUID getBookingId() { return booking.getId(); }
    public String getReference() { return reference; }
    public String getMode() { return mode; }
}
