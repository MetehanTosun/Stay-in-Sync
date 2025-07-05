// src/app/app.config.ts (oder wo dein appConfig steht)
// biome-ignore lint/style/useImportType: <explanation>
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { MyPreset } from './mypreset';
import Material from '@primeng/themes/material';
import Aura from '@primeng/themes/aura';
import Lara from '@primeng/themes/lara';
import Nora from '@primeng/themes/nora';
import { provideMarkdown } from 'ngx-markdown';

// Importiere BASE_PATH aus deinem generated-Ordner:
import { BASE_PATH } from './generated/variables';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    MessageService,
    provideMarkdown(),
    providePrimeNG({
      theme: {
        preset: MyPreset,
        options: {
          darkModeSelector: '.my-app-dark'
        }
      },
    }),
    // Hier wird der BASE_PATH gesetzt:
    { provide: BASE_PATH, useValue: 'http://localhost:8090' },
  ]
};