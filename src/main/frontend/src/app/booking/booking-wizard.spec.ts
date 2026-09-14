import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ClinicApi } from '../core/clinic-api';
import { clinicDate } from '../core/clinic-date';
import { Availability, BookingRequest, Catalog, Slot } from '../core/models';
import { BookingWizard } from './booking-wizard';

describe('booking workflow reliability', () => {
  const slot: Slot = {
    startsAt: '2026-09-14T09:00:00+06:30',
    endsAt: '2026-09-14T09:30:00+06:30',
    therapistId: 'therapist',
    therapistName: 'Ari',
  };
  const catalog: Catalog = {
    branches: [],
    treatments: [],
    therapists: [],
    currency: 'MMK',
    depositAmount: 300,
    timeZone: 'Asia/Yangon',
    openingTime: '09:00',
    closingTime: '18:00',
    holdMinutes: 15,
  };
  let api: {
    catalog: ReturnType<typeof vi.fn>;
    availability: ReturnType<typeof vi.fn>;
    createBooking: ReturnType<typeof vi.fn>;
  };
  beforeEach(() => {
    api = {
      catalog: vi.fn(() => of(catalog)),
      availability: vi.fn(() => of({ slots: [] })),
      createBooking: vi.fn(() => throwError(() => new HttpErrorResponse({ status: 0 }))),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: ClinicApi, useValue: api },
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    });
  });

  it('reuses the request key after a lost response, but changes it when booking details change', () => {
    const component = TestBed.createComponent(BookingWizard).componentInstance;
    component.ngOnInit();
    component.branchId.set('branch');
    component.treatmentId.set('treatment');
    component.selectedSlot.set(slot);
    component.clientForm.setValue({
      name: 'Test Client',
      email: 'test@example.com',
      phone: '+95 9 123 456 789',
      idNumber: ' 0012/KaMaYa(N)000123 ',
      dateOfBirth: '1992-02-29',
      membershipCode: '',
    });
    component.submit();
    component.submit();
    expect(api.createBooking).toHaveBeenCalledTimes(2);
    const firstKey = api.createBooking.mock.calls[0]![1] as string;
    expect(api.createBooking.mock.calls[1]![1]).toBe(firstKey);
    expect((api.createBooking.mock.calls[0]![0] as BookingRequest).client).toEqual({
      name: 'Test Client',
      email: 'test@example.com',
      phone: '+95 9 123 456 789',
      idNumber: '0012/KaMaYa(N)000123',
      dateOfBirth: '1992-02-29',
    });
    component.clientForm.controls.name.setValue('Changed Client');
    component.submit();
    expect(api.createBooking.mock.calls[2]![1]).not.toBe(firstKey);
    component.clientForm.controls.idNumber.setValue('0012345678');
    component.submit();
    expect(api.createBooking.mock.calls[3]![1]).not.toBe(api.createBooking.mock.calls[2]![1]);
    component.clientForm.controls.dateOfBirth.setValue('1992-03-01');
    component.submit();
    expect(api.createBooking.mock.calls[4]![1]).not.toBe(api.createBooking.mock.calls[3]![1]);
  });

  it('does not reserve until both consent record fields are present', () => {
    const component = TestBed.createComponent(BookingWizard).componentInstance;
    component.selectedSlot.set(slot);
    component.clientForm.patchValue({
      name: 'Test Client',
      email: 'test@example.com',
      phone: '+95 9 123 456 789',
    });
    component.submit();
    expect(component.invalid('idNumber')).toBe(true);
    expect(component.invalid('dateOfBirth')).toBe(true);
    expect(api.createBooking).not.toHaveBeenCalled();
  });

  it.each(['', '   ', 'x'.repeat(65), 'ID\n123', 'ID\u0000123', 'ID\u007f123', 'ID\u0085123'])(
    'rejects an empty, oversized or control-character ID: %j',
    (value) => {
      const component = TestBed.createComponent(BookingWizard).componentInstance;
      component.clientForm.controls.idNumber.setValue(value);
      expect(component.clientForm.controls.idNumber.invalid).toBe(true);
    },
  );

  it.each(['0012345678', '12/KaMaYa(N)123456', 'AB-001 234', 'x'.repeat(64)])(
    'accepts varied text ID formats without converting them to numbers: %s',
    (value) => {
      const component = TestBed.createComponent(BookingWizard).componentInstance;
      component.clientForm.controls.idNumber.setValue(value);
      expect(component.clientForm.controls.idNumber.valid).toBe(true);
    },
  );

  it.each(['', '0000-01-01', '1993-02-29', '1992-02-30', '1992-13-01', '1992-2-01', '2999-01-01'])(
    'rejects missing, impossible, non-ISO or future dates of birth: %j',
    (value) => {
      const component = TestBed.createComponent(BookingWizard).componentInstance;
      component.clientForm.controls.dateOfBirth.setValue(value);
      expect(component.clientForm.controls.dateOfBirth.invalid).toBe(true);
    },
  );

  it('accepts a leap-day birthday and today in the clinic timezone', () => {
    const component = TestBed.createComponent(BookingWizard).componentInstance;
    for (const value of ['1992-02-29', clinicDate()]) {
      component.clientForm.controls.dateOfBirth.setValue(value);
      expect(component.clientForm.controls.dateOfBirth.valid).toBe(true);
    }
    component.clientForm.controls.dateOfBirth.setValue(
      clinicDate(new Date(Date.now() + 86_400_000)),
    );
    expect(component.clientForm.controls.dateOfBirth.invalid).toBe(true);
  });

  it('ignores stale availability after the user changes the date', () => {
    const oldRequest = new Subject<Availability>();
    const currentRequest = new Subject<Availability>();
    api.availability.mockReturnValueOnce(oldRequest).mockReturnValueOnce(currentRequest);
    const component = TestBed.createComponent(BookingWizard).componentInstance;
    component.ngOnInit();
    component.branchId.set('branch');
    component.treatmentId.set('treatment');
    component.refreshSlots();
    component.date.set('2026-09-15');
    component.refreshSlots();
    currentRequest.next({ date: '2026-09-15', timeZone: 'Asia/Yangon', slots: [slot] });
    oldRequest.next({ date: '2026-09-14', timeZone: 'Asia/Yangon', slots: [] });
    expect(component.slots()).toEqual([slot]);
    expect(component.slotsLoading()).toBe(false);
  });
});
