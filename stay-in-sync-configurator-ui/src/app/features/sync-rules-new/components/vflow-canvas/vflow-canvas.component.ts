import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Node, Vflow } from 'ngx-vflow';
import { GraphAPIService } from '../../service';
import { LogicOperator as LogicOperatorMeta, NodeType, VFlowGraphDTO } from '../../models';
import { ConstantNodeComponent, FinalNodeComponent, LogicNodeComponent, ProviderNodeComponent } from '..';

/**
 * The canvas of the rule editor on which the rule graph is visualized
 */
@Component({
  selector: 'app-vflow-canvas',
  imports: [Vflow],
  templateUrl: './vflow-canvas.component.html',
  styleUrl: './vflow-canvas.component.css'
})
export class VflowCanvasComponent implements OnInit {
  //#region Setup
  nodes: Node[] = [];
  edges: Edge[] = [];

  @Output() canvasClick = new EventEmitter<{ x: number, y: number }>();

  constructor(private route: ActivatedRoute, private graphApi: GraphAPIService) { }

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    const ruleId = routeId ? Number(routeId) : undefined;

    if (ruleId) {
      this.loadGraph(ruleId);
    } else {
      alert("Unable to load graph - cannot read rule id") // TODO-s err
    }
  }
  //#endregion

  //#region Template Methods
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
  }
  //#endregion

  //#region Graph Operations
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
   * Creates and adds a new node to the vflow canvas
   * * This does not persist the node in the database
   *
   * @param nodeType The node type of to be created node
   * @param pos The position of the new node
   * @param operator Optional: The operator of the new (logic) node
   */
  addNode(nodeType: NodeType, pos: { x: number, y: number }, operator?: LogicOperatorMeta) {
    // Standard node data with node name
    let nodeData: any = {
      name: `${nodeType} Node`
    };

    // Add operator meta data if applicable
    if (nodeType === NodeType.LOGIC && operator) {
      nodeData = {
        ...operator,
        name: `Logic Node ${operator.operatorName}`
      }
    }

    // Create and add new node
    const newNode: Node = {
      id: `${Date.now()}-${crypto.randomUUID().split('-')[0]}`,
      point: pos,
      type: this.getNodeType(nodeType),
      width: 200,
      height: 100,
      data: {
        ...nodeData,
        nodeType: nodeType
      }
    };
    this.nodes = [...this.nodes, newNode];
  }
  //#endregion

  //#region Helpers
  getNodeType(nodeType: NodeType) {
    switch (nodeType) {
      case NodeType.PROVIDER: return ProviderNodeComponent;
      case NodeType.CONSTANT: return ConstantNodeComponent;
      case NodeType.LOGIC: return LogicNodeComponent;
      case NodeType.FINAL: return FinalNodeComponent;
      default:
        alert("Unknown NodeType");
        throw Error("Unknown NodeType"); // TODO-s err
    }
  }
  //#region

}
