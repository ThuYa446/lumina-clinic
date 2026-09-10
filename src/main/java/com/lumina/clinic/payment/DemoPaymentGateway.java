package com.lumina.clinic.payment;

import com.lumina.clinic.booking.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** A simulation: this adapter never accepts card details or moves money. */
@Component
public class DemoPaymentGateway implements PaymentGateway {
    private final boolean enabled;

    public DemoPaymentGateway(@Value("${app.payments.demo-enabled:true}") boolean enabled) { this.enabled = enabled; }

    @Override
    public Receipt collectDeposit(UUID bookingId, BigDecimal amount, String currency, UUID idempotencyKey) {
        if (!enabled) throw new DomainException(503, "PAYMENTS_DISABLED", "Demo payments are disabled. Contact the clinic to arrange your deposit.");
        return new Receipt("DEMO-" + UUID.nameUUIDFromBytes(("deposit:" + bookingId).getBytes(StandardCharsets.UTF_8)), "DEMO");
    }
}
