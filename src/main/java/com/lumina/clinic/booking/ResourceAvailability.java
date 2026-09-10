package com.lumina.clinic.booking;

import com.lumina.clinic.catalog.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

/** A therapist's turnaround and a room's cleaning period are independent resources. */
@Component
public class ResourceAvailability {
    public Optional<Room> findRoom(List<Room> rooms, List<Booking> reservations,
            Therapist therapist, Instant startsAt, int durationMinutes) {
        Instant treatmentEnd = startsAt.plus(Duration.ofMinutes(durationMinutes));
        Instant therapistEnd = treatmentEnd.plus(Duration.ofMinutes(therapist.getTurnaroundMinutes()));
        boolean therapistBusy = reservations.stream().anyMatch(b -> b.getTherapist().getId().equals(therapist.getId())
            && BookingPolicy.overlaps(startsAt, therapistEnd, b.getStartsAt(), b.getTherapistOccupiedUntil()));
        if (therapistBusy) return Optional.empty();
        Instant roomEnd = treatmentEnd.plus(Duration.ofMinutes(BookingPolicy.ROOM_TURNAROUND_MINUTES));
        return rooms.stream().filter(room -> reservations.stream().noneMatch(b -> b.getRoom().getId().equals(room.getId())
            && BookingPolicy.overlaps(startsAt, roomEnd, b.getStartsAt(), b.getRoomOccupiedUntil()))).findFirst();
    }
}
