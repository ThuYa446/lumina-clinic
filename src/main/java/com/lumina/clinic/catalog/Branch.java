package com.lumina.clinic.catalog;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "branches")
public class Branch {
    @Id private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String address;

    protected Branch() {}
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
}
