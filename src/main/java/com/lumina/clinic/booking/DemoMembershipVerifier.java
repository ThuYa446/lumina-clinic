package com.lumina.clinic.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Replace this adapter with a real member directory before enrolling real members. */
@Component
public class DemoMembershipVerifier implements MembershipVerifier {
    private final String configuredCode;
    private final String configuredEmail;

    public DemoMembershipVerifier(@Value("${app.membership.code:}") String configuredCode,
            @Value("${app.membership.email:member@example.com}") String configuredEmail) {
        this.configuredCode = configuredCode;
        this.configuredEmail = configuredEmail.strip();
    }

    @Override
    public boolean verify(String email, String membershipCode) {
        if (membershipCode == null || membershipCode.isBlank()) return false;
        if (configuredCode.isBlank() || configuredEmail.isBlank() || !configuredEmail.equalsIgnoreCase(email.strip())
            || !MessageDigest.isEqual(configuredCode.getBytes(StandardCharsets.UTF_8), membershipCode.strip().getBytes(StandardCharsets.UTF_8))) {
            throw new DomainException(422, "MEMBERSHIP_INVALID", "The demo membership details could not be verified.");
        }
        return true;
    }
}
