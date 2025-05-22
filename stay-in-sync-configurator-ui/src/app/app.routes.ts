import { Routes } from '@angular/router';
import { SyncRulesComponent } from './sync-rules/sync-rules.component';
import {ConfigBaseComponent} from './config-base/config-base.component';
import {ConfigurationscriptsBaseComponent} from './configurationscripts-base/configurationscripts-base.component';
import {AssBaseComponent} from './ass/ass-base/ass-base.component';
import {EdcBaseComponent} from './edc/edc-base/edc-base.component';

export const routes: Routes = [
  {path: 'sync-rules', component : SyncRulesComponent},
  {path: 'configs', component: ConfigBaseComponent},
  {path: 'transformation-scripts', component: ConfigurationscriptsBaseComponent},
  {path: 'ass', component:AssBaseComponent},
  {path:'edc', component: EdcBaseComponent}
];
