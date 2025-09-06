import { Component } from '@angular/core';
import {SafeUrlPipe} from './safe.url.pipe';

@Component({
  selector: 'app-metrics-panel',
  standalone: true,
  imports: [SafeUrlPipe],   // ✅ Pipe lokal importieren
  templateUrl: './metrics-panel.component.html',
  styleUrls: ['./metrics-panel.component.css']
})
export class MetricsPanelComponent {
  selectedSyncJobId = '5678';

  get grafanaUrl(): string {
    return `"http://localhost:3000/d/abcd1234/metrics?var-syncJobId=${this.selectedSyncJobId}`;
  }
}
