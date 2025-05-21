import {Component, AfterViewInit, Output, EventEmitter} from '@angular/core';
import * as d3 from 'd3';
import {Node, NodeConnection} from '../../../node.model';

@Component({
  selector: 'app-graph-panel',
  templateUrl: './graph-panel.component.html',
  styleUrl: './graph-panel.component.css'
})
export class GraphPanelComponent implements AfterViewInit {
  @Output() nodeSelected = new EventEmitter<string | null>();
  ngAfterViewInit() {

    const nodes: Node[] = [
      { id: 'A', type: 'API', status: 'active', connections: [] },
      { id: 'B', type: 'ASS', status: 'active', connections: [] },
      { id: 'C', type: 'Syncnode', status: 'inactive', connections: [] }
    ];

    const links: NodeConnection[] = [
      { source: nodes[0], target: nodes[1], status: "active" },
      { source: nodes[1], target: nodes[2], status: "inactive" },
    ];

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

    const link = container.append('g')
      .selectAll('line')
      .data(links)
      .enter().append('line')
      .attr('stroke', '#999');

    // Gruppiere für jeden Knoten ein <g>
    const nodeGroup = container.append('g')
      .selectAll('g')
      .data(nodes)
      .enter().append('g');

    // Großer grauer Kreis
    nodeGroup.append('circle')
      .attr('r', 20)
      .attr('fill', '#888');

    // Kleiner grüner Kreis für aktive Knoten
    nodeGroup.filter(d => d.status === 'active')
      .append('circle')
      .attr('r', 8)
      .attr('fill', '#4caf50');

    // Kleiner roter Kreis für error Knoten
    nodeGroup.filter(d => d.status === 'error')
      .append('circle')
      .attr('r', 8)
      .attr('fill', '#f44336');

    // Kleiner gelber Kreis für inactive Knoten
    nodeGroup.filter(d => d.status === 'inactive')
      .append('circle')
      .attr('r', 8)
      .attr('fill', '#ffeb3b');

    const simulation = d3.forceSimulation<Node>(nodes)
      .force('link', d3.forceLink<Node, NodeConnection>(links).id(d => (d as Node).id).distance(100))
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

    nodeGroup.on('click', (event, d) => {
      this.nodeSelected.emit(d.id);
      console.log(`Node ${d.id} selected`);
    });

    // Drag-Verhalten für Knoten, einzelne Knoten können verschoben werden
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
}
