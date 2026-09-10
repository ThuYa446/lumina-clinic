import { describe, expect, it } from 'vitest';
import { clinicDate } from './clinic-date';

describe('clinic time zone', () => {
  it('changes Myanmar calendar dates at 17:30 UTC, regardless of the browser time zone', () => {
    expect(clinicDate(new Date('2026-09-09T17:29:59Z'))).toBe('2026-09-09');
    expect(clinicDate(new Date('2026-09-09T17:30:00Z'))).toBe('2026-09-10');
  });
});
