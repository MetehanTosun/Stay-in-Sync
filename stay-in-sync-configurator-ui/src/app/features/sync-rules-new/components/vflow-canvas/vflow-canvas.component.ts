import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Node, Vflow } from 'ngx-vflow';
import { GraphAPIService } from '../../service';
import { NodeType, VFlowGraphDTO } from '../../models';
import { ConstantNodeComponent, FinalNodeComponent, LogicNodeComponent, ProviderNodeComponent } from '..';

@Component({
  selector: 'app-vflow-canvas',
  imports: [Vflow],
  templateUrl: './vflow-canvas.component.html',
  styleUrl: './vflow-canvas.component.css'
})
export class VflowCanvasComponent implements OnInit {
  nodes: Node[] = [];
  edges: Edge[] = [];

  @Output() canvasClick = new EventEmitter<{ x: number, y: number }>();

  constructor(private route: ActivatedRoute, private graphApi: GraphAPIService) { }

  ngOnInit() {
    const routeId = this.route.snapshot.paramMap.get('id');
    const ruleId = routeId ? Number(routeId) : undefined;

    if (ruleId) {
      this.loadGraph(ruleId);
    } else {
      alert("Unable to load graph - cannot read rule id") // TODO-s err
    }
  }

  /**
   * This loads the transformation rules graph from the backend and assigns the corresponding type
   * @param ruleId
   */
  loadGraph(ruleId: number) {
    this.graphApi.getGraph(ruleId).subscribe({
      next: (graph: VFlowGraphDTO) => {

        // loads nodes
        this.nodes = graph.nodes.map(node => ({
          ...node,
          id: node.id.toString(),
          type: this.getNodeType(node.type)
        }));
        console.log(this.nodes) // TODO-s DELETE

        // loads edges
        this.edges = graph.edges.map(edge => ({
          ...edge,
          type: edge.type as any
        }));

        // TODO-s loads errors
      },
      error: (err) => {
        alert(err.error?.message || err.message);  // TODO-s err
      }
    })
  }

  /**
   * Calculates and emits the mouse position on the vflow canvas
   * @param mouseEvent
   */
  onCanvasClick(mouseEvent: MouseEvent) {
    // Gets the canvas element's position and size relative to the viewport
    const rect = (mouseEvent.currentTarget as HTMLElement).getBoundingClientRect();
    const x = mouseEvent.clientX - rect.left;
    const y = mouseEvent.clientY - rect.top;

    this.canvasClick.emit({ x, y });
    console.log("Mouse click registered at: ", this.canvasClick); // TODO-s DELETE
  }

  addNode(nodeType: NodeType, pos: { x: number, y: number }) {
    const newNode: Node = {
      id: this.nodes.length.toString(),
      point: pos,
      type: this.getNodeType(nodeType),
      width: 200,
      height: 100,
      data: {
        name: nodeType,
        nodeType: nodeType
      }
    };
    this.nodes = [...this.nodes, newNode];
  }

  // #region Helpers
  getNodeType(nodeType: NodeType) {
    switch (nodeType) {
      case NodeType.PROVIDER: return ProviderNodeComponent;
      case NodeType.CONSTANT: return ConstantNodeComponent;
      case NodeType.LOGIC: return LogicNodeComponent;
      case NodeType.FINAL: return FinalNodeComponent;
    }
  }
  // #region

}
