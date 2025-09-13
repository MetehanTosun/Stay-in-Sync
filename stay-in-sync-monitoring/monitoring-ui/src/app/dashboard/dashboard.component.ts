import { Component } from '@angular/core';
import {GraphPanelComponent} from './graph-panel/graph-panel.component';
import {Splitter} from 'primeng/splitter';
import {PrimeTemplate} from 'primeng/api';
import {SearchBarComponent} from './search-bar/search-bar.component';
import {Tab, TabList, Tabs} from 'primeng/tabs';
import {Router, RouterLink, RouterOutlet} from '@angular/router';


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

  constructor(private router: Router) {}

  onNodeSelected(nodeId: string | null) {
    this.selectedNodeId = nodeId;
    if (nodeId) {
      this.router.navigate([], {
        queryParams: { input: nodeId },
        queryParamsHandling: 'merge',
      });
    }
  }

  searchTerm = '';
  tabs = [
    { route: '/log-table', label: 'Logs Panel', icon: 'pi pi-file' },
    { route: '/metrics-view', label: 'Metrics Panel', icon: 'pi pi-chart-line' },
    { route: '/error-snapshots', label: 'Error Snapshots', icon: 'pi pi-camera' }
  ];

  onSearch(term: string) {
    this.searchTerm = term;
  }
}
