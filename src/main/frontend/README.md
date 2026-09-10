# Lumina Angular application

Standalone Angular 22 with strict TypeScript/templates, lazy routes, signals and reactive forms. Feature components use the typed ClinicApi boundary; BookingAccess keeps capabilities in memory/session storage. Booking/payment retries preserve keys after lost responses. Availability uses switchMap to discard stale responses.

The root Maven build compiles and tests Angular, then embeds dist/lumina/browser into Spring Boot. See the [root README](../../../README.md) for complete instructions.

With Node 24.15+ (24.x): npm ci, then npm start. The development server proxies /api to localhost:8080.

- npm run build: production compilation and bundle budgets.
- npm run test:ci: Angular/Vitest tests without watch mode.
- npm run test:e2e: Playwright against the running packaged app.

The interface labels simulated payments, displays the clinic time zone, and supports keyboard navigation, responsive layouts, loading/error/empty states. No card details or identity documents are collected. Confirmation emails are not sent.
