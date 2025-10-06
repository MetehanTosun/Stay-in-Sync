import { provideHttpClient } from '@angular/common/http';
import {
  ApplicationConfig,
  importProvidersFrom,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import {
  MonacoEditorModule,
  NgxMonacoEditorConfig,
} from 'ngx-monaco-editor-v2';
import { providePrimeNG } from 'primeng/config';
import { MyPreset } from '../../../../stay-in-sync-configurator-ui/src/app/mypreset';
import { routes } from './app.routes';

const monacoConfig: NgxMonacoEditorConfig = {
  baseUrl: window.location.origin + '/assets/monaco/vs',
  defaultOptions: { scrollBeyondLastLine: false },
  onMonacoLoad: () => {
    console.log((<any>window).monaco);
  },
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    importProvidersFrom(MonacoEditorModule.forRoot(monacoConfig)),

    providePrimeNG({
      theme: {
        preset: MyPreset,
        options: {
          darkModeSelector: '.my-app-dark',
        },
      },
    }),
  ],
};
