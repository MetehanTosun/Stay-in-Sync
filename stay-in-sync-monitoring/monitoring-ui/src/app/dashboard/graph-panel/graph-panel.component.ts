import {AfterViewInit, Component, EventEmitter, Input, Output} from '@angular/core';
import * as d3 from 'd3';
import type {Node, NodeConnection} from '../../../node.model';
import {LegendPanelComponent} from './legend-panel/legend-panel.component';

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

  /**
   * Input property for the search term.
   * When the search term changes, the graph is filtered and updated accordingly.
   */
  @Input()
  set searchTerm(value: string) {
    this._searchTerm = value;
    this.filteredNodes = this.filterNodes(this._searchTerm);
    this.filteredLinks = this.filterLinks(this._searchTerm);
    this.updateGraph(this.filteredNodes, this.filteredLinks);
  }
  get searchTerm(): string {
    return this._searchTerm;
  }

  /**
   * Array of all nodes in the graph.
   * TODO: Replace with actual data fetching logic.
   */
  nodes: Node[] = [
    { id: 'A', type: 'API', status: 'active', connections: [] },
    { id: 'B', type: 'ASS', status: 'active', connections: [] },
    { id: 'C', type: 'Syncnode', status: 'inactive', connections: [] }
  ];

  /**
   * Array of all links in the graph.
   * TODO: Replace with actual data fetching logic.
   */
  links: NodeConnection[] = [
    { source: this.nodes[0], target: this.nodes[1], status: "active" },
    { source: this.nodes[1], target: this.nodes[2], status: "inactive" },
  ];

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
   * @param term The search term used for filtering links.
   * @returns An array of links that connect filtered nodes.
   */
  filterLinks(term: string): NodeConnection[] {
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
    const width = (svgNode instanceof SVGSVGElement) ? svgNode.clientWidth : 400;
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
    const link = container.append('g')
      .selectAll('line')
      .data(links)
      .enter().append('line')
      .attr('stroke', '#999');

    const nodeGroup = container.append('g')
      .selectAll('g')
      .data(nodes)
      .enter().append('g');

    nodeGroup.append('circle')
      .attr('r', 20)
      .attr('fill', '#888');

    this.applyStatusStyles(nodeGroup);

    const simulation = this.createSimulation(nodes, links, width, height, link, nodeGroup);

    this.addDragBehavior(nodeGroup, simulation, link);

    nodeGroup.on('click', (event, d) => {
      this.nodeSelected.emit(d.id);
      console.log(`Node ${d.id} selected`);
    });

    container.on('click', (event: MouseEvent) => {
      if ((event.target as SVGElement).tagName === 'svg') {
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
    nodeGroup.filter(d => d.status === 'active')
      .append('circle')
      .attr('r', 8)
      .attr('fill', '#4caf50');

    nodeGroup.filter(d => d.status === 'error')
      .append('circle')
      .attr('r', 8)
      .attr('fill', '#f44336');

    nodeGroup.filter(d => d.status === 'inactive')
      .append('circle')
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
      .force('center', d3.forceCenter(width / 2, height / 2))  //TODO: Fix center position when filtering
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
