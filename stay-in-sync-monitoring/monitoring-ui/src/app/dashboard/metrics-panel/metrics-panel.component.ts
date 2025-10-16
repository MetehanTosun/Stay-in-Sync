import { Component, OnInit } from '@angular/core';
import { SafeUrlPipe } from './safe.url.pipe';
import { ActivatedRoute } from '@angular/router';
import { TransformationService } from '../../core/services/transformation.service';
import { ConfigService } from '../../core/services/config.service';

/**
 * **MetricsPanelComponent**
 *
 * This component is responsible for displaying Grafana dashboards
 * based on the selected node in the application.
 *
 * It dynamically constructs a Grafana iframe URL depending on:
 * - The selected node ID (from route query parameters)
 * - Whether the node represents a polling node or a sync node
 * - The list of transformations retrieved from the backend
 *
 * ### Key responsibilities:
 * - Fetch the base Grafana URL from the backend (`ConfigService`)
 * - Listen to route changes to detect selected node updates
 * - Fetch transformations for the selected node (`TransformationService`)
 * - Dynamically build the Grafana dashboard URL with appropriate parameters
 *
 * ### Grafana parameter logic:
 * - If **no node** is selected → show dashboard with default variables.
 * - If a **polling node** is selected → use its name as `WorkerpodName`.
 * - If a **sync node** is selected → include its transformation IDs.
 *
 * The final URL is bound to an `<iframe>` in the template, displayed via `SafeUrlPipe`.
 */
@Component({
  selector: 'app-metrics-panel',
  standalone: true,
  imports: [SafeUrlPipe],
  templateUrl: './metrics-panel.component.html',
  styleUrls: ['./metrics-panel.component.css']
})
export class MetricsPanelComponent implements OnInit {
  /** The ID of the currently selected node (from query parameters). */
  selectedNodeId!: string;

  /** Indicates whether the selected node is a polling node. */
  isPollingNode: boolean = false;

  /** The name of the polling node (if applicable). */
  pollingNodeName: string = '';

  /** List of transformation IDs associated with the selected node. */
  transformationIds: (number | undefined)[] = [];

  /** The fully constructed Grafana dashboard URL. */
  grafanaUrl: string = '';

  /** The base Grafana URL, fetched from the backend. */
  grafanaBaseUrl: string = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly transformationService: TransformationService,
    private readonly configService: ConfigService
  ) {}

  /**
   * Lifecycle hook — initializes the component:
   * - Fetches Grafana base URL from the backend
   * - Subscribes to route query parameters
   * - Builds the dashboard URL based on selected node type
   */
  ngOnInit() {
    this.configService.getGrafanaBaseUrl().then(baseUrl => {
      this.grafanaBaseUrl = baseUrl;

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
    });
  }

  /**
   * Fetches transformations for a given node and rebuilds the Grafana URL.
   *
   * @param nodeId - The ID of the node for which transformations should be loaded.
   */
  private loadTransformationsAndBuildUrl(nodeId: string) {
    this.transformationService.getTransformations(nodeId).subscribe({
      next: (transformations) => {
        this.transformationIds = transformations.map(t => t.id);
        this.buildGrafanaUrl();
      },
      error: (err) => {
        console.error('Error loading transformations:', err);
        this.buildGrafanaUrl();
      }
    });
  }

  /**
   * Constructs the Grafana dashboard URL based on the selected node type
   * and transformation data. Ensures that the URL contains valid variables
   * for worker pods and transformation IDs.
   */
  private buildGrafanaUrl() {
    const orgId = 1;
    const from = Date.now() - 60 * 60 * 1000;
    const to = Date.now();
    const refresh = 'auto';

    let urlParams = `orgId=${orgId}&from=${from}&to=${to}&refresh=${refresh}&theme=light`;

    if (!this.selectedNodeId) {
      // Default: No node selected
      urlParams += `&var-WorkerpodName=$__all&var-transformationId=$__all`;

    } else if (this.isPollingNode) {
      // Polling node: use the node name as WorkerpodName
      urlParams += `&var-WorkerpodName=${this.pollingNodeName}&var-transformationId=$__all`;

    } else if (this.transformationIds.length > 0) {
      // Sync node: include transformation IDs
      const transformationVars = this.transformationIds
        .map(id => `var-transformationId=${id}`)
        .join('&');
      urlParams += `&var-WorkerpodName=$__all&${transformationVars}`;

    } else {
      // Fallback: no transformations found
      urlParams += `&var-WorkerpodName=$__all&var-transformationId=$__all`;
    }

    this.grafanaUrl = `${this.grafanaBaseUrl}?${urlParams}`;
    console.log('Grafana URL:', this.grafanaUrl);
  }
}
