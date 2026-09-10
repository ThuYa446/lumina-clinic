package com.lumina.clinic.booking;

import jakarta.persistence.*;

@Embeddable
public class ClientDetails {
    @Column(name = "client_name", nullable = false, length = 120) private String name;
    @Column(name = "client_email", nullable = false, length = 254) private String email;
    @Column(name = "client_phone", nullable = false, length = 30) private String phone;

    protected ClientDetails() {}
    public ClientDetails(String name, String email, String phone) {
        this.name = name.strip();
        this.email = email.strip().toLowerCase(java.util.Locale.ROOT);
        this.phone = phone.strip();
    }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
