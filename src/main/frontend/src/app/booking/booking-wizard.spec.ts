import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ClinicApi } from '../core/clinic-api';
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
      membershipCode: '',
    });
    component.submit();
    component.submit();
    expect(api.createBooking).toHaveBeenCalledTimes(2);
    const firstKey = api.createBooking.mock.calls[0]![1] as string;
    expect(api.createBooking.mock.calls[1]![1]).toBe(firstKey);
    expect((api.createBooking.mock.calls[0]![0] as BookingRequest).client.email).toBe(
      'test@example.com',
    );
    component.clientForm.controls.name.setValue('Changed Client');
    component.submit();
    expect(api.createBooking.mock.calls[2]![1]).not.toBe(firstKey);
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
