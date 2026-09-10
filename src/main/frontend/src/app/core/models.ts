export interface Branch {
  id: string;
  name: string;
  address: string;
}
export interface Treatment {
  id: string;
  name: string;
  description: string;
  durationMinutes: number;
  price: number;
}
export interface Therapist {
  id: string;
  name: string;
  branchId: string;
  treatmentIds: string[];
  turnaroundMinutes: number;
}
export interface Catalog {
  branches: Branch[];
  treatments: Treatment[];
  therapists: Therapist[];
  currency: string;
  depositAmount: number;
  timeZone: string;
  openingTime: string;
  closingTime: string;
  holdMinutes: number;
}
export interface Slot {
  startsAt: string;
  endsAt: string;
  therapistId: string;
  therapistName: string;
}
export interface Availability {
  date: string;
  timeZone: string;
  slots: Slot[];
}
export interface BookingRequest {
  branchId: string;
  treatmentId: string;
  therapistId: string;
  startsAt: string;
  client: { name: string; email: string; phone: string };
  membershipCode?: string;
}
export type BookingStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';
export interface Booking {
  id: string;
  managementToken?: string;
  status: BookingStatus;
  branchName: string;
  treatmentName: string;
  therapistName: string;
  startsAt: string;
  endsAt: string;
  holdExpiresAt: string | null;
  depositAmount: number;
  currency: string;
  clientName: string;
  member: boolean;
  paymentStatus: 'NOT_REQUIRED' | 'UNPAID' | 'PAID' | 'REFUND_PENDING' | 'REFUNDED';
  cancellationAllowed: boolean;
}
export interface PaymentResponse {
  booking: Booking;
  paymentReference: string;
  mode: 'DEMO';
}
export interface StaffBooking {
  id: string;
  clientName: string;
  branchName: string;
  treatmentName: string;
  therapistName: string;
  roomName: string;
  startsAt: string;
  endsAt: string;
  status: BookingStatus;
  paymentStatus: Booking['paymentStatus'];
  depositAmount: number;
  currency: string;
  member: boolean;
  refundReference: string | null;
}
