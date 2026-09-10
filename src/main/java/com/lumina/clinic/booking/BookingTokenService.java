package com.lumina.clinic.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Deterministic capability tokens permit safe create retries without storing bearer tokens. */
@Component
public class BookingTokenService {
    private final byte[] secret;

    public BookingTokenService(@Value("${app.booking.token-secret:local-development-only-change-this-32-character-secret}") String secret) {
        if (secret.length() < 32) throw new IllegalArgumentException("app.booking.token-secret must contain at least 32 characters");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(UUID bookingId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("booking:" + bookingId).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    public void verify(UUID bookingId, String suppliedToken) {
        if (suppliedToken == null || !MessageDigest.isEqual(issue(bookingId).getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new DomainException(404, "BOOKING_NOT_FOUND", "Booking not found or the management link is invalid.");
        }
    }
}
