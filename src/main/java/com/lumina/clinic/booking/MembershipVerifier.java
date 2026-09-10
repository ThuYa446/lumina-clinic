package com.lumina.clinic.booking;

public interface MembershipVerifier {
    boolean verify(String email, String membershipCode);
}
