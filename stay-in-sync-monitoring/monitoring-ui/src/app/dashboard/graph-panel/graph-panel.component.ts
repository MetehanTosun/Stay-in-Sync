import { AfterViewInit, Component, EventEmitter, Input, Output } from '@angular/core';
import * as d3 from 'd3';
import type { Node, NodeConnection } from '../../core/models/node.model';
import { LegendPanelComponent } from './legend-panel/legend-panel.component';
import { MonitoringGraphService } from '../../core/services/monitoring-graph.service';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';

/**
 * GraphPanelComponent
 *
 * Angular component responsible for rendering and managing a graph visualization
 * using D3.js. It supports:
 * - Filtering nodes and links by a search term.
 * - Updating the graph dynamically when data changes.
 * - User interactions such as selection, drag-and-drop, zoom, and click navigation.
 */
@Component({
  selector: 'app-graph-panel',
  templateUrl: './graph-panel.component.html',
  imports: [LegendPanelComponent],
  styleUrl: './graph-panel.component.css',
})
export class GraphPanelComponent implements AfterViewInit {
  /**
   * Event emitted when a node is selected or deselected.
   * Emits the ID of the selected node or `null` if no node is selected.
   */
  @Output() nodeSelected = new EventEmitter<string | null>();

  private _searchTerm: string = '';
  private isInitialized: boolean = false;

  /**
   * Tracks nodes that are marked with an error state.
   */
  markedNodes: { [nodeId: string]: boolean } = {};

  constructor(
    private graphService: MonitoringGraphService,
    private router: Router
  ) {}

  /**
   * Input property for the search term.
   * Whenever the search term changes (after initialization),
   * nodes and links are filtered and the graph is updated.
   */
  @Input()
  set searchTerm(value: string) {
    this._searchTerm = value;

    if (this.isInitialized) {
      this.filteredNodes = this.filterNodes(this._searchTerm);
      this.filteredLinks = this.filterLinks();
      this.updateGraph(this.filteredNodes, this.filteredLinks);
    }
  }
  get searchTerm(): string {
    return this._searchTerm;
  }

  /**
   * All nodes in the graph.
   */
  nodes: Node[] = [];

  /**
   * All links (edges) in the graph.
   */
  links: NodeConnection[] = [];

  /**
   * Filtered links based on the search term.
   */
  filteredLinks: NodeConnection[] = [...this.links];

  /**
   * Filtered nodes based on the search term.
   */
  filteredNodes: Node[] = [...this.nodes];

  /**
   * Lifecycle hook called after the component’s view is initialized.
   * - Loads graph data.
   * - Sets up zoom and pan interactions.
   * - Subscribes to SSE events to update node states in real-time.
   */
  ngAfterViewInit() {
    this.loadGraphData();

    // Subscribe to server-sent events for real-time updates
    const evtSource = new EventSource('/events/subscribe');
    evtSource.addEventListener('job-update', (e) => {
      const changedJobIds: number[] = JSON.parse(e.data);

      // Mark nodes belonging to changed jobs as error/active
      this.nodes.forEach((node) => {
        if (node.type === 'SyncNode') {
          node.status = changedJobIds.includes(Number(node.id))
            ? 'error'
            : 'active';
        }
      });

      this.filteredNodes = this.filterNodes(this.searchTerm);
      this.filteredLinks = this.filterLinks();
      this.updateGraph(this.filteredNodes, this.filteredLinks);
    });
  }

  /**
   * Loads graph data from the monitoring service,
   * initializes zoom behavior, and renders the graph.
   */
  loadGraphData() {
    this.graphService.getMonitoringGraphData().subscribe(
      (data) => {
        this.nodes = data.nodes;
        this.links = data.connections ?? [];

        // Apply error state to marked nodes
        this.nodes.forEach((node) => {
          if (this.markedNodes[node.id]) {
            node.status = 'error';
          }
        });

        this.filteredNodes = this.filterNodes(this.searchTerm);
        this.filteredLinks = this.filterLinks();
        this.updateGraph(this.filteredNodes, this.filteredLinks);
        this.isInitialized = true;

        const svgElement = document.querySelector('svg');
        const width = svgElement?.clientWidth ?? 400;
        const height = svgElement?.clientHeight ?? 300;

        const svg = d3
          .select('svg')
          .attr('width', '100%')
          .attr('height', '100%');

        const container = svg.append('g');

        // Enable zooming and panning
        svg.call(
          d3
            .zoom<any, unknown>()
            .scaleExtent([0.5, 5])
            .on('zoom', (event) => {
              container.attr('transform', event.transform);
            })
        );

        this.renderGraph(container, this.nodes, this.links, width, height);
      },
      (error) => {
        console.error('Error loading graph data:', error);
      }
    );
  }

  /**
   * Filters nodes by the given search term.
   * TODO: Replace ID/type filtering with sync-job-based filtering.
   *
   * @param term Search term.
   * @returns Nodes matching the search term.
   */
  filterNodes(term: string): Node[] {
    if (term === '') {
      return [...this.nodes];
    }
    return this.nodes.filter(
      (node) =>
        node.id.toLowerCase().includes(term.toLowerCase()) ||
        node.type.toLowerCase().includes(term.toLowerCase())
    );
  }

  /**
   * Filters links so only those connecting currently filtered nodes remain.
   *
   * @returns Filtered array of links.
   */
  filterLinks(): NodeConnection[] {
    return this.links.filter(
      (link) =>
        this.filteredNodes.includes(link.source as Node) &&
        this.filteredNodes.includes(link.target as Node)
    );
  }

  /**
   * Updates the graph visualization with new nodes and links.
   *
   * @param newNodes Nodes to render.
   * @param newLinks Links to render.
   */
  updateGraph(newNodes: Node[], newLinks: NodeConnection[]) {
    const svg = d3.select('svg');
    const container = svg.select<SVGGElement>('g');

    container.selectAll('line').remove();
    container.selectAll('g').remove();

    const svgNode = svg.node();
    const width =
      svgNode instanceof SVGSVGElement ? window.innerWidth : 400;
    const height =
      svgNode instanceof SVGSVGElement ? svgNode.clientHeight : 300;

    this.renderGraph(container, newNodes, newLinks, width, height);
  }

  /**
   * Renders the graph.
   * - Creates shapes for nodes based on type.
   * - Adds labels and tooltips.
   * - Sets up drag behavior, zoom, and node click interactions.
   *
   * @param container D3 container group element (<g>).
   * @param nodes Nodes to render.
   * @param links Links to render.
   * @param width Width of the SVG area.
   * @param height Height of the SVG area.
   */
  private renderGraph(
    container: d3.Selection<SVGGElement, unknown, HTMLElement, any>,
    nodes: Node[],
    links: NodeConnection[],
    width: number,
    height: number
  ) {
    const tooltip = d3.select('#tooltip');

    // Draw links
    const link = container
      .append('g')
      .selectAll('line')
      .data(links)
      .enter()
      .append('line')
      .attr('stroke', '#999');

    // Draw nodes
    const nodeGroup = container
      .append('g')
      .selectAll('g')
      .data(nodes)
      .enter()
      .append('g');

    // Node shapes and labels
    nodeGroup.each(function (d: Node) {
      let shape: any;
      if (d.type === 'SourceSystem' || d.type === 'ASS') {
        shape = d3
          .select(this)
          .append('polygon')
          .attr('points', '-25,20 25,20 0,-35')
          .attr('fill', '#888');
      } else if (d.type === 'TargetSystem') {
        shape = d3
          .select(this)
          .append('rect')
          .attr('width', 40)
          .attr('height', 40)
          .attr('x', -20)
          .attr('y', -20)
          .attr('fill', '#888');
      } else {
        shape = d3
          .select(this)
          .append('circle')
          .attr('r', 20)
          .attr('fill', '#888');
      }

      if (shape) {
        shape
          .on('mouseover', (event: MouseEvent, d: Node) => {
            tooltip
              .style('visibility', 'visible')
              .text(d.label)
              .style('position', 'absolute')
              .style('top', `${event.pageY + 10}px`)
              .style('left', `${event.pageX + 10}px`);
          })
          .on('mousemove', (event: MouseEvent) => {
            tooltip
              .style('top', `${event.pageY + 10}px`)
              .style('left', `${event.pageX + 10}px`);
          })
          .on('mouseout', () => {
            tooltip.style('visibility', 'hidden');
          });
      }

      d3.select(this)
        .append('text')
        .attr('dy', 35)
        .attr('text-anchor', 'middle')
        .text(d.label ?? d.id)
        .style('font-size', '12px')
        .style('fill', '#333');
    });

    // Node click → navigate to replay if status=error
    nodeGroup.on('click', (event, d) => {
      if (d.status === 'error') {
        this.router.navigate(['/replay'], { queryParams: { nodeId: d.id } });
      }
    });

    this.applyStatusStyles(nodeGroup);

    // Setup simulation
    const simulation = this.createSimulation(
      nodes,
      links,
      width,
      height,
      link,
      nodeGroup
    );

    this.addDragBehavior(nodeGroup, simulation, link);

    // Node click → emit selection event
    nodeGroup.on('click', (event, d) => {
      event.stopPropagation();
      if (d.type === 'PollingNode' || d.type === 'SyncNode') {
        this.nodeSelected.emit(d.id);
      }
    });

    // Background click → deselect
    d3.select('svg').on('click', (event: MouseEvent) => {
      if (event.target === event.currentTarget) {
        this.nodeSelected.emit(null);
      }
    });
  }

  /**
   * Applies visual styles for node status:
   * - Active → green circle
   * - Error → red circle
   * - Inactive → yellow circle
   *
   * @param nodeGroup D3 selection of node groups.
   */
  private applyStatusStyles(
    nodeGroup: d3.Selection<SVGGElement, Node, any, any>
  ) {
    nodeGroup.selectAll('.status-circle').remove();

    nodeGroup
      .filter((d) => d.status === 'active')
      .append('circle')
      .attr('class', 'status-circle')
      .attr('r', 8)
      .attr('fill', '#4caf50');

    nodeGroup
      .filter((d) => d.status === 'error')
      .append('circle')
      .attr('class', 'status-circle')
      .attr('r', 8)
      .attr('fill', '#f44336');

    nodeGroup
      .filter((d) => d.status === 'inactive')
      .append('circle')
      .attr('class', 'status-circle')
      .attr('r', 8)
      .attr('fill', '#ffeb3b');
  }

  /**
   * Adds drag-and-drop behavior for nodes.
   * - While dragging: updates node position and link coordinates.
   * - On release: frees the node back to the simulation.
   */
  private addDragBehavior(
    nodeGroup: d3.Selection<SVGGElement, Node, any, any>,
    simulation: d3.Simulation<Node, NodeConnection>,
    link: d3.Selection<SVGLineElement, NodeConnection, any, any>
  ) {
    nodeGroup.call(
      d3
        .drag<SVGGElement, Node>()
        .on('start', () => simulation.stop())
        .on('drag', (event, d) => {
          d.x = event.x;
          d.y = event.y;
          d.fx = event.x;
          d.fy = event.y;

          link
            .attr('x1', (l) => (l.source as Node).x ?? 0)
            .attr('y1', (l) => (l.source as Node).y ?? 0)
            .attr('x2', (l) => (l.target as Node).x ?? 0)
            .attr('y2', (l) => (l.target as Node).y ?? 0);

          nodeGroup.attr(
            'transform',
            (n) => `translate(${n.x ?? 0},${n.y ?? 0})`
          );
        })
        .on('end', (event, d) => {
          d.fx = undefined;
          d.fy = undefined;
        })
    );
  }

  /**
   * Creates and configures the D3 force simulation
   * used to position nodes and links dynamically.
   */
  private createSimulation(
    nodes: Node[],
    links: NodeConnection[],
    width: number,
    height: number,
    link: d3.Selection<SVGLineElement, NodeConnection, any, any>,
    nodeGroup: d3.Selection<SVGGElement, Node, any, any>
  ) {
    return d3
      .forceSimulation<Node>(nodes)
      .force(
        'link',
        d3
          .forceLink<Node, NodeConnection>(links)
          .id((d) => d.id)
          .distance(100)
      )
      .force('charge', d3.forceManyBody().strength(-200))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .on('tick', () => {
        link
          .attr('x1', (d) => (d.source as Node).x ?? 0)
          .attr('y1', (d) => (d.source as Node).y ?? 0)
          .attr('x2', (d) => (d.target as Node).x ?? 0)
          .attr('y2', (d) => (d.target as Node).y ?? 0);
        nodeGroup.attr(
          'transform',
          (d) => `translate(${d.x ?? 0},${d.y ?? 0})`
        );
      });
  }
}
