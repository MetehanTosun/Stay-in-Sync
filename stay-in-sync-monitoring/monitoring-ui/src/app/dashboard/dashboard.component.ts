import { Component } from '@angular/core';
import {GraphPanelComponent} from './graph-panel/graph-panel.component';
import {LogsPanelComponent} from './logs-panel/logs-panel.component';
import {MetricsPanelComponent} from './metrics-panel/metrics-panel.component';
import {Splitter} from 'primeng/splitter';
import {PrimeTemplate} from 'primeng/api';


@Component({
  selector: 'app-dashboard',
  imports: [
    GraphPanelComponent,
    LogsPanelComponent,
    MetricsPanelComponent,
    Splitter,
    PrimeTemplate,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  selectedNodeId: string | null = null;

  onNodeSelected(nodeId: string | null) {
    this.selectedNodeId = nodeId;
  }

}
