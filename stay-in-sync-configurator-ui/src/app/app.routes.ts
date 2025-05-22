// biome-ignore lint/style/useImportType: <explanation>
import { Routes } from '@angular/router';
import { SyncRulesComponent } from './sync-rules/sync-rules.component';
import { ConfigBaseComponent } from './config-base/config-base.component';
import { ConfigurationscriptsBaseComponent } from './configurationscripts-base/configurationscripts-base.component';
import { AasBaseComponent } from './aas/aas-base/aas-base.component';
import { EdcBaseComponent } from './edc/edc-base/edc-base.component';
import { CreateSourceSystemComponent } from './aas/aas-base/create-source-system/create-source-system.component';


export const routes: Routes = [
  // Route für Sync Rules
  { path: 'sync-rules', component: SyncRulesComponent },

  // Route für Configurations
  { path: 'configs', component: ConfigBaseComponent },

  // Route für Transformation Scripts
  { path: 'transformation-scripts', component: ConfigurationscriptsBaseComponent },

  // Route für Asset Administration Shell (ASS)
  {
    path: 'ass',
    component: AasBaseComponent,
    children: [
      {
        path: 'create-source-system',
        component: CreateSourceSystemComponent
      }
    ]
  },

  // Route für EDC
  { path: 'edc', component: EdcBaseComponent },

  // Standard-Redirect (optional, falls keine Route passt)
  { path: '', redirectTo: '/sync-rules', pathMatch: 'full' },

  // Fallback-Route für nicht gefundene Seiten
  { path: '**', redirectTo: '/sync-rules' }
];