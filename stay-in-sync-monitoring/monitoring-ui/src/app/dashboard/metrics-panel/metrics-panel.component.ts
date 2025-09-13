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
  grafanaDashboardUrl: string = '';
  grafanaSpeicherUrl: string = '';

  constructor(
    private route: ActivatedRoute,
    private transformationService: TransformationService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'] || '';

      if (!this.selectedNodeId) {
        this.buildGrafanaUrls();
        return;
      }

      if (this.selectedNodeId.startsWith('POLL_')) {
        this.isPollingNode = true;
        this.pollingNodeName = this.selectedNodeId.replace('POLL_', '');
        this.buildGrafanaUrls();
      } else {
        this.isPollingNode = false;
        this.loadTransformationsAndBuildUrls(this.selectedNodeId);
      }
    });
  }

  private loadTransformationsAndBuildUrls(nodeId: string) {
    this.transformationService.getTransformations(nodeId).subscribe({
      next: (transformations) => {
        this.transformationIds = transformations.map(t => t.id);
        this.buildGrafanaUrls();
      },
      error: (err) => {
        console.error('Fehler beim Laden der Transformationen', err);
        this.buildGrafanaUrls();
      }
    });
  }

  private buildGrafanaUrls() {
    //  Haupt-Dashboard
    const baseUrl = 'http://localhost:3000/d/c0d04c42-641e-438b-8592-f1ca577899dd/quarkus-service-monitoring';
    const orgId = 1;
    const from = Date.now() - 60 * 60 * 1000; // letzte Stunde
    const to = Date.now();
    const refresh = 'auto';

    let urlParams = `orgId=${orgId}&from=${from}&to=${to}&refresh=${refresh}&theme=light`;

    if (this.isPollingNode) {
      urlParams += `&var-${this.pollingNodeName}=1`;
    } else if (this.transformationIds.length > 0) {
      urlParams += '&' + this.transformationIds.map(id => `var-transformationId=${id}`).join('&');
    }

    this.grafanaDashboardUrl = `${baseUrl}?${urlParams}`;


    this.grafanaSpeicherUrl = `http://localhost:3000/d/a26b9626-8e35-480f-b6e1-cf4e66717bb7/speicher?orgId=1&from=${from}&to=${to}&refresh=${refresh}&theme=light`;
  }
}

