import { Routes } from '@angular/router';
import { LogsPanelComponent } from './dashboard/logs-panel/logs-panel.component';
import { MetricsPanelComponent } from './dashboard/metrics-panel/metrics-panel.component';
import { ErrorSnapshotPanelComponent } from './dashboard/error-snapshot-panel/error-snapshot-panel.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {ReplayFullpageComponent} from './dashboard/replay-fullpage/replay-fullpage.component';

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
    component: ReplayFullpageComponent
  },
  { path: '**', redirectTo: '' }
];

