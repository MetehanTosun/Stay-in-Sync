// biome-ignore lint/style/useImportType: <explanation>
import { Routes } from '@angular/router';
import { SyncRulesComponent } from './features/sync-rules/components/sync-rules.component';
import { ConfigBaseComponent } from './features/configuration/config-base/config-base.component';
import { ConfigurationscriptsBaseComponent } from './features/configuration/configurationscripts-base/configurationscripts-base.component';
// neu (relativ zu src/app/app.routes.ts)
import { SourceSystemBaseComponent } from
    './features/source-system/components/source-system-base/source-system-base.component';
import { CreateSourceSystemComponent } from
    './features/source-system/components/create-source-system/create-source-system.component';
import { EdcBaseComponent } from './features/edc/components/edc-base/edc-base.component';
import {HelpPageComponent} from './features/help-page/help-page.component';



export const routes: Routes = [
  // Route für Sync Rules
  { path: 'sync-rules', component: SyncRulesComponent },

  // Route für Configurations
  { path: 'configs', component: ConfigBaseComponent },

  // Route für Transformation Scripts
  { path: 'transformation-scripts', component: ConfigurationscriptsBaseComponent },

  // Route für Source System
  {
    path: 'source-system',
    component: SourceSystemBaseComponent,
    children: [
      {
        path: 'create',
        component: CreateSourceSystemComponent
      }
    ]
  },

  // Route für Help mit Children
  {
    path: 'help',
    component: HelpPageComponent
  },
  {
    path: 'help/:topic',
    component: HelpPageComponent
  },

  // Route für EDC
  { path: 'edc', component: EdcBaseComponent },

  // Standard-Redirect
  { path: '', redirectTo: '/sync-rules', pathMatch: 'full' },

  // Fallback-Route
  { path: '**', redirectTo: '/sync-rules' }
];
