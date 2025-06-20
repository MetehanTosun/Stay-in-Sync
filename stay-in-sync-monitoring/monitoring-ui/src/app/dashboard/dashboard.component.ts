import { Component } from '@angular/core';
import {GraphPanelComponent} from './graph-panel/graph-panel.component';
import {LogsPanelComponent} from './logs-panel/logs-panel.component';
import {MetricsPanelComponent} from './metrics-panel/metrics-panel.component';
import {Splitter} from 'primeng/splitter';
import {PrimeTemplate} from 'primeng/api';
import {SearchBarComponent} from './search-bar/search-bar.component';


@Component({
  selector: 'app-dashboard',
  imports: [
    GraphPanelComponent,
    LogsPanelComponent,
    MetricsPanelComponent,
    Splitter,
    PrimeTemplate,
    SearchBarComponent,
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

  onSearch(term: string) {
    this.searchTerm = term;
  }

}
