import { Routes } from '@angular/router';
import {LogsPanelComponent} from './dashboard/logs-panel/logs-panel.component';
import {MetricsPanelComponent} from './dashboard/metrics-panel/metrics-panel.component';
import {ReplayPanelComponent} from './dashboard/replay-panel/replay-panel.component';

export const routes: Routes = [
  { path: '', redirectTo: '/log-table', pathMatch: 'full' },
  { path: 'log-table', component: LogsPanelComponent},
  { path: 'metrics-view', component: MetricsPanelComponent},
  { path: 'replay', component: ReplayPanelComponent },
  { path: '**', redirectTo: '/log-table' }

];
