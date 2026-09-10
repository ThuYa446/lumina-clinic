package com.lumina.clinic.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "treatments")
public class Treatment {
    @Id private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String description;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;

    protected Treatment() {}
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getPrice() { return price; }
}
