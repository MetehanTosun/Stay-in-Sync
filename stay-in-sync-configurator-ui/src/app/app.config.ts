// biome-ignore lint/style/useImportType: <explanation>
import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {providePrimeNG} from "primeng/config";
import { provideHttpClient } from '@angular/common/http';
import {MessageService} from 'primeng/api';
import {MyPreset} from './mypreset';
import Material from '@primeng/themes/material';
import Aura from '@primeng/themes/aura';
import Lara from '@primeng/themes/lara';
import Nora from  '@primeng/themes/nora';
import { provideMarkdown } from 'ngx-markdown';

import { MonacoEditorModule, NgxMonacoEditorConfig } from 'ngx-monaco-editor-v2';

const monacoConfig: NgxMonacoEditorConfig = {
  baseUrl: window.location.origin + '/assets/monaco/vs',
  defaultOptions: { scrollBeyondLastLine: false },
  onMonacoLoad: () => { console.log((<any>window).monaco); },
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    MessageService,
    provideMarkdown(),
    importProvidersFrom(MonacoEditorModule.forRoot(monacoConfig)),
    providePrimeNG({
    theme: {
      preset: MyPreset,
      options: {
        darkModeSelector: '.my-app-dark'
      }
    },
  })]
};
