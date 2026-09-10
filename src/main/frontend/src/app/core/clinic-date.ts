export const CLINIC_TIME_ZONE = 'Asia/Yangon';
// Angular DatePipe accepts a numeric offset; Myanmar observes UTC+06:30 year-round.
export const CLINIC_UTC_OFFSET = '+0630';
export function clinicDate(now = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: CLINIC_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);
  return `${parts.find((p) => p.type === 'year')!.value}-${parts.find((p) => p.type === 'month')!.value}-${parts.find((p) => p.type === 'day')!.value}`;
}
