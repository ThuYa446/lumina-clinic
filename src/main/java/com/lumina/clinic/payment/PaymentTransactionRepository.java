package com.lumina.clinic.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByIdempotencyKey(UUID idempotencyKey);
    @org.springframework.data.jpa.repository.Query("select p from PaymentTransaction p where p.booking.id = :bookingId")
    Optional<PaymentTransaction> findByBookingId(@org.springframework.data.repository.query.Param("bookingId") UUID bookingId);
}
