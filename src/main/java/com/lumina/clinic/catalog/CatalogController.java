package com.lumina.clinic.catalog;

import com.lumina.clinic.booking.BookingPolicy;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final BranchRepository branches;
    private final TreatmentRepository treatments;
    private final TherapistRepository therapists;

    public CatalogController(BranchRepository branches, TreatmentRepository treatments, TherapistRepository therapists) {
        this.branches = branches;
        this.treatments = treatments;
        this.therapists = therapists;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public CatalogResponse catalog() {
        return new CatalogResponse(
            branches.findAll(Sort.by("name")).stream().map(b -> new BranchView(b.getId(), b.getName(), b.getAddress())).toList(),
            treatments.findAll(Sort.by("durationMinutes", "name")).stream().map(t -> new TreatmentView(t.getId(), t.getName(), t.getDescription(), t.getDurationMinutes(), t.getPrice())).toList(),
            therapists.findAll(Sort.by("name")).stream().map(t -> new TherapistView(t.getId(), t.getName(), t.getBranch().getId(),
                t.getTreatments().stream().map(Treatment::getId).sorted().toList(), t.getTurnaroundMinutes())).toList(),
            BookingPolicy.CURRENCY, BookingPolicy.DEPOSIT, BookingPolicy.ZONE.getId(),
            BookingPolicy.OPENING.toString(), BookingPolicy.CLOSING.toString(), BookingPolicy.HOLD_MINUTES);
    }

    public record BranchView(UUID id, String name, String address) {}
    public record TreatmentView(UUID id, String name, String description, int durationMinutes, BigDecimal price) {}
    public record TherapistView(UUID id, String name, UUID branchId, List<UUID> treatmentIds, int turnaroundMinutes) {}
    public record CatalogResponse(List<BranchView> branches, List<TreatmentView> treatments,
        List<TherapistView> therapists, String currency, BigDecimal depositAmount, String timeZone,
        String openingTime, String closingTime, int holdMinutes) {}
}
