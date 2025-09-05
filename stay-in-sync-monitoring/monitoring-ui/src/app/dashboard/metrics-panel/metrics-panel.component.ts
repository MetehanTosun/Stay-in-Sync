import { Component } from '@angular/core';

@Component({
  selector: 'app-metrics-panel',
  imports: [],
  templateUrl: './metrics-panel.component.html',
  styleUrl: './metrics-panel.component.css'
})
export class MetricsPanelComponent {

  selectedSyncJobId = '5678';
  grafanaUrl = `http://grafana.example.com/d/abcd1234/metrics?var-syncJobId=${this.selectedSyncJobId}`;

}
