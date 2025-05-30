// biome-ignore lint/style/useImportType: <explanation>
import { Routes } from '@angular/router';
import { SyncRulesComponent } from './features/sync-rules/components/sync-rules.component';
import { ConfigBaseComponent } from './features/configuration/config-base/config-base.component';
import { ConfigurationscriptsBaseComponent } from './features/configuration/configurationscripts-base/configurationscripts-base.component';
// neu (relativ zu src/app/app.routes.ts)
import { AasBaseComponent } from './features/source-system/components/aas-base/aas-base.component';
import { EdcBaseComponent } from './features/edc/components/edc-base/edc-base.component';
import { CreateSourceSystemComponent } from
    './features/source-system/components/create-source-system/create-source-system.component';
import { ScriptEditorPageComponent } from './features/script-editor/script-editor-page/script-editor-page.component';


export const routes: Routes = [
  // Route für Sync Rules
  { path: 'sync-rules', component: SyncRulesComponent },

  // Route für Configurations
  { path: 'configs', component: ConfigBaseComponent },

  // Route für Transformation Scripts
  { path: 'transformation-scripts', component: ConfigurationscriptsBaseComponent },

  // Route für Script Editor
  { path: 'script-editor', component: ScriptEditorPageComponent},

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
