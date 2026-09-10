package com.lumina.clinic.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TherapistRepository extends JpaRepository<Therapist, UUID> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"branch", "treatments"})
    List<Therapist> findAll(org.springframework.data.domain.Sort sort);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"branch", "treatments"})
    List<Therapist> findByBranchIdOrderByName(UUID branchId);
}
