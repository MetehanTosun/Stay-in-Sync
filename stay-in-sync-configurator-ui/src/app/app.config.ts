// biome-ignore lint/style/useImportType: <explanation>
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {providePrimeNG} from "primeng/config";
import { provideHttpClient } from '@angular/common/http';
import {MessageService} from 'primeng/api';
import {MyPreset} from './mypreset';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    MessageService,
    providePrimeNG({
    theme: {
      preset: MyPreset,
      options: {
        darkModeSelector: '.my-app-dark'
      }
    },
  })]
};
