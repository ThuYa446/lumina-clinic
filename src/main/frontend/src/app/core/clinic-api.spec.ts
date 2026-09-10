import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ClinicApi } from './clinic-api';

describe('Clinic API boundary', () => {
  let api: ClinicApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ClinicApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('obtains CSRF first and sends payment capability and retry key as headers', () => {
    api.pay('booking-id', 'private-token', 'stable-retry-key').subscribe();
    http.expectNone('/api/bookings/booking-id/payments');
    http.expectOne('/api/csrf').flush({});
    const request = http.expectOne('/api/bookings/booking-id/payments');
    expect(request.request.headers.get('X-Booking-Token')).toBe('private-token');
    expect(request.request.headers.get('Idempotency-Key')).toBe('stable-retry-key');
    expect(request.request.url).not.toContain('private-token');
    expect(request.request.body).toEqual({});
    request.flush({});
  });

  it('does not submit a mutation when CSRF bootstrap fails', () => {
    let failed = false;
    api.cancel('booking-id', 'token').subscribe({
      error: () => {
        failed = true;
      },
    });
    http.expectOne('/api/csrf').flush({}, { status: 503, statusText: 'Unavailable' });
    http.expectNone('/api/bookings/booking-id/cancel');
    expect(failed).toBe(true);
  });

  it('unwraps the staff response without exposing credentials in query parameters', () => {
    let items: unknown;
    api.staffBookings('2026-09-14', '', 'Basic secret').subscribe((result) => {
      items = result;
    });
    const request = http.expectOne('/api/staff/bookings?date=2026-09-14');
    expect(request.request.headers.get('Authorization')).toBe('Basic secret');
    request.flush({ items: [] });
    expect(items).toEqual([]);
  });
});
