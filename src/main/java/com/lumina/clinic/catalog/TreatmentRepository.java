package com.lumina.clinic.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {}
