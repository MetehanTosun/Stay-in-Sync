import { Component, OnInit } from '@angular/core';
import { SafeUrlPipe } from './safe.url.pipe';
import { ActivatedRoute } from '@angular/router';
import { TransformationService } from '../../core/services/transformation.service';

@Component({
  selector: 'app-metrics-panel',
  standalone: true,
  imports: [SafeUrlPipe],
  templateUrl: './metrics-panel.component.html',
  styleUrls: ['./metrics-panel.component.css']
})
export class MetricsPanelComponent implements OnInit {

  selectedNodeId!: string;
  isPollingNode: boolean = false;
  pollingNodeName: string = '';
  transformationIds: (number | undefined)[] = [];
  grafanaUrl: string = '';

  constructor(
    private route: ActivatedRoute,
    private transformationService: TransformationService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'] || '';

      if (!this.selectedNodeId) {
        // Keine Node ausgewählt, Standard-URL bauen
        this.buildGrafanaUrl();
        return;
      }

      // Prüfen, ob es ein PollingNode ist
      if (this.selectedNodeId.startsWith('POLL_')) {
        this.isPollingNode = true;
        this.pollingNodeName = this.selectedNodeId.replace('POLL_', '');
        this.buildGrafanaUrl();
      } else {
        // Standard: Transformationen holen
        this.isPollingNode = false;
        this.loadTransformationsAndBuildUrl(this.selectedNodeId);
      }
    });
  }

  private loadTransformationsAndBuildUrl(nodeId: string) {
    this.transformationService.getTransformations(nodeId).subscribe({
      next: (transformations) => {
        this.transformationIds = transformations.map(t => t.id);
        this.buildGrafanaUrl();
      },
      error: (err) => {
        console.error('Fehler beim Laden der Transformationen', err);
        this.buildGrafanaUrl(); // URL trotzdem bauen
      }
    });
  }

  private buildGrafanaUrl() {
    const baseUrl = 'http://localhost:3000/d/abd0d0fc-75cf-4a29-abeb-7c96b57a1629/stayinsync-monitoring-metrics';
    const orgId = 1;
    const from = Date.now() - 60 * 60 * 1000; // letzte Stunde
    const to = Date.now();
    const refresh = 'auto';

    let urlParams = `orgId=${orgId}&from=${from}&to=${to}&refresh=${refresh}&theme=light`;

    if (this.isPollingNode) {
      // PollingNode-Parameter
      urlParams += `&var-${this.pollingNodeName}=1`;
    } else if (this.transformationIds.length > 0) {
      // Transformation IDs
      urlParams += '&' + this.transformationIds.map(id => `var-transformationId=${id}`).join('&');
    }

    this.grafanaUrl = `${baseUrl}?${urlParams}`;
    console.log('Grafana URL:', this.grafanaUrl);
  }
}
