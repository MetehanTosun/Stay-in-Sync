import { Component } from '@angular/core';
import { GraphPanelComponent } from './graph-panel/graph-panel.component';
import { Splitter } from 'primeng/splitter';
import { PrimeTemplate } from 'primeng/api';
import { SearchBarComponent } from './search-bar/search-bar.component';
import { Tab, TabList, Tabs } from 'primeng/tabs';
import { Router, RouterLink, RouterOutlet } from '@angular/router';

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
  // Currently selected node ID (from GraphPanel), or null if none is selected
  selectedNodeId: string | null = null;

  // Current search term from the SearchBar
  searchTerm = '';

  // List of tabs with their routes, labels, and icons
  tabs = [
    { route: '/log-table', label: 'Logs Panel', icon: 'pi pi-file' },
    { route: '/metrics-view', label: 'Metrics Panel', icon: 'pi pi-chart-line' },
    { route: '/error-snapshots', label: 'Error Snapshots', icon: 'pi pi-camera' }
  ];

  constructor(private router: Router) {}

  /**
   * Handles node selection from the GraphPanel.
   * Updates the query parameters in the URL with the selected node ID.
   * If no node is selected, the parameter is cleared.
   */
  onNodeSelected(nodeId: string | null) {
    this.selectedNodeId = nodeId;
    if (nodeId) {
      this.router.navigate([], {
        queryParams: { input: nodeId },
        queryParamsHandling: 'merge',
      });
    } else {
      this.router.navigate([], {
        queryParams: { input: null },
        queryParamsHandling: 'merge',
      });
    }
  }

  /**
   * Handles search events from the SearchBar.
   * Stores the provided search term locally.
   */
  onSearch(term: string) {
    this.searchTerm = term;
  }
}
