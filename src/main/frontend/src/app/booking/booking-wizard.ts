import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, map, of, Subject, switchMap } from 'rxjs';
import { BookingAccess } from '../core/booking-access';
import { ClinicApi, errorMessage } from '../core/clinic-api';
import { clinicDate } from '../core/clinic-date';
import { BookingRequest, Catalog, Slot } from '../core/models';
@Component({
  selector: 'app-booking-wizard',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  templateUrl: './booking-wizard.html',
})
export class BookingWizard implements OnInit {
  private readonly api = inject(ClinicApi);
  private readonly router = inject(Router);
  private readonly access = inject(BookingAccess);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly scheduleChanges = new Subject<void>();
  private previousPayload = '';
  private idempotencyKey = '';
  readonly catalog = signal<Catalog | null>(null);
  readonly catalogLoading = signal(true);
  readonly error = signal('');
  readonly slotError = signal('');
  readonly step = signal(1);
  readonly branchId = signal('');
  readonly treatmentId = signal('');
  readonly therapistId = signal('');
  readonly date = signal(clinicDate());
  readonly today = clinicDate();
  readonly slots = signal<Slot[]>([]);
  readonly selectedSlot = signal<Slot | null>(null);
  readonly slotsLoading = signal(false);
  readonly submitting = signal(false);
  readonly treatment = computed(() =>
    this.catalog()?.treatments.find((t) => t.id === this.treatmentId()),
  );
  readonly branch = computed(() => this.catalog()?.branches.find((b) => b.id === this.branchId()));
  readonly therapists = computed(
    () =>
      this.catalog()?.therapists.filter(
        (t) => t.branchId === this.branchId() && t.treatmentIds.includes(this.treatmentId()),
      ) ?? [],
  );
  readonly clientForm = this.fb.nonNullable.group({
    name: [
      '',
      [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(100),
        Validators.pattern(/.*\S.*/),
      ],
    ],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    phone: ['', [Validators.required, Validators.pattern(/^\+?[0-9 ()\-]{7,25}$/)]],
    membershipCode: ['', Validators.maxLength(100)],
  });
  ngOnInit(): void {
    this.scheduleChanges
      .pipe(
        switchMap(() => {
          this.selectedSlot.set(null);
          this.slots.set([]);
          this.slotError.set('');
          if (!this.branchId() || !this.treatmentId() || !this.date()) {
            this.slotsLoading.set(false);
            return of({ slots: [] as Slot[], error: '' });
          }
          this.slotsLoading.set(true);
          return this.api
            .availability(this.branchId(), this.treatmentId(), this.date(), this.therapistId())
            .pipe(
              map((result) => ({ slots: result.slots, error: '' })),
              catchError((error) => of({ slots: [] as Slot[], error: errorMessage(error) })),
            );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        this.slots.set(result.slots);
        this.slotError.set(result.error);
        this.slotsLoading.set(false);
      });
    this.loadCatalog();
  }
  loadCatalog(): void {
    this.catalogLoading.set(true);
    this.error.set('');
    this.api
      .catalog()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.catalog.set(data);
          this.catalogLoading.set(false);
        },
        error: (error) => {
          this.error.set(errorMessage(error));
          this.catalogLoading.set(false);
        },
      });
  }
  chooseTreatment(id: string): void {
    this.treatmentId.set(id);
    this.therapistId.set('');
    this.refreshSlots();
  }
  chooseBranch(id: string): void {
    this.branchId.set(id);
    this.therapistId.set('');
    this.refreshSlots();
  }
  changeTherapist(event: Event): void {
    this.therapistId.set((event.target as HTMLSelectElement).value);
    this.refreshSlots();
  }
  changeDate(event: Event): void {
    this.date.set((event.target as HTMLInputElement).value);
    this.refreshSlots();
  }
  refreshSlots(): void {
    this.scheduleChanges.next();
  }
  goToStep(step: number): void {
    this.step.set(step);
    this.error.set('');
    document.getElementById('booking-heading')?.focus();
  }
  invalid(field: 'name' | 'email' | 'phone'): boolean {
    const control = this.clientForm.controls[field];
    return control.invalid && control.touched;
  }
  submit(): void {
    if (this.submitting()) return;
    this.clientForm.markAllAsTouched();
    const slot = this.selectedSlot();
    if (this.clientForm.invalid || !slot) return;
    const form = this.clientForm.getRawValue();
    const request: BookingRequest = {
      branchId: this.branchId(),
      treatmentId: this.treatmentId(),
      therapistId: slot.therapistId,
      startsAt: slot.startsAt,
      client: { name: form.name.trim(), email: form.email.trim(), phone: form.phone.trim() },
      ...(form.membershipCode.trim() ? { membershipCode: form.membershipCode.trim() } : {}),
    };
    const payload = JSON.stringify(request);
    if (payload !== this.previousPayload) {
      this.previousPayload = payload;
      this.idempotencyKey = crypto.randomUUID();
    }
    this.submitting.set(true);
    this.error.set('');
    this.api
      .createBooking(request, this.idempotencyKey)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (booking) => {
          this.submitting.set(false);
          if (!booking.managementToken) {
            this.error.set(
              'The appointment was created, but its private access link was missing. Please contact the clinic.',
            );
            return;
          }
          this.access.save(booking.id, booking.managementToken);
          void this.router.navigate(['/booking', booking.id], {
            fragment: `token=${encodeURIComponent(booking.managementToken)}`,
          });
        },
        error: (error) => {
          this.submitting.set(false);
          this.error.set(errorMessage(error));
        },
      });
  }
}
