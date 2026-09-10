package com.lumina.clinic.booking;

import com.lumina.clinic.catalog.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceAvailabilityTest {
    private final ResourceAvailability availability = new ResourceAvailability();
    private final Instant nine = Instant.parse("2026-09-10T02:30:00Z");

    @Test void seniorMayStartAdjacentTreatmentInDifferentRoomWhileFirstRoomIsCleaning() {
        Therapist senior = therapist(0);
        Room first = room();
        Room second = room();
        Booking earlier = reservation(senior, first, nine, nine.plusSeconds(3600), nine.plusSeconds(4500));
        assertThat(availability.findRoom(List.of(first, second), List.of(earlier), senior, nine.plusSeconds(3600), 30)).contains(second);
        assertThat(availability.findRoom(List.of(first), List.of(earlier), senior, nine.plusSeconds(3600), 30)).isEmpty();
    }

    @Test void regularTherapistStillNeedsTurnaroundEvenWhenAnotherRoomIsFree() {
        Therapist regular = therapist(15);
        Room first = room();
        Room second = room();
        Booking earlier = reservation(regular, first, nine, nine.plusSeconds(4500), nine.plusSeconds(4500));
        assertThat(availability.findRoom(List.of(first, second), List.of(earlier), regular, nine.plusSeconds(3600), 30)).isEmpty();
        assertThat(availability.findRoom(List.of(first, second), List.of(earlier), regular, nine.plusSeconds(4500), 30)).contains(first);
    }

    @Test void earlierRequestCannotExtendThroughAnExistingLaterAppointment() {
        Therapist regular = therapist(15);
        Room room = room();
        Booking later = reservation(regular, room, nine.plusSeconds(3600), nine.plusSeconds(8100), nine.plusSeconds(8100));
        assertThat(availability.findRoom(List.of(room), List.of(later), regular, nine, 60)).isEmpty();
        assertThat(availability.findRoom(List.of(room), List.of(later), regular, nine, 30)).contains(room);
    }

    @Test void fullyOccupiedRoomsBlockOtherAvailableTherapists() {
        Therapist candidate = therapist(15);
        Therapist other = therapist(15);
        Room room = room();
        Booking existing = reservation(other, room, nine, nine.plusSeconds(4500), nine.plusSeconds(4500));
        assertThat(availability.findRoom(List.of(room), List.of(existing), candidate, nine, 30)).isEmpty();
    }

    private Therapist therapist(int turnaround) {
        Therapist therapist = mock(Therapist.class);
        when(therapist.getId()).thenReturn(UUID.randomUUID());
        when(therapist.getTurnaroundMinutes()).thenReturn(turnaround);
        return therapist;
    }
    private Room room() {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(UUID.randomUUID());
        return room;
    }
    private Booking reservation(Therapist therapist, Room room, Instant start, Instant therapistEnd, Instant roomEnd) {
        Booking booking = mock(Booking.class);
        when(booking.getTherapist()).thenReturn(therapist);
        when(booking.getRoom()).thenReturn(room);
        when(booking.getStartsAt()).thenReturn(start);
        when(booking.getTherapistOccupiedUntil()).thenReturn(therapistEnd);
        when(booking.getRoomOccupiedUntil()).thenReturn(roomEnd);
        return booking;
    }
}
