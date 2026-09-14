import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ClinicApi } from '../core/clinic-api';
import { StaffBooking } from '../core/models';
import { StaffPage } from './staff-page';

describe('staff consent records', () => {
  const booking: StaffBooking = {
    id: 'booking-id',
    clientName: 'Test Client',
    clientIdNumber: '0012/KaMaYa(N)000123',
    clientDateOfBirth: '1992-02-29',
    branchName: 'Bahan',
    treatmentName: 'Laser treatment',
    therapistName: 'Ari',
    roomName: 'Room 1',
    startsAt: '2026-09-14T09:00:00+06:30',
    endsAt: '2026-09-14T09:30:00+06:30',
    status: 'CONFIRMED',
    paymentStatus: 'NOT_REQUIRED',
    depositAmount: 300,
    currency: 'MMK',
    member: true,
    refundReference: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: ClinicApi, useValue: { catalog: vi.fn(() => of({ branches: [] })) } }],
    });
  });

  it('shows the saved ID and date without timezone conversion, then clears them on sign-out', () => {
    const fixture = TestBed.createComponent(StaffPage);
    fixture.componentInstance.signedIn.set(true);
    fixture.componentInstance.bookings.set([booking]);
    fixture.detectChanges();
    const details = fixture.nativeElement.querySelector('.consent-details') as HTMLDetailsElement;
    expect(details.open).toBe(false);
    expect(Array.from(details.querySelectorAll('dd'), (element) => element.textContent)).toEqual([
      '0012/KaMaYa(N)000123',
      '1992-02-29',
    ]);
    fixture.componentInstance.logout();
    fixture.detectChanges();
    expect(fixture.componentInstance.bookings()).toEqual([]);
    expect(fixture.nativeElement.textContent).not.toContain(booking.clientIdNumber);
  });

  it('labels missing legacy identity details without fabricating a date or ID', () => {
    const fixture = TestBed.createComponent(StaffPage);
    fixture.componentInstance.signedIn.set(true);
    fixture.componentInstance.bookings.set([
      { ...booking, clientIdNumber: null, clientDateOfBirth: null },
    ]);
    fixture.detectChanges();
    const values = fixture.nativeElement.querySelectorAll(
      '.consent-details dd',
    ) as NodeListOf<HTMLElement>;
    expect(Array.from(values, (element) => element.textContent)).toEqual([
      'Not recorded',
      'Not recorded',
    ]);
  });
});
