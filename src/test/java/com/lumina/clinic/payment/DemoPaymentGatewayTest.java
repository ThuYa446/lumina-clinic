package com.lumina.clinic.payment;

import com.lumina.clinic.booking.DomainException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DemoPaymentGatewayTest {
    @Test void retriesForOneBookingProduceOneExplicitlyDemoReference() {
        DemoPaymentGateway gateway = new DemoPaymentGateway(true);
        UUID bookingId = UUID.randomUUID();
        PaymentGateway.Receipt first = gateway.collectDeposit(bookingId, new BigDecimal("300"), "MMK", UUID.randomUUID());
        PaymentGateway.Receipt retry = gateway.collectDeposit(bookingId, new BigDecimal("300"), "MMK", UUID.randomUUID());
        assertThat(first).isEqualTo(retry);
        assertThat(first.reference()).startsWith("DEMO-");
        assertThat(first.mode()).isEqualTo("DEMO");
    }

    @Test void disabledAdapterDoesNotProducePaymentReceipt() {
        assertThatThrownBy(() -> new DemoPaymentGateway(false).collectDeposit(UUID.randomUUID(), new BigDecimal("300"), "MMK", UUID.randomUUID()))
            .isInstanceOf(DomainException.class).hasMessageContaining("disabled");
    }
}
