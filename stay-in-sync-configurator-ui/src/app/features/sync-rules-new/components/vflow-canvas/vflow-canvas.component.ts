import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Connection, Edge, EdgeType, Vflow } from 'ngx-vflow';
import { GraphAPIService } from '../../service';
import { CustomVFlowNode, LogicOperatorMeta, NodeType, VFlowEdgeDTO, VFlowGraphDTO } from '../../models';
import { ConstantNodeComponent, FinalNodeComponent, LogicNodeComponent, ProviderNodeComponent } from '..';
import { CommonModule } from '@angular/common';

/**
 * The canvas of the rule editor on which the rule graph is visualized
 */
@Component({
  selector: 'app-vflow-canvas',
  imports: [
    Vflow,
    CommonModule
  ],
  templateUrl: './vflow-canvas.component.html',
  styleUrl: './vflow-canvas.component.css'
})
export class VflowCanvasComponent implements OnInit {
  //#region Setup
  nodes: CustomVFlowNode[] = [];
  edges: Edge[] = [];
  ruleId: number | undefined = undefined;
  lastNodeId = 0;

  // Edge Validation Attributes
  showTypeIncompatibilityModal = false;
  typeIncompatibilityMessage = '';
  lastAttemptedConnection: { sourceType: string, targetType: string } | null = null;

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

  closeTypeIncompatibilityModal() {
    this.showTypeIncompatibilityModal = false;
    this.typeIncompatibilityMessage = '';
    this.lastAttemptedConnection = null;
  }
  //#endregion

  //#region Element Creation
  /**
   * Creates and adds a new node to the vflow canvas
   * * This does not persist the node in the database
   *
   * @param nodeType The node type of to be created node
   * @param pos The position of the new node
   * @param providerJsonPath Optional: The JSON path of the new (provider) node
   * @param constantValue Optional: The value of the new (constant) node
   * @param operatorData Optional: The operator of the new (logic) node
   */
  addNode(
    nodeType: NodeType,
    pos: { x: number, y: number },
    providerJsonPath?: string,
    constantValue?: any,
    operatorData?: LogicOperatorMeta,
  ) {
    let nodeData: any;

    if (nodeType === NodeType.PROVIDER && providerJsonPath) {
      nodeData = {
        name: providerJsonPath, // TODO-s real json paths are gonna be real long
        arcId: 0, // TODO-s get arcId from JSON path
        jsonPath: providerJsonPath
      }
    } else if (nodeType === NodeType.CONSTANT && constantValue) {
      nodeData = {
        name: `Constant: ${constantValue}`,
        value: constantValue
      }
    } else if (nodeType === NodeType.LOGIC && operatorData) {
      nodeData = {
        ...operatorData,
        name: operatorData.operatorName,
        operatorType: operatorData.operatorName,  // Map operatorName to operatorType for the Backend
      }
    } else {
      alert("Unable to create node") // TODO-s err
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

  /**
   * Creates and adds a new edge to the vflow canvas
   * * This does not persist the edge in the database
   *
   * @param source The source node's ID
   * @param target The target node's ID
   * @param targetHandle The target node's handler
   * @returns
   */
  addEdge({ source, target, targetHandle }: Connection) {
    if (!this.validateEdge({ source, target, targetHandle })) {
      return;
    }

    const newEdge: Edge = {
      id: `${source} -> ${target}`,
      source,
      target,
      targetHandle: targetHandle,
    };
    this.edges = [...this.edges, newEdge];
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
        this.edges = graph.edges.map(edge => {
          const targetNode = this.nodes.find(n => n.id === String(edge.target)); // TODO-s: backend problem; DELETE
          const inputHandles = targetNode?.data?.inputTypes ?? []; // TODO-s: backend problem; DELETE
          const hasSingleInput = inputHandles.length === 1 || inputHandles.length === 0; // TODO-s: backend problem; DELETE
          return {
            ...edge,
            type: edge.type as EdgeType,
            targetHandle: hasSingleInput ? undefined : edge.targetHandle // TODO-s: backend problem; DELETE
          }
        }); // TODO-s: backend problem: Single input nodes should not assign a target handle to the connected node, use a static amount of input handlers

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
      edges: this.edges.map(edge => ({
        ...edge,
        type: edge.type as string
      }))
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
  /**
   * TODO-s
   * @param param0
   * @returns
   */
  private validateEdge({ source, target, targetHandle }: Connection): boolean {
    const existingEdge = this.edges.find(e => e.source === source && e.target === target);

    if (existingEdge) {
      if (existingEdge.targetHandle != targetHandle) {
        this.showTypeIncompatibilityModal = true;
        this.typeIncompatibilityMessage = 'An edge between these nodes with a different handle already exists.';
        this.lastAttemptedConnection = null;
      }
      return false;
    }

    const sourceNode = this.nodes.find(n => n.id === source);
    const targetNode = this.nodes.find(n => n.id === target);

    let sourceType = sourceNode?.data.outputType ?? 'ANY';
    if (sourceNode?.data.nodeType! === NodeType.CONSTANT)
      sourceType = this.inferTypeFromValue(sourceNode?.data.value);

    const targetType = this.getExpectedInputType(targetNode!, targetHandle);

    if (sourceType === 'ANY' || targetType === 'ANY') return true;
    if (sourceType === targetType) return true;

    this.showTypeIncompatibilityModal = true;
    this.typeIncompatibilityMessage = `Type mismatch: cannot connect ${sourceType} to ${targetType}`;
    this.lastAttemptedConnection = { sourceType, targetType };
    return false;
  }

  /**
   * Infers the data type from a JavaScript value
   *
   * @param value an not undefined, non-blank value
   */
  private inferTypeFromValue(value: any): string {
    if (value === null) return 'ANY'

    const jsType = typeof value;
    switch (jsType) {
      case 'number':
        return 'NUMBER';
      case 'string':
        return 'STRING';
      case 'boolean':
        return 'BOOLEAN';
      case 'object':
        if (Array.isArray(value)) return 'ARRAY';
        return 'JSON';
      default:
        return 'ANY';
    }
  }

  /**
   * Returns the expected input type for a target Node
   */
  private getExpectedInputType(target: CustomVFlowNode, targetHandle?: string): string {
    const inputTypes = target?.data?.inputTypes;
    if (target.data.nodeType === NodeType.FINAL) return 'BOOLEAN'

    if (targetHandle) {
      // Extract index from handle, e.g., "input-0" => 0
      const match = targetHandle.match(/input-(\d+)/);
      const index = match ? parseInt(match[1], 10) : 0;
      return inputTypes![index];
    }
    return inputTypes![0];
  }

  /**
   * TODO-s
   * @param nodeType
   * @returns
   */
  private getNodeType(nodeType: NodeType) {
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
