// biome-ignore lint/style/useImportType: <explanation>
import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {providePrimeNG} from "primeng/config";
import { provideHttpClient } from '@angular/common/http';
import {ConfirmationService, MessageService} from 'primeng/api';
import {MyPreset} from './mypreset';
import {provideMarkdown} from 'ngx-markdown';

import { MonacoEditorModule, NgxMonacoEditorConfig } from 'ngx-monaco-editor-v2';

const monacoConfig: NgxMonacoEditorConfig = {
  baseUrl: window.location.origin + '/assets/monaco/vs',
  defaultOptions: { scrollBeyondLastLine: false },
  onMonacoLoad: () => { console.log((<any>window).monaco); },
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideZoneChangeDetection({eventCoalescing: true}),
    provideRouter(routes),
    provideAnimationsAsync(),
    MessageService,
    ConfirmationService,
    provideMarkdown(),
    importProvidersFrom(MonacoEditorModule.forRoot(monacoConfig)),
    providePrimeNG({
    theme: {
      preset: MyPreset,
      options: {
        darkModeSelector: '.my-app-dark',
      }
    },
  })]
};
