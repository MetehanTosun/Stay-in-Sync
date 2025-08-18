import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'dashboard/logs', loadComponent: () => import("./dashboard/logs-panel/logs-panel.component").then(m => m.LogsPanelComponent) },
  { path: 'dashboard/metrics', loadComponent: () => import('./dashboard/metrics-panel/metrics-panel.component').then(m => m.MetricsPanelComponent) },
  { path: 'dashboard/snapshots', loadComponent: () => import('./dashboard/snapshot-panel/snapshot-panel.component').then(m => m.SnapshotPanelComponent) },
  { path: 'dashboard/replay', loadComponent: () => import('./dashboard/replay-panel/replay-panel.component').then(m => m.ReplayPanelComponent) }
];
