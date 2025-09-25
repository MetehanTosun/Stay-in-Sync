import {AfterViewInit, Component, EventEmitter, Input, Output} from '@angular/core';
import * as d3 from 'd3';
import type {Node, NodeConnection} from '../../core/models/node.model';
import {LegendPanelComponent} from './legend-panel/legend-panel.component';
import {MonitoringGraphService} from '../../core/services/monitoring-graph.service';
import {Router} from '@angular/router';
/**
 * GraphPanelComponent
 *
 * This Angular component is responsible for rendering and managing a graph visualization
 * using D3.js. It supports filtering nodes and links based on a search term, updating the graph,
 * and handling user interactions such as node selection and drag-and-drop behavior.
 */
@Component({
  selector: 'app-graph-panel',
  templateUrl: './graph-panel.component.html',
  imports: [
    LegendPanelComponent
  ],
  styleUrl: './graph-panel.component.css'
})
export class GraphPanelComponent implements AfterViewInit {
  /**
   * Event emitted when a node is selected or deselected.
   * Emits the ID of the selected node or `null` if no node is selected.
   */
  @Output() nodeSelected = new EventEmitter<string | null>();

  private _searchTerm: string = '';
  private isInitialized: boolean = false;
  markedNodes: { [nodeId: string]: boolean } = {};



  constructor(private graphService: MonitoringGraphService, private router: Router) {
  }

  /**
   * Input property for the search term.
   * When the search term changes, the graph is filtered and updated accordingly.
   */
  @Input()
  set searchTerm(value: string) {
    this._searchTerm = value;

    // Filter nur anwenden, wenn die Komponente bereits initialisiert ist
    if (this.isInitialized) {
      this.filteredNodes = this.filterNodes(this._searchTerm);
      this.filteredLinks = this.filterLinks();
      this.updateGraph(this.filteredNodes, this.filteredLinks);
    }
  }
  get searchTerm(): string {
    return this._searchTerm;
  }

  loadGraphData() {
    this.graphService.getMonitoringGraphData().subscribe((data) => {
      console.log('Graph data loaded:', data);
      this.nodes = data.nodes;
      if (data.connections === undefined || data.connections === null) {
        this.links = [];
      }else {
        this.links = data.connections;
      }
      // Status der Nodes basierend auf markedNodes aktualisieren
      this.nodes.forEach(node => {
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

      const svg = d3.select('svg')
        .attr('width', '100%')
        .attr('height', '100%');

      const container = svg.append('g');

      svg.call(
        d3.zoom<any, unknown>()
          .scaleExtent([0.5, 5])
          .on('zoom', (event) => {
            container.attr('transform', event.transform);
          })
      );
      this.renderGraph(container, this.nodes, this.links, width, height);
    }, (error) => {
      console.error('Error loading graph data:', error);
    });
  }



  /**
   * Array of all nodes in the graph.
   */
  nodes: Node[] = [];

  /**
   * Array of all links in the graph.
   */
  links: NodeConnection[] = [];

  /**
   * Array of filtered links based on the search term.
   */
  filteredLinks: NodeConnection[] = [...this.links];

  /**
   * Array of filtered nodes based on the search term.
   */
  filteredNodes: Node[] = [...this.nodes];

  /**
   * Lifecycle hook that is called after the view has been initialized.
   * Sets up the SVG container, zoom behavior, and renders the initial graph.
   */
  ngAfterViewInit() {
    this.loadGraphData();

    // SSE abonnieren
    const evtSource = new EventSource('/events/subscribe');
    evtSource.addEventListener('job-update', (e) => {
      const changedJobIds: number[] = JSON.parse(e.data);

      // markiere Nodes, die zu diesen JobIds gehören
      this.nodes.forEach(node => {
        if (node.type === 'SyncNode') {
          node.status = changedJobIds.includes(Number(node.id)) ? 'error' : 'active';
        }
      });

      this.filteredNodes = this.filterNodes(this.searchTerm);
      this.filteredLinks = this.filterLinks();
      this.updateGraph(this.filteredNodes, this.filteredLinks);
    });
  }



  /**
   * Filters the nodes based on the provided search term.
   * TODO: filter by syncjob and not id and type
   *
   * @param term The search term used for filtering nodes.
   * @returns An array of nodes that match the search term.
   */
  filterNodes(term: string): Node[] {
    console.log(`Filtering nodes with term: ${term}`);
    if (term === '') {
      return [...this.nodes];
    }
    return this.nodes.filter(node =>
      node.id.toLowerCase().includes(term.toLowerCase()) ||
      node.type.toLowerCase().includes(term.toLowerCase())
    );
  }

  /**
   * Filters the links based on the filtered nodes.
   *
   * @returns An array of links that connect filtered nodes.
   */
  filterLinks(): NodeConnection[] {
    return this.links.filter(link =>
      this.filteredNodes.includes(link.source as Node) && this.filteredNodes.includes(link.target as Node)
    );
  }

  /**
   * Updates the graph visualization with new nodes and links.
   *
   * - Removes all existing lines and node groups from the container.
   * - Calculates the current SVG width and height.
   * - Calls renderGraph to redraw the graph with the provided nodes and links.
   *
   * @param newNodes Array of nodes to display in the graph.
   * @param newLinks Array of links to display in the graph.
   */
  updateGraph(newNodes: Node[], newLinks: NodeConnection[]) {
    const svg = d3.select('svg');
    const container = svg.select<SVGGElement>('g');

    container.selectAll('line').remove();
    container.selectAll('g').remove();

    const svgNode = svg.node();
    const width = (svgNode instanceof SVGSVGElement) ? window.innerWidth : 400;
    const height = (svgNode instanceof SVGSVGElement) ? svgNode.clientHeight : 300;

    this.renderGraph(container, newNodes, newLinks, width, height);
  }

  /**
   * Renders the graph with nodes and links.
   *
   * - Creates SVG groups for links and nodes.
   * - Draws nodes as circles and applies status styles.
   * - Initializes the D3 force simulation for layout.
   * - Enables drag-and-drop for nodes.
   * - Handles click events on nodes and the SVG container element.
   *
   * @param container D3 selection of the container group element (<g>).
   * @param nodes Array of node objects.
   * @param links Array of link objects.
   * @param width Width of the drawing area.
   * @param height Height of the drawing area.
   */
  private renderGraph(
    container: d3.Selection<SVGGElement, unknown, HTMLElement, any>,
    nodes: Node[],
    links: NodeConnection[],
    width: number,
    height: number
  ) {
    const tooltip = d3.select('#tooltip');
    console.log(`Rendering graph with ${nodes.length} nodes and ${links.length} links`);

    const link = container.append('g')
      .selectAll('line')
      .data(links)
      .enter().append('line')
      .attr('stroke', '#999');

    const nodeGroup = container.append('g')
      .selectAll('g')
      .data(nodes)
      .enter().append('g');

    nodeGroup.each(function (d: Node) {
      let shape: any;
      if (d.type === 'SourceSystem' || d.type === 'ASS') {
        shape = d3.select(this)
          .append('polygon')
          .attr('points', '-25,20 25,20 0,-35') // Größeres Dreieck
          .attr('fill', '#888');
      } else if (d.type === 'TargetSystem') {
        shape = d3.select(this)
          .append('rect')
          .attr('width', 40)
          .attr('height', 40)
          .attr('x', -20)
          .attr('y', -20)
          .attr('fill', '#888');
      } else {
        shape = d3.select(this)
          .append('circle')
          .attr('r', 20)
          .attr('fill', '#888');
      }
      if (shape) {
        shape
          .on('mouseover', (event: MouseEvent, d: Node) => {
            tooltip.style('visibility', 'visible')
              .text(d.label)
              .style('position', 'absolute')
              .style('top', `${event.pageY + 10}px`)
              .style('left', `${event.pageX + 10}px`);
          })
          .on('mousemove', (event: MouseEvent) => {
            tooltip.style('top', `${event.pageY + 10}px`)
              .style('left', `${event.pageX + 10}px`);
          })
          .on('mouseout', () => {
            tooltip.style('visibility', 'hidden');
          });
      }
      d3.select(this)
        .append('text')
        .attr('dy', 35)          // Abstand nach unten
        .attr('text-anchor', 'middle')
        .text(d.label ?? d.id)   // Label oder ID anzeigen
        .style('font-size', '12px')
        .style('fill', '#333');
    });

    // Klick-Handler für Nodes mit Status "error"
    nodeGroup.on('click', (event, d) => {
      if (d.status === 'error') {
        this.router.navigate(['/replay'], { queryParams: { nodeId: d.id } });
      }
    });

    this.applyStatusStyles(nodeGroup);

    const simulation = this.createSimulation(nodes, links, width, height, link, nodeGroup);

    this.addDragBehavior(nodeGroup, simulation, link);

    nodeGroup.on('click', (event, d) => {
      event.stopPropagation();
      if (d.type === 'PollingNode' || d.type === 'SyncNode') {
        this.nodeSelected.emit(d.id);
      }
    });

    d3.select('svg').on('click', (event: MouseEvent) => {
      // Prüfe, ob direkt auf das SVG-Element (und nicht auf ein Kind) geklickt wurde
      if (event.target === event.currentTarget) {
        this.nodeSelected.emit(null);
      }
    });
  }

  /**
   * Applies status-specific styles to nodes.
   *
   * - Active nodes receive a green circle.
   * - Nodes with error status receive a red circle.
   * - Inactive nodes receive a yellow circle.
   *
   * @param nodeGroup D3 selection of node groups (<g> elements).
   */
  private applyStatusStyles(nodeGroup: d3.Selection<SVGGElement, Node, any, any>) {
    nodeGroup.selectAll('.status-circle').remove();

    nodeGroup.filter(d => d.status === 'active')
      .append('circle')
      .attr('class', 'status-circle')
      .attr('r', 8)
      .attr('fill', '#4caf50');

    nodeGroup.filter(d => d.status === 'error')
      .append('circle')
      .attr('class', 'status-circle')
      .attr('r', 8)
      .attr('fill', '#f44336');

    nodeGroup.filter(d => d.status === 'inactive')
      .append('circle')
      .attr('class', 'status-circle')
      .attr('r', 8)
      .attr('fill', '#ffeb3b');
  }


  /**
   * Adds drag-and-drop behavior to the graph nodes.
   *
   * @param nodeGroup D3 selection of node groups (SVG <g> elements).
   * @param simulation D3 force simulation used for node positioning.
   * @param link D3 selection of link elements (SVG <line> elements).
   *
   * While dragging, the node coordinates are updated and the positions of links and nodes are reset.
   * After releasing, the fixed coordinates are removed so the simulation can continue.
   */
  private addDragBehavior(nodeGroup: d3.Selection<SVGGElement, Node, any, any>, simulation: d3.Simulation<Node, NodeConnection>, link: d3.Selection<SVGLineElement, NodeConnection, any, any>) {
    nodeGroup.call(
      d3.drag<SVGGElement, Node>()
        .on('start', () => {
          simulation.stop();
        })
        .on('drag', (event, d) => {
          d.x = event.x;
          d.y = event.y;
          d.fx = event.x;
          d.fy = event.y;

          link
            .attr('x1', l => (l.source as Node).x ?? 0)
            .attr('y1', l => (l.source as Node).y ?? 0)
            .attr('x2', l => (l.target as Node).x ?? 0)
            .attr('y2', l => (l.target as Node).y ?? 0);

          nodeGroup
            .attr('transform', n => `translate(${n.x ?? 0},${n.y ?? 0})`);
        })
        .on('end', (event, d) => {
          d.fx = undefined;
          d.fy = undefined;
        })
    );
  }

  /**
   * Creates a D3 force simulation for the graph layout.
   *
   * @param nodes Array of node objects.
   * @param links Array of link objects.
   * @param width Width of the drawing area.
   * @param height Height of the drawing area.
   * @param link D3 selection of link elements (SVG <line> elements).
   * @param nodeGroup D3 selection of node groups (SVG <g> elements).
   * @returns A D3 force simulation instance.
   */
  private createSimulation(nodes: Node[], links: NodeConnection[], width: number, height: number, link: d3.Selection<SVGLineElement, NodeConnection, any, any>, nodeGroup: d3.Selection<SVGGElement, Node, any, any>) {
    return d3.forceSimulation<Node>(nodes)
      .force('link', d3.forceLink<Node, NodeConnection>(links).id(d => d.id).distance(100))
      .force('charge', d3.forceManyBody().strength(-200))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .on('tick', () => {
        link
          .attr('x1', d => (d.source as Node).x ?? 0)
          .attr('y1', d => (d.source as Node).y ?? 0)
          .attr('x2', d => (d.target as Node).x ?? 0)
          .attr('y2', d => (d.target as Node).y ?? 0);
        nodeGroup
          .attr('transform', d => `translate(${d.x ?? 0},${d.y ?? 0})`);
      });
  }
}
