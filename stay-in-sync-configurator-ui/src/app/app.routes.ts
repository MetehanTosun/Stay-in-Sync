import { Routes } from '@angular/router';
import { ConfigurationscriptsBaseComponent } from './features/configuration/configurationscripts-base/configurationscripts-base.component';
import { SourceSystemBaseComponent } from
    './features/source-system/components/source-system-base/source-system-base.component';
import { CreateSourceSystemComponent } from
    './features/source-system/components/create-source-system/create-source-system.component';
import { EdcBaseComponent } from './features/edc/components/edc-base/edc-base.component';
import {HelpPageComponent} from './features/help-page/help-page.component';
import {SyncJobPageComponent} from './features/sync-job/components/sync-job-page/sync-job-page.component';
import {SyncJobCreationComponent} from './features/sync-job/components/sync-job-creation/sync-job-creation.component';
import {
  TransformationRuleSelectionComponent
} from './features/transformation/components/transformation-rule-selection/transformation-rule-selection.component';

import { ScriptEditorPageComponent } from './features/script-editor/script-editor-page/script-editor-page.component';
import {
  TransformationScriptSelectionComponent
} from './features/transformation/components/transformation-script-selection/transformation-script-selection.component';
import {
  SyncJobDetailsPageComponent
} from './features/sync-job/components/sync-job-details-page/sync-job-details-page.component';
import { RulesOverviewComponent } from './features/sync-rules/pages/rules-overview/rules-overview.component';
import { EditRuleComponent } from './features/sync-rules/pages/edit-rule/edit-rule.component';
import { EditRuleDeactivateGuard } from './features/sync-rules/pages/edit-rule/edit-rule-deactivate.guard';

// Target System
import { TargetSystemBaseComponent } from './features/target-system/components/target-system-base/target-system-base.component';

export const routes: Routes = [
  // Route für Sync Rules
  { path: 'sync-rules', component: RulesOverviewComponent },
  { path: 'sync-rules/edit-rule/:id', component: EditRuleComponent, canDeactivate: [EditRuleDeactivateGuard] },

  // Route für Transformation Scripts
  { path: 'transformation-scripts', component: ConfigurationscriptsBaseComponent },

  // Route für Script Editor
  { path: 'script-editor/:transformationId', component: ScriptEditorPageComponent},

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

  // Route für Target System
  {
    path: 'target-system',
    component: TargetSystemBaseComponent
  },

  //Route für Sync Jobs
  {path: 'sync-jobs', component: SyncJobPageComponent,
  children: [
    {
      path: 'create',
      component: SyncJobCreationComponent
    },
    {
      path: 'edit/:id',
      component: SyncJobCreationComponent
    },
    {
      path: 'create/rule/:transformationId',
      component: TransformationRuleSelectionComponent
    },
    {
      path: 'create/script/:transformationId',
      component: TransformationScriptSelectionComponent
    }
  ]
  },
  { path: 'sync-jobs/:id', component: SyncJobDetailsPageComponent},

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
