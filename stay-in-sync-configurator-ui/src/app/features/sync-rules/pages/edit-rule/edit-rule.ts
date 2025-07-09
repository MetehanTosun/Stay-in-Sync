import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { delay, of, Observable } from 'rxjs';
import { Vflow, Node, Edge, Connection } from 'ngx-vflow';
import { ProviderNodeComponent, LogicNodeComponent, ConstantNodeComponent } from '../../components/nodes';

interface CustomNode {
  id: number;
  offsetX: number;
  offsetY: number;
  nodeType: string;
  name: string;
  arcId?: number;
  jsonPath?: string;
  value?: any;
  operatorType?: string;
  inputNodes?: { id: number; orderIndex: number }[];
}

interface GraphData {
  nodes: CustomNode[];
  edges: Edge[];
}

@Component({
  selector: 'app-edit-rule',
  imports: [Vflow, FormsModule],
  templateUrl: './edit-rule.html',
  styleUrl: './edit-rule.scss'
})
export class EditRule implements OnInit {

  nodes: Node[] = [];
  edges: Edge[] = [];
  isSaving = false;
  saveMessage = '';
  currentRuleId: number | null = null;
  ruleName = 'New Rule';

  // Selection and context menu properties
  selectedNode: Node | null = null;
  showContextMenu = false;
  contextMenuPosition = { x: 0, y: 0 };

  // Edge context menu properties
  selectedEdge: Edge | null = null;
  showEdgeContextMenu = false;
  edgeContextMenuPosition = { x: 0, y: 0 };

  // Edit mode properties
  isEditingJsonPath = false;
  editingJsonPath = '';
  isEditingOperator = false;
  editingOperator = '';
  isEditingConstantValue = false;
  editingConstantValue: any = '';
  isEditingNodeName = false;
  editingNodeName = '';

  lastMousePosition: { x: number, y: number } | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {
    // Track mouse position globally
    window.addEventListener('mousemove', (e: MouseEvent) => {
      this.lastMousePosition = { x: e.clientX, y: e.clientY };
    });
  }

  ngOnInit() {
    // Get rule ID from route parameters or fallback to query parameters
    this.route.paramMap.subscribe(params => {
      const idFromRoute = params.get('id');
      if (idFromRoute) {
        this.currentRuleId = parseInt(idFromRoute);
        this.loadRuleData();
      } else {
        this.route.queryParams.subscribe(queryParams => {
          this.currentRuleId = queryParams['id'] ? parseInt(queryParams['id']) : null;
          this.loadRuleData();
        });
      }
    });
  }

  // sTODO REST
  private loadRuleData() {
    // Load mock rule graph for rule ID 1 "Temp Check"
    if (this.currentRuleId === 1) {
      this.ruleName = 'Temp Check';
      this.mockApiGetGraphDataFromDatabase().subscribe({
        next: (data) => {
          console.log('Graph data loaded:', data);
          this.nodes = this.convertToVflowNodes(data.nodes);
          this.edges = data.edges;
        },
        error: (error) => {
          console.error('Error loading graph data:', error);
        }
      });
    } else {
      // Empty graph for other Rules
      this.ruleName = this.currentRuleId ? `Rule ${this.currentRuleId}` : 'New Rule';
      this.nodes = [];
      this.edges = [];
    }
  }

  goBackToOverview() {
    this.router.navigate(['/sync-rules']);
  }

  private clearMessageAfter(seconds: number) {
    setTimeout(() => {
      this.saveMessage = '';
    }, seconds * 1000);
  }

  //#region Drag and Drop Region
  onDragStart(event: DragEvent, nodeType: string) {
    if (event.dataTransfer) {
      event.dataTransfer.setData('text/plain', nodeType);
      event.dataTransfer.effectAllowed = 'copy';
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'copy';
    }

    // show visual feedback
    const target = event.currentTarget as HTMLElement;
    target.classList.add('drag-over');
  }

  onDragLeave(event: DragEvent) {
    const target = event.currentTarget as HTMLElement;
    target.classList.remove('drag-over');
  }

  onDrop(event: DragEvent) {
    event.preventDefault();

    // Remove visual feedback
    const target = event.currentTarget as HTMLElement;
    target.classList.remove('drag-over');

    const nodeType = event.dataTransfer?.getData('text/plain');
    if (!nodeType) return;

    const rect = target.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    this.createNewNode(nodeType, x, y);
  }
  //#endregion

  //#region Create Region
  private createNewNode(nodeType: string, x: number, y: number) {
    const newId = this.getNextNodeId();
    let nodeComponent: any;
    let defaultName: string;
    let nodeData: any = {
      nodeType: nodeType,
      inputNodes: [],
      selected: false
    };

    // sTODO replace with actual nodes
    switch (nodeType) {
      case 'PROVIDER':
        nodeComponent = ProviderNodeComponent;
        defaultName = `Provider ${newId}`;
        nodeData = {
          ...nodeData,
          name: defaultName,
          arcId: 1,
          jsonPath: 'source.example.path'
        };
        break;
      case 'LOGIC':
        nodeComponent = LogicNodeComponent;
        defaultName = `Logic ${newId}`;
        nodeData = {
          ...nodeData,
          name: defaultName,
          operatorType: 'AND'
        };
        break;
      case 'CONSTANT':
        nodeComponent = ConstantNodeComponent;
        defaultName = `Constant ${newId}`;
        nodeData = {
          ...nodeData,
          name: defaultName,
          value: 0
        };
        break;
      default:
        nodeComponent = ProviderNodeComponent;
        defaultName = `Node ${newId}`;
        nodeData = {
          ...nodeData,
          name: defaultName
        };
    }

    const newNode: Node = {
      id: newId.toString(),
      point: { x: x - 110, y: y - 50 }, // Offset to center
      type: nodeComponent,
      width: 220,
      height: 100,
      data: nodeData
    };

    this.nodes = [...this.nodes, newNode];
  }

  private getNextNodeId(): number {
    if (this.nodes.length === 0) return 1;
    const maxId = Math.max(...this.nodes.map((node: any) => parseInt(node.id)));
    return maxId + 1;
  }

  public createEdge({ source, target, targetHandle }: Connection) {
    const newEdge: Edge = {
      id: `${source} -> ${target}`,
      source,
      target,
      targetHandle,
      type: 'default'
    };
    this.edges = [...this.edges, newEdge];
    console.log(this.edges);
  }
  //#endregion

  //#region Selection Region
  onCanvasClick() {
    this.selectedNode = null;
    this.hideContextMenu();
    this.selectedEdge = null;
    this.hideEdgeContextMenu();
    this.updateNodeSelection();
  }

  onNodeSelected(event: any) {
    const nodeData = event.detail;
    this.selectedNode = nodeData.node;
    this.hideContextMenu();
    this.updateNodeSelection();
    console.log('Node selected via custom event:', this.selectedNode);
  }

  onNodeContextMenuFromEvent(event: any) {
    this.hideEdgeContextMenu();
    console.log('Context menu event received:', event);
    const nodeData = event.detail;
    this.selectedNode = nodeData.node;
    this.updateNodeSelection();

    if (this.selectedNode) {
      const rect = nodeData.buttonRect;
      this.contextMenuPosition = {
        x: rect ? rect.right + 5 : (nodeData.event?.clientX || 0),
        y: rect ? rect.top : (nodeData.event?.clientY || 0)
      };
      this.showContextMenu = true;

      console.log('Context menu opened via custom event at:', this.contextMenuPosition);

      setTimeout(() => {
        document.addEventListener('click', this.hideContextMenuHandler, { once: true });
      });
    }
  }

  onNodeClick(event: any) {
    if (event.event) {
      event.event.stopPropagation();
    }
    this.selectedNode = event.node;
    this.hideContextMenu();
    this.updateNodeSelection();
    console.log('Node selected:', this.selectedNode);
  }

  onNodeContextMenu(event: any) {
    this.hideEdgeContextMenu();
    if (event.event) {
      event.event.preventDefault();
      event.event.stopPropagation();
    }

    this.selectedNode = event.node;
    this.updateNodeSelection();

    if (this.selectedNode) {
      this.contextMenuPosition = {
        x: event.event?.clientX || 0,
        y: event.event?.clientY || 0
      };
      this.showContextMenu = true;

      console.log('Context menu opened for node:', this.selectedNode);

      setTimeout(() => {
        document.addEventListener('click', this.hideContextMenuHandler, { once: true });
      });
    }
  }

  private updateNodeSelection() {
    this.nodes.forEach((node: any) => {
      const nodeData = node.data;
      if (nodeData) {
        nodeData.selected = node.id === this.selectedNode?.id;
      }
    });
    this.nodes = [...this.nodes];
  }

  selectNode(nodeId: string, event?: MouseEvent) {
    this.selectedNode = this.nodes.find((node: any) => node.id === nodeId) || null;
    if (event) {
      event.stopPropagation();
    }
    this.hideContextMenu();
    this.updateNodeSelection();
  }

  private hideContextMenuHandler = () => {
    this.hideContextMenu();
  }

  hideContextMenu() {
    this.showContextMenu = false;
  }
  //#endregion

  //#region Context Menu
  getNodeData(node: Node | null): any {
    return (node as any)?.data;
  }

  startEditingJsonPath() {
    const nodeData = (this.selectedNode as any)?.data;
    if (nodeData?.nodeType === 'PROVIDER') {
      this.editingJsonPath = nodeData.jsonPath || '';
      this.isEditingJsonPath = true;
      this.hideContextMenu();
    }
  }

  saveJsonPath() {
    if (this.selectedNode && this.editingJsonPath.trim()) {
      // Update the node data
      const nodeData = (this.selectedNode as any).data;
      const updatedNode = {
        ...this.selectedNode,
        data: {
          ...nodeData,
          jsonPath: this.editingJsonPath.trim()
        }
      };
      this.nodes = this.nodes.map((node: any) =>
        node.id === this.selectedNode!.id ? updatedNode : node
      );
      this.selectedNode = updatedNode;
      this.isEditingJsonPath = false;
      this.editingJsonPath = '';
    }
  }

  cancelEditJsonPath() {
    this.isEditingJsonPath = false;
    this.editingJsonPath = '';
  }

  // Placeholder methods for other node types
  editOperatorType() {
    const nodeData = (this.selectedNode as any)?.data;
    if (nodeData?.nodeType === 'LOGIC') {
      this.editingOperator = nodeData.operatorType || 'AND';
      this.isEditingOperator = true;
      this.hideContextMenu();
    }
  }

  saveOperatorType() {
    if (this.selectedNode && this.editingOperator.trim()) {
      // Update node data
      const nodeData = (this.selectedNode as any).data;
      const updatedNode = {
        ...this.selectedNode,
        data: {
          ...nodeData,
          operatorType: this.editingOperator.trim()
        }
      };
      this.nodes = this.nodes.map((node: any) =>
        node.id === this.selectedNode!.id ? updatedNode : node
      );
      this.selectedNode = updatedNode;
      this.isEditingOperator = false;
      this.editingOperator = '';
    }
  }

  cancelEditOperator() {
    this.isEditingOperator = false;
    this.editingOperator = '';
  }

  editConstantValue() {
    const nodeData = (this.selectedNode as any)?.data;
    if (nodeData?.nodeType === 'CONSTANT') {
      this.editingConstantValue = nodeData.value || 0;
      this.isEditingConstantValue = true;
      this.hideContextMenu();
    }
  }

  saveConstantValue() {
    if (this.selectedNode && this.editingConstantValue !== '') {
      // Update node data
      const nodeData = (this.selectedNode as any).data;
      const updatedNode = {
        ...this.selectedNode,
        data: {
          ...nodeData,
          value: this.editingConstantValue
        }
      };
      this.nodes = this.nodes.map((node: any) =>
        node.id === this.selectedNode!.id ? updatedNode : node
      );
      this.selectedNode = updatedNode;
      this.isEditingConstantValue = false;
      this.editingConstantValue = '';
    }
  }

  cancelEditConstantValue() {
    this.isEditingConstantValue = false;
    this.editingConstantValue = '';
  }

  startEditingNodeName() {
    const nodeData = (this.selectedNode as any)?.data;
    if (nodeData) {
      this.editingNodeName = nodeData.name || '';
      this.isEditingNodeName = true;
      this.hideContextMenu();
    }
  }

  saveNodeName() {
    if (this.selectedNode && this.editingNodeName.trim()) {
      // Update node data
      const nodeData = (this.selectedNode as any).data;
      const updatedNode = {
        ...this.selectedNode,
        data: {
          ...nodeData,
          name: this.editingNodeName.trim()
        }
      };
      this.nodes = this.nodes.map((node: any) =>
        node.id === this.selectedNode!.id ? updatedNode : node
      );
      this.selectedNode = updatedNode;
      this.isEditingNodeName = false;
      this.editingNodeName = '';
    }
  }

  cancelEditNodeName() {
    this.isEditingNodeName = false;
    this.editingNodeName = '';
  }

  deleteSelectedNode() {
    if (this.selectedNode) {
      this.nodes = this.nodes.filter((node: any) => node.id !== this.selectedNode!.id);
      this.edges = this.edges.filter((edge: any) =>
        edge.source !== this.selectedNode!.id && edge.target !== this.selectedNode!.id
      );
      this.selectedNode = null;
      this.hideContextMenu();
    }
  }

  public openEdgeContextMenu(event?: any, edge?: Edge) {
    let selectedEdge: Edge | null = null;
    let menuPosition: { x: number; y: number } = { x: window.innerWidth / 2, y: window.innerHeight / 2 };

    if (Array.isArray(event) && event.length > 0 && event[0].id && event[0].selected) {
      selectedEdge = this.edges.find(e => e.id === event[0].id) || null;
      if (this.lastMousePosition) { menuPosition = { ...this.lastMousePosition }; }
    }

    if (!selectedEdge) {
      this.selectedEdge = null;
      this.showEdgeContextMenu = false;
      this.hideEdgeContextMenu();
      return;
    }

    this.selectedEdge = selectedEdge;
    this.edgeContextMenuPosition = menuPosition;
    this.showEdgeContextMenu = true;
    document.removeEventListener('click', this.hideEdgeContextMenuHandler);
    setTimeout(() => {
      document.addEventListener('click', this.hideEdgeContextMenuHandler, { once: true });
    });
    console.log("Selected an edge", event, edge);
  }

  private hideEdgeContextMenuHandler = () => {
    this.hideEdgeContextMenu();
  }

  hideEdgeContextMenu() {
    this.showEdgeContextMenu = false;
  }

  deleteSelectedEdge() {
    if (this.selectedEdge) {
      this.edges = this.edges.filter(e => e.id !== this.selectedEdge!.id);
      this.selectedEdge = null;
      this.hideEdgeContextMenu();
    }
  }

  private convertToVflowNodes(customNodes: CustomNode[]): Node[] {
    return customNodes.map(node => {
      let nodeType: any;
      switch (node.nodeType) {
        case 'PROVIDER':
          nodeType = ProviderNodeComponent;
          break;
        case 'LOGIC':
          nodeType = LogicNodeComponent;
          break;
        case 'CONSTANT':
          nodeType = ConstantNodeComponent;
          break;
        default:
          nodeType = ProviderNodeComponent;
      }
      const scaledX = node.offsetX * 250;
      const scaledY = node.offsetY * 150;
      return {
        id: node.id.toString(),
        point: { x: scaledX, y: scaledY },
        type: nodeType,
        width: 220,
        height: 100,
        data: {
          name: node.name,
          nodeType: node.nodeType,
          arcId: node.arcId,
          jsonPath: node.jsonPath,
          value: node.value,
          operatorType: node.operatorType,
          inputNodes: node.inputNodes || [],
          selected: false
        }
      };
    });
  }

  private generateEdgesFromInputNodes(nodes: CustomNode[]): Edge[] {
    const edges: Edge[] = [];
    nodes.forEach(node => {
      if (node.inputNodes && node.inputNodes.length > 0) {
        node.inputNodes.forEach(inputNode => {
          const edge: Edge = {
            id: `${inputNode.id}->${node.id}`,
            source: inputNode.id.toString(),
            target: node.id.toString(),
            type: 'default'
          };
          if (node.nodeType === 'LOGIC' && (node.inputNodes?.length || 0) > 1) {
            edge.targetHandle = `input-${inputNode.orderIndex}`;
          }
          edges.push(edge);
        });
      }
    });
    return edges;
  }

  private convertToSaveFormat(): GraphData {
    const customNodes: CustomNode[] = this.nodes.map((vflowNode: any) => {
      const nodeData = vflowNode.data;
      return {
        id: parseInt(vflowNode.id),
        offsetX: Math.round(vflowNode.point.x / 250),
        offsetY: Math.round(vflowNode.point.y / 150),
        nodeType: nodeData.nodeType,
        name: nodeData.name,
        arcId: nodeData.arcId,
        jsonPath: nodeData.jsonPath,
        value: nodeData.value,
        operatorType: nodeData.operatorType,
        inputNodes: nodeData.inputNodes
      };
    });
    return {
      nodes: customNodes,
      edges: this.edges
    };
  }
  //#endregion

  // sTODO
  //#region Mock Backend API
  private mockApiGetGraphDataFromDatabase(): Observable<GraphData> {
    const databaseResponse = {
      "nodes": [
        {
          "id": 0,
          "offsetX": 0,
          "offsetY": 0,
          "inputNodes": [],
          "nodeType": "PROVIDER",
          "name": "currentTemperature",
          "arcId": 22,
          "jsonPath": "source.anlageAAS.arc_temperature22.sensorData.currentTemperature"
        },
        {
          "id": 1,
          "offsetX": 1,
          "offsetY": 1,
          "inputNodes": [],
          "nodeType": "CONSTANT",
          "name": "tempOffset",
          "value": -2.0
        },
        {
          "id": 2,
          "offsetX": 1,
          "offsetY": 0,
          "inputNodes": [],
          "nodeType": "PROVIDER",
          "name": "maxAllowedThreshold",
          "arcId": 11,
          "jsonPath": "source.anlageAAS.arc_thresholds38.maxAllowedTemp"
        },
        {
          "id": 3,
          "offsetX": 0,
          "offsetY": 1,
          "inputNodes": [],
          "nodeType": "CONSTANT",
          "name": "SystemEnabledFlag",
          "value": true
        },
        {
          "id": 4,
          "offsetX": 2,
          "offsetY": 1,
          "inputNodes": [
            {
              "id": 0,
              "orderIndex": 0
            },
            {
              "id": 1,
              "orderIndex": 1
            }
          ],
          "nodeType": "LOGIC",
          "name": "addOperation",
          "operatorType": "ADD"
        },
        {
          "id": 5,
          "offsetX": 2,
          "offsetY": 2,
          "inputNodes": [
            {
              "id": 4,
              "orderIndex": 0
            },
            {
              "id": 2,
              "orderIndex": 1
            }
          ],
          "nodeType": "LOGIC",
          "name": "lessThanComparison",
          "operatorType": "LESS_THAN"
        },
        {
          "id": 6,
          "offsetX": 4,
          "offsetY": 0,
          "inputNodes": [
            {
              "id": 5,
              "orderIndex": 0
            },
            {
              "id": 3,
              "orderIndex": 1
            }
          ],
          "nodeType": "LOGIC",
          "name": "finalAndGate",
          "operatorType": "AND"
        }
      ]
    };

    const mockData: GraphData = {
      nodes: databaseResponse.nodes,
      edges: this.generateEdgesFromInputNodes(databaseResponse.nodes)
    };

    return of(mockData).pipe(delay(500));
  }

  saveGraph(): void {
    this.isSaving = true;
    this.saveMessage = '';

    try {
      const graphData = this.convertToSaveFormat();
      console.log('Saving graph data:', graphData);

      // sTODO Call the mock save API (replace with real API call later)
      this.mockApiSaveGraphData(graphData).subscribe({
        next: (response) => {
          this.isSaving = false;
          this.saveMessage = 'Graph saved successfully!';
          console.log('Graph saved successfully:', response);
          this.clearMessageAfter(3);
        },
        error: (error) => {
          this.isSaving = false;
          this.saveMessage = 'Error saving graph. Please try again.';
          console.error('Error saving graph:', error);
          this.clearMessageAfter(5);
        }
      });
    } catch (error) {
      this.isSaving = false;
      this.saveMessage = 'Error preparing graph data for save.';
      console.error('Error converting graph data:', error);
    }
  }

  // sTODO
  private mockApiSaveGraphData(graphData: GraphData): Observable<any> {
    console.log('Mock API: Saving to backend...', graphData);

    return of({
      success: true,
      message: 'Graph data saved successfully',
      timestamp: new Date().toISOString(),
      savedNodes: graphData.nodes.length,
      savedEdges: graphData.edges.length
    }).pipe(delay(1000));
  }
  //#endregion

}
