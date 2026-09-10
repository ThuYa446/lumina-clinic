import { expect, test } from '@playwright/test';

function appointmentDate(): string {
  const date = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000);
  while (
    new Intl.DateTimeFormat('en-US', { timeZone: 'Asia/Yangon', weekday: 'short' }).format(date) ===
    'Sun'
  ) {
    date.setUTCDate(date.getUTCDate() + 1);
  }
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Yangon',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

test('client books, pays once, reloads, cancels and reception records the refund', async ({
  page,
  context,
}, testInfo) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text());
  });
  const date = appointmentDate();
  const catalogResponse = await page.request.get('/api/catalog');
  expect(catalogResponse.ok()).toBeTruthy();
  const catalog = await catalogResponse.json();
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Make time for yourself.' })).toBeVisible();
  await expect(page.locator('.branch-card')).toHaveCount(6);
  await expect(page.locator('.branch-card')).toContainText([
    'Bahan',
    'Kamayut',
    'Sanchaung',
    'Tamwe',
    'Thingangyun',
    'Yankin',
  ]);
  await expect(page.locator('footer')).toContainText('Myanmar time · MMT (UTC+06:30)');
  await expect(page.locator('.treatment-card')).toContainText([
    /MMK\s*20,000/,
    /MMK\s*45,000/,
    /MMK\s*70,000/,
  ]);
  await expect(page.locator('.site-header')).toHaveCSS('display', 'flex');
  await expect(page.locator('.hero-art')).toHaveCSS('position', 'relative');
  await page.screenshot({ path: testInfo.outputPath('landing-desktop.png'), fullPage: true });
  await page.locator('.treatment-card').first().click();
  await page.locator('.branch-card').first().click();
  await page.getByRole('button', { name: 'Choose a time' }).click();
  await page.getByLabel('Appointment date').fill(date);
  // Date edits can cancel browser requests. Read the API independently, then
  // wait for the UI to show the same Myanmar time from the completed response.
  const availabilityResponse = await page.request.get('/api/availability', {
    params: {
      branchId: catalog.branches[0].id,
      treatmentId: catalog.treatments[0].id,
      date,
    },
  });
  expect(availabilityResponse.ok()).toBeTruthy();
  const availability = await availabilityResponse.json();
  expect(availability.timeZone).toBe('Asia/Yangon');
  const chosenTime = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Yangon',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(availability.slots[0].startsAt));
  await expect(page.locator('.slot').first()).toContainText(chosenTime);
  await page.locator('.slot').first().click();
  await page.getByRole('button', { name: 'Your details' }).click();
  await page.getByLabel('Full name').fill('Browser Test Client');
  await page.getByLabel('Email address').fill('browser-test@example.com');
  await page.getByLabel('Phone number').fill('+95 9 123 456 789');
  await page.getByRole('button', { name: 'Reserve appointment' }).click();
  await expect(page.getByRole('heading', { name: 'Your time is on hold.' })).toBeVisible();
  await expect(page.locator('.appointment-details')).toContainText(chosenTime);
  await expect(page.locator('.appointment-details')).toContainText('Bahan');
  await expect(page.locator('.deposit-amount')).toContainText(/MMK\s*300/);
  const bookingPath = new URL(page.url()).pathname;
  await expect(page).not.toHaveURL(/#token=/);
  await page.getByRole('button', { name: 'Pay demo deposit & confirm' }).click();
  await expect(page.locator('.status-badge')).toHaveText('CONFIRMED');
  await page.reload();
  await expect(page.locator('.status-badge')).toHaveText('CONFIRMED');
  await page.screenshot({ path: testInfo.outputPath('confirmed-desktop.png'), fullPage: true });
  await page.getByRole('button', { name: 'Cancel appointment', exact: true }).click();
  await page.getByRole('button', { name: 'Yes, cancel', exact: true }).click();
  await expect(page.locator('.status-badge')).toHaveText('CANCELLED');
  await expect(page.getByText('Your demo refund is pending.', { exact: true })).toBeVisible();

  const staff = await context.newPage();
  await staff.goto('/staff');
  await staff.getByLabel('Username').fill(process.env['E2E_STAFF_USERNAME'] || 'receptionist');
  await staff
    .getByLabel('Password')
    .fill(process.env['E2E_STAFF_PASSWORD'] || 'local-reception-only');
  await staff.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(staff.getByRole('button', { name: 'Sign out' })).toBeVisible();
  await staff.getByLabel('Date', { exact: true }).fill(date);
  await staff.getByRole('button', { name: 'View appointments' }).click();
  const bookingId = bookingPath.split('/').pop()!;
  // The complete row is scoped by the ID, not by a name potentially shared by another test.
  const row = staff
    .locator('tr')
    .filter({ has: staff.locator(`[data-booking-id="${bookingId}"]`) });
  await expect(row).toBeVisible();
  await expect(row).toContainText(chosenTime);
  await expect(row).toContainText(/MMK\s*300/);
  await row.getByRole('button', { name: 'Record demo refund' }).click();
  await staff.getByLabel('Refund reference').fill(`DEMO-E2E-${Date.now()}`);
  await staff.getByRole('button', { name: 'Record refund', exact: true }).click();
  await expect(row).toContainText('REFUNDED');
  await page.getByRole('button', { name: 'Refresh appointment status' }).click();
  await expect(page.getByText('Demo refund recorded.', { exact: true })).toBeVisible();
  await staff.getByRole('button', { name: 'Sign out' }).click();
  await expect(staff.getByLabel('Password')).toHaveValue('');
  expect(errors).toEqual([]);
});

test('mobile layout fits the viewport and exposes available clinic choices', async ({
  page,
}, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  await expect(page.locator('.branch-card')).toHaveCount(6);
  await expect(page.locator('.site-header')).toHaveCSS('display', 'flex');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({ path: testInfo.outputPath('landing-mobile.png'), fullPage: true });
});

test('a private booking cannot be opened in a new browser session without its token', async ({
  page,
  request,
}) => {
  await page.goto('/booking/00000000-0000-0000-0000-000000000001');
  await expect(page.getByLabel('Management token')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Pay demo deposit & confirm' })).toHaveCount(0);
  const missing = await request.get('/api/not-a-route');
  expect(missing.status()).toBe(404);
  expect(missing.headers()['content-type']).toContain('json');
});
