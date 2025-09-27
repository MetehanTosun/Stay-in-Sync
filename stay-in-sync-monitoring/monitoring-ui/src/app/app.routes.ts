import { Routes } from '@angular/router';
import { LogsPanelComponent } from './dashboard/logs-panel/logs-panel.component';
import { MetricsPanelComponent } from './dashboard/metrics-panel/metrics-panel.component';
import { ErrorSnapshotPanelComponent } from './dashboard/error-snapshot-panel/error-snapshot-panel.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {ReplayViewComponent} from './replay/replay-view.component';

export const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    children: [
      { path: 'log-table', component: LogsPanelComponent },
      { path: 'metrics-view', component: MetricsPanelComponent },
      { path: 'error-snapshots', component: ErrorSnapshotPanelComponent },
      { path: '', redirectTo: 'log-table', pathMatch: 'full' }
    ]
  },
  {
    path: 'replay',
    component: ReplayViewComponent
  },
  { path: '**', redirectTo: '' }
];

