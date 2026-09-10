import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { BookingAccess } from '../core/booking-access';
import { ClinicApi, errorMessage } from '../core/clinic-api';
import { Booking } from '../core/models';

@Component({
  selector: 'app-booking-management',
  imports: [CurrencyPipe, DatePipe, RouterLink, FormsModule],
  templateUrl: './booking-management.html',
})
export class BookingManagement implements OnInit {
  private readonly api = inject(ClinicApi);
  private readonly access = inject(BookingAccess);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private token = '';
  private paymentKey = crypto.randomUUID();
  private expiredRefreshDone = false;
  readonly id = this.route.snapshot.paramMap.get('id') ?? '';
  readonly booking = signal<Booking | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly hasAccess = signal(false);
  readonly confirmCancel = signal(false);
  readonly now = signal(Date.now());
  readonly privateLink = signal('');
  accessCode = '';
  readonly holdSeconds = computed(() => {
    const expires = this.booking()?.holdExpiresAt;
    return expires ? Math.max(0, Math.ceil((new Date(expires).getTime() - this.now()) / 1000)) : 0;
  });
  readonly countdown = computed(
    () =>
      `${Math.floor(this.holdSeconds() / 60)}:${String(this.holdSeconds() % 60).padStart(2, '0')}`,
  );
  readonly cancellationAllowed = computed(
    () =>
      !!this.booking()?.cancellationAllowed &&
      new Date(this.booking()!.startsAt).getTime() - this.now() >= 24 * 60 * 60 * 1000,
  );
  ngOnInit(): void {
    const fragment = new URLSearchParams(this.route.snapshot.fragment ?? '');
    this.token = fragment.get('token') || this.access.get(this.id);
    if (this.token) {
      this.access.save(this.id, this.token);
      this.hasAccess.set(true);
      this.refresh();
    }
    if (fragment.has('token'))
      void this.router.navigate([], {
        relativeTo: this.route,
        fragment: undefined,
        replaceUrl: true,
      });
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.now.set(Date.now());
        if (
          this.booking()?.status === 'PENDING_PAYMENT' &&
          this.holdSeconds() === 0 &&
          !this.expiredRefreshDone &&
          !this.busy()
        ) {
          this.expiredRefreshDone = true;
          this.refresh();
        }
      });
  }
  unlock(): void {
    if (!this.accessCode.trim()) return;
    this.token = this.accessCode.trim();
    this.access.save(this.id, this.token);
    this.hasAccess.set(true);
    this.refresh();
  }
  refresh(): void {
    if (this.loading() || !this.token) return;
    this.loading.set(true);
    this.error.set('');
    this.api
      .booking(this.id, this.token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (booking) => {
          this.booking.set(booking);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(errorMessage(error));
          this.loading.set(false);
          if (error.status === 404) {
            this.hasAccess.set(false);
            this.booking.set(null);
          }
        },
      });
  }
  pay(): void {
    if (this.busy() || this.loading() || this.holdSeconds() === 0) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api
      .pay(this.id, this.token, this.paymentKey)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.booking.set(result.booking);
          this.busy.set(false);
          this.message.set(`Demo payment completed. Reference: ${result.paymentReference}`);
        },
        error: (error) => {
          this.busy.set(false);
          this.error.set(errorMessage(error));
        },
      });
  }
  cancel(): void {
    if (this.busy() || this.loading() || !this.confirmCancel()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api
      .cancel(this.id, this.token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (booking) => {
          this.booking.set(booking);
          this.busy.set(false);
          this.confirmCancel.set(false);
          this.message.set('Your appointment has been cancelled.');
        },
        error: (error) => {
          this.busy.set(false);
          this.error.set(errorMessage(error));
        },
      });
  }
  async copyLink(): Promise<void> {
    const link = this.access.link(this.id, this.token);
    try {
      await navigator.clipboard.writeText(link);
      this.message.set(
        'Private booking link copied. Keep it safe; anyone with this link can manage this appointment.',
      );
    } catch {
      this.privateLink.set(link);
      this.message.set('Copy the private link below and keep it safe.');
    }
  }
}
