import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, switchMap } from 'rxjs';
import {
  Availability,
  Booking,
  BookingRequest,
  Catalog,
  PaymentResponse,
  StaffBooking,
} from './models';
@Injectable({ providedIn: 'root' })
export class ClinicApi {
  private readonly http = inject(HttpClient);
  catalog(): Observable<Catalog> {
    return this.http.get<Catalog>('/api/catalog');
  }
  availability(
    branchId: string,
    treatmentId: string,
    date: string,
    therapistId?: string,
  ): Observable<Availability> {
    let params = new HttpParams()
      .set('branchId', branchId)
      .set('treatmentId', treatmentId)
      .set('date', date);
    if (therapistId) params = params.set('therapistId', therapistId);
    return this.http.get<Availability>('/api/availability', { params });
  }
  createBooking(request: BookingRequest, key: string): Observable<Booking> {
    return this.withCsrf(() =>
      this.http.post<Booking>('/api/bookings', request, { headers: { 'Idempotency-Key': key } }),
    );
  }
  booking(id: string, token: string): Observable<Booking> {
    return this.http.get<Booking>(`/api/bookings/${encodeURIComponent(id)}`, {
      headers: this.bookingHeaders(token),
    });
  }
  pay(id: string, token: string, key: string): Observable<PaymentResponse> {
    return this.withCsrf(() =>
      this.http.post<PaymentResponse>(
        `/api/bookings/${encodeURIComponent(id)}/payments`,
        {},
        { headers: this.bookingHeaders(token).set('Idempotency-Key', key) },
      ),
    );
  }
  cancel(id: string, token: string): Observable<Booking> {
    return this.withCsrf(() =>
      this.http.post<Booking>(
        `/api/bookings/${encodeURIComponent(id)}/cancel`,
        {},
        { headers: this.bookingHeaders(token) },
      ),
    );
  }
  staffBookings(date: string, branchId: string, authorization: string): Observable<StaffBooking[]> {
    let params = new HttpParams().set('date', date);
    if (branchId) params = params.set('branchId', branchId);
    return this.http
      .get<{ items: StaffBooking[] }>('/api/staff/bookings', {
        params,
        headers: { Authorization: authorization },
      })
      .pipe(map((result) => result.items));
  }
  refund(id: string, reference: string, authorization: string): Observable<StaffBooking> {
    return this.withCsrf(() =>
      this.http.post<StaffBooking>(
        `/api/staff/bookings/${encodeURIComponent(id)}/refund`,
        { reference },
        { headers: { Authorization: authorization } },
      ),
    );
  }
  private bookingHeaders(token: string): HttpHeaders {
    return new HttpHeaders({ 'X-Booking-Token': token });
  }
  // Angular copies the server's same-origin CSRF cookie to the X-XSRF-TOKEN header.
  private withCsrf<T>(action: () => Observable<T>): Observable<T> {
    return this.http.get('/api/csrf').pipe(switchMap(action));
  }
}
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0)
      return 'We could not reach the clinic. Please check your connection and try again.';
    const detail = error.error?.detail;
    if (typeof detail === 'string') return detail;
    if (error.status === 401 || error.status === 403)
      return 'Access could not be verified. Please check your private booking link or credentials.';
    if (error.status === 429)
      return 'There have been too many requests. Please wait a moment and try again.';
  }
  return 'Something went wrong. Please try again.';
}
