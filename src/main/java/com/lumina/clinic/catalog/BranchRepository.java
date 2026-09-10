package com.lumina.clinic.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Branch b where b.id = :id")
    Optional<Branch> findLockedById(@Param("id") UUID id);
}
