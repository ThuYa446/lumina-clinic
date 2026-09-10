package com.lumina.clinic.booking;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DemoMembershipVerifierTest {
    @Test void membershipRequiresBothServerCodeAndKnownMemberEmail() {
        DemoMembershipVerifier verifier = new DemoMembershipVerifier("secret-member-code", "member@example.com");
        assertThat(verifier.verify("member@example.com", "secret-member-code")).isTrue();
        assertThat(verifier.verify("MEMBER@example.com", "secret-member-code")).isTrue();
        assertThatThrownBy(() -> verifier.verify("stranger@example.com", "secret-member-code")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> verifier.verify("member@example.com", "wrong")).isInstanceOf(DomainException.class);
    }

    @Test void blankServerCodeDisablesMembershipAndAbsentClientCodeIsNonMember() {
        DemoMembershipVerifier verifier = new DemoMembershipVerifier("", "member@example.com");
        assertThat(verifier.verify("anyone@example.com", null)).isFalse();
        assertThat(verifier.verify("member@example.com", " ")).isFalse();
        assertThatThrownBy(() -> verifier.verify("member@example.com", "anything")).isInstanceOf(DomainException.class);
    }
}
