package com.lumina.clinic.booking;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class BookingTokenServiceTest {
    private final BookingTokenService tokens = new BookingTokenService("test-secret-with-at-least-thirty-two-characters");

    @Test void tokenIsStableForIdempotencyAndScopedToOneBooking() {
        UUID id = UUID.randomUUID();
        String token = tokens.issue(id);
        assertThat(token).hasSize(43).isEqualTo(tokens.issue(id));
        assertThatCode(() -> tokens.verify(id, token)).doesNotThrowAnyException();
        assertThatThrownBy(() -> tokens.verify(UUID.randomUUID(), token)).isInstanceOf(DomainException.class);
    }

    @Test void alteredMissingAndRotatedTokensAreRejected() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> tokens.verify(id, null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> tokens.verify(id, tokens.issue(id) + "x")).isInstanceOf(DomainException.class);
        String rotated = new BookingTokenService("rotated-secret-with-at-least-thirty-two-characters").issue(id);
        assertThatThrownBy(() -> tokens.verify(id, rotated)).isInstanceOf(DomainException.class);
    }
}
