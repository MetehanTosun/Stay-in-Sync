import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'log-table', loadComponent: () => import("./dashboard/logs-panel/logs-panel.component").then(m => m.LogsPanelComponent) },
  { path: 'metrics-view', loadComponent: () => import('./dashboard/metrics-panel/metrics-panel.component').then(m => m.MetricsPanelComponent) },
  { path: 'replay', loadComponent: () => import('./dashboard/replay-panel/replay-panel.component').then(m => m.ReplayPanelComponent) }
];
