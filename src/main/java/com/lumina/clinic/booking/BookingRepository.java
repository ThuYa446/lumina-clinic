package com.lumina.clinic.booking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByIdempotencyKey(UUID idempotencyKey);

    @Query("select b.branch.id from Booking b where b.id = :id")
    Optional<UUID> findBranchIdById(@Param("id") UUID id);

    @Query("select b from Booking b where b.branch.id = :branchId and b.status = com.lumina.clinic.booking.BookingStatus.PENDING_PAYMENT and b.holdExpiresAt <= :now")
    List<Booking> findExpiredHolds(@Param("branchId") UUID branchId, @Param("now") Instant now);

    @Query("select b from Booking b where b.branch.id = :branchId and b.status in (com.lumina.clinic.booking.BookingStatus.PENDING_PAYMENT, com.lumina.clinic.booking.BookingStatus.CONFIRMED) and b.startsAt < :until and (b.roomOccupiedUntil > :from or b.therapistOccupiedUntil > :from)")
    List<Booking> findActiveInWindow(@Param("branchId") UUID branchId, @Param("from") Instant from, @Param("until") Instant until);

    @Query("select b from Booking b where (:branchId is null or b.branch.id = :branchId) and b.startsAt >= :from and b.startsAt < :until order by b.startsAt, b.id")
    List<Booking> findStaffDay(@Param("branchId") UUID branchId, @Param("from") Instant from, @Param("until") Instant until);
}
