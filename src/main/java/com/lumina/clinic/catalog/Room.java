package com.lumina.clinic.catalog;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id") private Branch branch;
    @Column(nullable = false) private String name;

    protected Room() {}
    public UUID getId() { return id; }
    public Branch getBranch() { return branch; }
    public String getName() { return name; }
}
