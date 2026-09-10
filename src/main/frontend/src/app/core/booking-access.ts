import { Injectable } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class BookingAccess {
  private readonly tokens = new Map<string, string>();
  save(id: string, token: string): void {
    this.tokens.set(id, token);
    try {
      sessionStorage.setItem(`lumina.booking.${id}`, token);
    } catch {
      /* Support restricted browsers using memory. */
    }
  }
  get(id: string): string {
    const value = this.tokens.get(id);
    if (value) return value;
    try {
      return sessionStorage.getItem(`lumina.booking.${id}`) ?? '';
    } catch {
      return '';
    }
  }
  link(id: string, token: string): string {
    return `${location.origin}/booking/${encodeURIComponent(id)}#token=${encodeURIComponent(token)}`;
  }
}
