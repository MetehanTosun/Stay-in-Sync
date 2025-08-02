import { Component } from '@angular/core';
import {GraphPanelComponent} from './graph-panel/graph-panel.component';
import {Splitter} from 'primeng/splitter';
import {PrimeTemplate} from 'primeng/api';
import {SearchBarComponent} from './search-bar/search-bar.component';
import {Tab, TabList, Tabs} from 'primeng/tabs';
import {RouterLink, RouterOutlet} from '@angular/router';


@Component({
  selector: 'app-dashboard',
  imports: [
    GraphPanelComponent,
    Splitter,
    PrimeTemplate,
    SearchBarComponent,
    Tabs,
    TabList,
    RouterLink,
    Tab,
    RouterOutlet,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  selectedNodeId: string | null = null;

  onNodeSelected(nodeId: string | null) {
    this.selectedNodeId = nodeId;
  }

  searchTerm = '';
  tabs = [
    { route: '/dashboard/logs', label: 'Logs Panel', icon: 'pi pi-file' },
    { route: '/dashboard/metrics', label: 'Metrics Panel', icon: 'pi pi-chart-line' },
    { route: '/dashboard/snapshots', label: 'Snapshots Panel', icon: 'pi pi-camera' },
    { route: '/dashboard/replay', label: 'Replay Panel', icon: 'pi pi-refresh' }
  ];

  onSearch(term: string) {
    this.searchTerm = term;
  }

}
