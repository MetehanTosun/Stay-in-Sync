import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Vflow } from 'ngx-vflow';
import { GraphAPIService } from '../../service';
import { CustomVFlowNode, LogicOperatorMeta, NodeType, VFlowGraphDTO } from '../../models';
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
  nodes: CustomVFlowNode[] = [];
  edges: Edge[] = [];
  ruleId: number | undefined = undefined;
  lastNodeId = 0;

  @Output() canvasClick = new EventEmitter<{ x: number, y: number }>();

  constructor(private route: ActivatedRoute, private graphApi: GraphAPIService) { }

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    this.ruleId = routeId ? Number(routeId) : undefined;

    if (this.ruleId) {
      this.loadGraph(this.ruleId);
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

  //#region Frontend Logic
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
        name: `Logic Node ${operator.operatorName}`,
        operatorType: operator.operatorName,  // Map operatorName to operatorType for the Backend
      }
    }

    // Create and add new node
    const newNode: CustomVFlowNode = {
      id: (++this.lastNodeId).toString(),
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

  //#region REST Methods
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
          type: this.getNodeType(node.type)
        }));

        // caches the largest node ID
        this.lastNodeId = graph.nodes.length > 0
          ? Math.max(...graph.nodes.map(n => parseInt(n.id)))
          : 0;

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
   * Saves the current graph
   */
  saveGraph() {
    const graphDTO: VFlowGraphDTO = {
      nodes: this.nodes.map(node => ({
        ...node,
        type: node.data.nodeType
      })),
      edges: this.edges
    }

    console.log('Final graphDTO being sent:', JSON.stringify(graphDTO, null, 2)); // TODO-s DELETE

    this.graphApi.updateGraph(this.ruleId!, graphDTO).subscribe({
      next: (res) => {
        console.log('Graph saved successfully'); // TODO-s
      },
      error: (err) => {
        console.error('Error response body:', err.error); // TODO-s DELETE
        alert(err.message);
      }
    });
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
  //#endregion

}
