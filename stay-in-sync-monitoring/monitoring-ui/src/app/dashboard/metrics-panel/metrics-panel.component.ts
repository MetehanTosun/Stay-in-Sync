import { Component, OnInit } from '@angular/core';
import { SafeUrlPipe } from './safe.url.pipe';
import { ActivatedRoute } from '@angular/router';
import { TransformationService } from '../../core/services/transformation.service';
import { ConfigService } from '../../core/services/config.service';

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
  grafanaBaseUrl: string = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly transformationService: TransformationService,
    private readonly configService: ConfigService
  ) {}

  async ngOnInit(): Promise<void> {
    // BaseUrl zuerst vom Backend holen
    this.grafanaBaseUrl = await this.configService.getGrafanaBaseUrl();

    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'] || '';

      if (!this.selectedNodeId) {
        this.buildGrafanaUrl();
        return;
      }

      if (this.selectedNodeId.startsWith('POLL_')) {
        this.isPollingNode = true;
        this.pollingNodeName = this.selectedNodeId.replace('POLL_', '');
        this.buildGrafanaUrl();
      } else {
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
        this.buildGrafanaUrl();
      }
    });
  }

  private buildGrafanaUrl() {
    const orgId = 1;
    const from = Date.now() - 60 * 60 * 1000;
    const to = Date.now();
    const refresh = 'auto';

    let urlParams = `orgId=${orgId}&from=${from}&to=${to}&refresh=${refresh}&theme=light`;

    if (this.isPollingNode) {
      urlParams += `&var-${this.pollingNodeName}=1`;
    } else if (this.transformationIds.length > 0) {
      urlParams += '&' + this.transformationIds.map(id => `var-transformationId=${id}`).join('&');
    }

    this.grafanaUrl = `${this.grafanaBaseUrl}?${urlParams}`;
    console.log('Grafana URL:', this.grafanaUrl);
  }
}
