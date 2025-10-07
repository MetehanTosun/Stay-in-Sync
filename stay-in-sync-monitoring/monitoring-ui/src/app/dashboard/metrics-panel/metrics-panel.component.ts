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

  // Currently selected node ID from query parameters
  selectedNodeId!: string;

  // True if the selected node is a "Polling Node"
  isPollingNode: boolean = false;

  // The name of the polling node (without "POLL_" prefix)
  pollingNodeName: string = '';

  // IDs of transformations associated with the selected node
  transformationIds: (number | undefined)[] = [];

  // The Grafana URL dynamically built based on node/transformations
  grafanaUrl: string = '';

  constructor(
    private route: ActivatedRoute,
    private transformationService: TransformationService
  ) {}

  /**
   * Initializes the component.
   * Subscribes to query parameters and builds the Grafana URL
   * depending on the selected node type (polling or transformation).
   */
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'] || '';

      if (!this.selectedNodeId) {
        // No node selected → build default Grafana URL
        this.buildGrafanaUrl();
        return;
      }

      // Handle polling nodes
      if (this.selectedNodeId.startsWith('POLL_')) {
        this.isPollingNode = true;
        this.pollingNodeName = this.selectedNodeId.replace('POLL_', '');
        this.buildGrafanaUrl();
      } else {
        // Otherwise: load transformations for the node
        this.isPollingNode = false;
        this.loadTransformationsAndBuildUrl(this.selectedNodeId);
      }
    });
  }

  /**
   * Loads transformations for the given node ID and builds the Grafana URL.
   * If the request fails, a fallback URL is still built.
   */
  private loadTransformationsAndBuildUrl(nodeId: string) {
    this.transformationService.getTransformations(nodeId).subscribe({
      next: (transformations) => {
        this.transformationIds = transformations.map(t => t.id);
        this.buildGrafanaUrl();
      },
      error: (err) => {
        console.error('Error while loading transformations', err);
        this.buildGrafanaUrl(); // Build URL even if loading fails
      }
    });
  }

  /**
   * Builds the Grafana URL depending on the context:
   * - Default: last hour, light theme
   * - Polling Node: add var-[pollingNodeName] parameter
   * - Transformation Node: add var-transformationId parameters
   */
  private buildGrafanaUrl() {
    const baseUrl = 'http://localhost:3000/d/abd0d0fc-75cf-4a29-abeb-7c96b57a1629/stayinsync-monitoring-metrics';
    const orgId = 1;
    const from = Date.now() - 60 * 60 * 1000; // last hour
    const to = Date.now();
    const refresh = 'auto';

    let urlParams = `orgId=${orgId}&from=${from}&to=${to}&refresh=${refresh}&theme=light`;

    if (this.isPollingNode) {
      // Add polling node parameter
      urlParams += `&var-${this.pollingNodeName}=1`;
    } else if (this.transformationIds.length > 0) {
      // Add transformation IDs
      urlParams += '&' + this.transformationIds.map(id => `var-transformationId=${id}`).join('&');
    }

    this.grafanaUrl = `${baseUrl}?${urlParams}`;
    console.log('Grafana URL:', this.grafanaUrl);
  }
}
