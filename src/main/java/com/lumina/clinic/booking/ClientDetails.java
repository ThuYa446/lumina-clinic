package com.lumina.clinic.booking;

import jakarta.persistence.*;
import java.time.LocalDate;

@Embeddable
public class ClientDetails {
    @Column(name = "client_name", nullable = false, length = 120) private String name;
    @Column(name = "client_email", nullable = false, length = 254) private String email;
    @Column(name = "client_phone", nullable = false, length = 30) private String phone;
    // Nullable only for bookings created before identity details were collected.
    @Column(name = "client_id_number", length = 64) private String idNumber;
    @Column(name = "client_date_of_birth") private LocalDate dateOfBirth;

    protected ClientDetails() {}
    public ClientDetails(String name, String email, String phone, String idNumber, LocalDate dateOfBirth) {
        this.name = name.strip();
        this.email = email.strip().toLowerCase(java.util.Locale.ROOT);
        this.phone = phone.strip();
        this.idNumber = idNumber.strip();
        this.dateOfBirth = dateOfBirth;
    }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getIdNumber() { return idNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
}
