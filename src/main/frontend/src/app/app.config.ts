import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { DATE_PIPE_DEFAULT_OPTIONS } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { CLINIC_UTC_OFFSET } from './core/clinic-date';
export const appConfig: ApplicationConfig = {
  providers: [
    { provide: DATE_PIPE_DEFAULT_OPTIONS, useValue: { timezone: CLINIC_UTC_OFFSET } },
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
  ],
};
