import { Component, AfterViewInit } from '@angular/core';
import * as d3 from 'd3';
import { Node } from '../../../node.model';

@Component({
  selector: 'app-graph-panel',
  templateUrl: './graph-panel.component.html',
  styleUrl: './graph-panel.component.css'
})

export class GraphPanelComponent implements AfterViewInit {
  ngAfterViewInit() {

    interface CustomLink extends d3.SimulationLinkDatum<Node> {
      source: Node | string;
      target: Node | string;
    }

   const nodes: Node[] = [
     { id: 'A', type: 'API', status: 'active', connections: [] },
     { id: 'B', type: 'ASS', status: 'active', connections: [] },
     { id: 'C', type: 'Syncnode', status: 'inactive', connections: [] }
   ];

    const links: CustomLink[] = [
      { source: nodes[0], target: nodes[1] },
      { source: nodes[1], target: nodes[2] }
    ];

    const svg = d3.select('svg')
      .attr('width', 400)
      .attr('height', 300)
      .attr('class', 'node-active');

    // Container-Gruppe für Zoom und Pan
    const container = svg.append('g');



    // d3.zoom hinzufügen
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

    const node = container.append('g')
      .selectAll('circle')
      .data(nodes)
      .enter().append('circle')
      .attr('r', 20)
      .attr('fill', '#69b3a2');

    d3.forceSimulation<Node>(nodes)
      .force('link', d3.forceLink<Node, CustomLink>(links).id(d => (d as Node).id).distance(100))
      .force('charge', d3.forceManyBody().strength(-200))
      .force('center', d3.forceCenter(200, 150))
      .on('tick', () => {
        link
          .attr('x1', d => (d.source as Node).x ?? 0)
          .attr('y1', d => (d.source as Node).y ?? 0)
          .attr('x2', d => (d.target as Node).x ?? 0)
          .attr('y2', d => (d.target as Node).y ?? 0);

        node
          .attr('cx', d => d.x ?? 0)
          .attr('cy', d => d.y ?? 0);
      });
  }
}
