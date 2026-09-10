import { Routes } from '@angular/router';
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./booking/booking-wizard').then((m) => m.BookingWizard),
    title: 'Book your moment | Lumina Clinic',
  },
  {
    path: 'booking/:id',
    loadComponent: () => import('./booking/booking-management').then((m) => m.BookingManagement),
    title: 'Your appointment | Lumina Clinic',
  },
  {
    path: 'staff',
    loadComponent: () => import('./staff/staff-page').then((m) => m.StaffPage),
    title: 'Clinic team | Lumina Clinic',
  },
  { path: '**', redirectTo: '' },
];
