package com.lumina.clinic.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByBranchIdOrderByName(UUID branchId);
}
