package com.lumina.clinic.catalog;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "therapists")
public class Therapist {
    @Id private UUID id;
    @Column(nullable = false) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id") private Branch branch;
    @Column(name = "turnaround_minutes", nullable = false) private int turnaroundMinutes;
    @ManyToMany
    @JoinTable(name = "therapist_treatments", joinColumns = @JoinColumn(name = "therapist_id"),
        inverseJoinColumns = @JoinColumn(name = "treatment_id"))
    private Set<Treatment> treatments = new HashSet<>();

    protected Therapist() {}
    public UUID getId() { return id; }
    public String getName() { return name; }
    public Branch getBranch() { return branch; }
    public int getTurnaroundMinutes() { return turnaroundMinutes; }
    public Set<Treatment> getTreatments() { return Collections.unmodifiableSet(treatments); }
    public boolean isQualifiedFor(UUID treatmentId) {
        return treatments.stream().anyMatch(treatment -> treatment.getId().equals(treatmentId));
    }
}
