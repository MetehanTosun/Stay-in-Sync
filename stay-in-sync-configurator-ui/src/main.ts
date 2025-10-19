import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));

export const APP_CONFIG = {
  apiUrl: 'http://localhost:8090/api/config'  // dein Quarkus‑Base‑Pfad
};