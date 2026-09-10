import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicApi, errorMessage } from '../core/clinic-api';
import { clinicDate } from '../core/clinic-date';
import { Branch, StaffBooking } from '../core/models';
@Component({
  selector: 'app-staff-page',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  templateUrl: './staff-page.html',
})
export class StaffPage {
  private readonly api = inject(ClinicApi);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private authorization = '';
  private loadGeneration = 0;
  readonly signedIn = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly bookings = signal<StaffBooking[]>([]);
  readonly branches = signal<Branch[]>([]);
  readonly refundId = signal('');
  readonly refundBusy = signal(false);
  readonly loginForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  readonly filterForm = this.fb.nonNullable.group({
    date: [clinicDate(), Validators.required],
    branchId: [''],
  });
  readonly refundForm = this.fb.nonNullable.group({
    reference: ['', [Validators.required, Validators.maxLength(100)]],
  });
  constructor() {
    this.api
      .catalog()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => this.branches.set(data.branches),
        error: () => {
          /* Sign-in remains available if the catalog is temporarily unavailable. */
        },
      });
  }
  login(): void {
    if (this.loginForm.invalid || this.loading()) return;
    const form = this.loginForm.getRawValue();
    const bytes = new TextEncoder().encode(`${form.username}:${form.password}`);
    this.authorization = `Basic ${btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''))}`;
    this.load();
  }
  logout(): void {
    this.loadGeneration++;
    this.authorization = '';
    this.signedIn.set(false);
    this.bookings.set([]);
    this.loginForm.reset();
    this.error.set('');
    this.message.set('');
    this.loading.set(false);
    this.refundId.set('');
  }
  load(): void {
    if (!this.authorization || this.filterForm.invalid) return;
    const generation = ++this.loadGeneration;
    const filter = this.filterForm.getRawValue();
    this.loading.set(true);
    this.error.set('');
    this.api
      .staffBookings(filter.date, filter.branchId, this.authorization)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (bookings) => {
          if (generation !== this.loadGeneration) return;
          this.signedIn.set(true);
          this.loginForm.controls.password.reset();
          this.bookings.set(bookings);
          this.loading.set(false);
        },
        error: (error) => {
          if (generation !== this.loadGeneration) return;
          this.error.set(errorMessage(error));
          this.loading.set(false);
        },
      });
  }
  startRefund(id: string): void {
    this.refundId.set(id);
    this.refundForm.reset();
    this.message.set('');
  }
  refund(): void {
    if (this.refundForm.invalid || this.refundBusy()) return;
    this.refundBusy.set(true);
    this.error.set('');
    this.api
      .refund(this.refundId(), this.refundForm.controls.reference.value.trim(), this.authorization)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.refundBusy.set(false);
          this.refundId.set('');
          this.message.set('Demo refund recorded.');
          this.load();
        },
        error: (error) => {
          this.refundBusy.set(false);
          this.error.set(errorMessage(error));
        },
      });
  }
}
