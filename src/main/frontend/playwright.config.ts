import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  expect: { timeout: 12_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env['E2E_BASE_URL'] || 'http://localhost:8080',
    ...devices['Desktop Chrome'],
    timezoneId: 'America/New_York', // Clinic times must not depend on the visitor's zone.
    channel: process.env['PLAYWRIGHT_CHANNEL'],
    screenshot: 'only-on-failure',
    trace: 'off', // Traces include private request headers; keep capability tokens out of artifacts.
  },
});
