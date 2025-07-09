// biome-ignore lint/style/useImportType: <explanation>
import { Routes } from '@angular/router';
import { SyncRulesComponent } from './features/sync-rules/components/sync-rules/sync-rules.component';
import { ConfigBaseComponent } from './features/configuration/config-base/config-base.component';
import { ConfigurationscriptsBaseComponent } from './features/configuration/configurationscripts-base/configurationscripts-base.component';
// neu (relativ zu src/app/app.routes.ts)
import { SourceSystemBaseComponent } from
  './features/source-system/components/source-system-base/source-system-base.component';
import { CreateSourceSystemComponent } from
  './features/source-system/components/create-source-system/create-source-system.component';
import { EdcBaseComponent } from './features/edc/components/edc-base/edc-base.component';
import { RulesOverview } from './features/sync-rules/pages/rules-overview/rules-overview';
import { EditRule } from './features/sync-rules/pages/edit-rule/edit-rule';



export const routes: Routes = [
  // Route für Sync Rules
  {
    path: 'sync-rules',
    component: SyncRulesComponent,
    children: [
      { path: '', component: RulesOverview },
      { path: 'edit-rule/:id', component: EditRule },
    ]
  },

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
  { path: '', redirectTo: 'source-system', pathMatch: 'full' },
  { path: '**', redirectTo: 'source-system' },

  // Route für EDC
  { path: 'edc', component: EdcBaseComponent },

  // Standard-Redirect (optional, falls keine Route passt)
  { path: '', redirectTo: '/sync-rules', pathMatch: 'full' },

  // Fallback-Route für nicht gefundene Seiten
  { path: '**', redirectTo: '/sync-rules' }
];
