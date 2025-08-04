import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { delay, of, Observable } from 'rxjs';
import { Vflow, Node, Edge, Connection } from 'ngx-vflow';
import { ProviderNodeComponent, LogicNodeComponent, ConstantNodeComponent, ResultNodeComponent } from '../../components/nodes';

// Dynamic data types extracted from backend
type NodeDataType = string;

interface LogicOperator {
  operatorName: string;
  description: string;
  category: string; // category: general, number, string, boolean, array, object, datetime
  outputType: string;
  inputTypes: string[];
}

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
  outputType?: NodeDataType;
  expectedInputTypes?: NodeDataType[];
}

interface GraphData {
  nodes: CustomNode[];
  edges: Edge[];
}

// Backend API DTOs
interface VFlowGraphDTO {
  nodes: VFlowNodeDTO[];
  edges: VFlowEdgeDTO[];
}

interface VFlowNodeDTO {
  id: string;
  point: { x: number; y: number };
  type: string;
  width: number;
  height: number;
  data: VFlowNodeDataDTO;
}

interface VFlowEdgeDTO {
  id: string;
  source: string;
  target: string;
  targetHandle?: string;
  type?: string;
}

interface VFlowNodeDataDTO {
  nodeType: string;
  name: string;
  arcId?: number;
  jsonPath?: string;
  value?: any;
  operatorType?: string;
  inputNodes?: { id: number; orderIndex: number }[];
}

interface GraphPersistenceResponseDTO {
  graphData: any;
  validationErrors: ValidationError[];
}

interface ValidationError {
  message: string;
  nodeId?: string;
  errorType: string;
}

@Component({
  selector: 'app-edit-rule',
  imports: [Vflow, FormsModule],
  templateUrl: './edit-rule.html',
  styleUrl: './edit-rule.scss'
})
export class EditRule implements OnInit {
  Object = Object;

  nodes: Node[] = [];
  edges: Edge[] = [];
  isSaving = false;
  saveMessage = '';
  currentRuleId: number | null = null;
  ruleName = 'New Rule';

  // Error handling properties
  validationErrors: ValidationError[] = [];
  loadingErrors: string[] = [];
  showErrorPanel = false;
  isLoadingRule = false;

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

  // Logic operators
  logicOperators: LogicOperator[] = [];
  logicOperatorGroups: { [key: string]: LogicOperator[] } = {};

  // Dynamic type system
  availableDataTypes: Set<string> = new Set();
  typeDisplayInfo: { [key: string]: { color: string; icon: string; label: string } } = {};

  // Type safety properties
  showTypeIncompatibilityModal = false;
  typeIncompatibilityMessage = '';
  lastAttemptedConnection: { source: string; target: string; sourceType: string; targetType: string } | null = null;

  // Node palette context menu properties
  showNodePaletteMenu = false;
  nodePaletteMenuPosition = { x: 0, y: 0 };

  // Logic group context menu properties
  showLogicGroupMenu = false;
  logicGroupMenuPosition = { x: 0, y: 0 };

  // Logic operators context menu properties
  showLogicOperatorsMenu = false;
  logicOperatorsMenuPosition = { x: 0, y: 0 };
  selectedLogicGroup = '';

  // Node creation state - awaiting canvas click for positioning
  pendingNodeCreation: { type: string, operator?: LogicOperator } | null = null;

  cancelNodePlacement() {
    console.log('Node placement cancelled');
    this.pendingNodeCreation = null;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadLogicOperators();

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

  // Load logic operators from API
  private loadLogicOperators() {
    this.http.get<any>(`/api/config/transformation-rule/operators`).subscribe({
      next: (operators: LogicOperator[]) => {
        this.logicOperators = operators;
        this.groupLogicOperators();
        this.extractAndBuildTypeSystem(operators);
        console.log('Logic operator groups:', this.logicOperatorGroups);
        console.log('Available data types:', Array.from(this.availableDataTypes));
      },
      error: (error: any) => {
        console.error('Error loading logic operators:', error);
      }
    });
  }

  private groupLogicOperators() {
    this.logicOperatorGroups = {};
    this.logicOperators.forEach(operator => {
      if (!this.logicOperatorGroups[operator.category]) {
        this.logicOperatorGroups[operator.category] = [];
      }
      this.logicOperatorGroups[operator.category].push(operator);
    });
  }

  private extractAndBuildTypeSystem(operators: LogicOperator[]) {
    this.availableDataTypes.clear();

    // Add standard types for constant nodes
    this.availableDataTypes.add('ANY');
    this.availableDataTypes.add('NUMBER');
    this.availableDataTypes.add('STRING');
    this.availableDataTypes.add('BOOLEAN');

    // Extract types from operators
    operators.forEach(operator => {
      if (operator.outputType) {
        this.availableDataTypes.add(operator.outputType);
      }

      if (operator.inputTypes) {
        operator.inputTypes.forEach(inputType => {
          this.availableDataTypes.add(inputType);
        });
      }
    });

    this.buildTypeDisplayInfo();
    console.log('Dynamic type system built:', Array.from(this.availableDataTypes));
  }

  /**
   * Builds display information for all available types
   */
  private buildTypeDisplayInfo() {
    // Default colors and icons for common types
    const typeDefaults: { [key: string]: { color: string; icon: string } } = {
      'NUMBER': { color: '#2196F3', icon: '🔢' },
      'STRING': { color: '#4CAF50', icon: '📝' },
      'BOOLEAN': { color: '#FF9800', icon: '✅' },
      'DATE': { color: '#9C27B0', icon: '📅' },
      'ARRAY': { color: '#795548', icon: '📋' },
      'JSON': { color: '#607D8B', icon: '🗃️' },
      'PROVIDER': { color: '#3F51B5', icon: '📡' },
      'SCHEMA': { color: '#009688', icon: '📋' },
      'ANY': { color: '#757575', icon: '❓' }
    };

    // Generate colors for unknown types
    const fallbackColors = ['#E91E63', '#673AB7', '#8BC34A', '#FFC107', '#FF5722', '#00BCD4'];
    let colorIndex = 0;

    this.typeDisplayInfo = {};

    Array.from(this.availableDataTypes).forEach(type => {
      if (typeDefaults[type]) {
        this.typeDisplayInfo[type] = {
          color: typeDefaults[type].color,
          icon: typeDefaults[type].icon,
          label: type
        };
      } else {
        // Generate display info for unknown types
        this.typeDisplayInfo[type] = {
          color: fallbackColors[colorIndex % fallbackColors.length],
          icon: '🔸',
          label: type
        };
        colorIndex++;
      }
    });
  }

  // Load rule data from backend API
  private loadRuleData() {
    if (!this.currentRuleId) {
      console.log('No rule ID provided');
      this.loadingErrors = ['No rule ID provided'];
      this.showErrorPanel = true;
      this.ruleName = 'New Rule';
      this.nodes = [];
      this.edges = [];
      console.log('Error panel shown for missing rule ID');
      return;
    }

    this.isLoadingRule = true;
    this.loadingErrors = [];
    console.log('Loading rule metadata...');

    this.http.get<any>(`/api/config/transformation-rule/${this.currentRuleId}`).subscribe({
      next: (transformationRule) => {
        console.log('Rule metadata loaded successfully:', transformationRule);
        this.ruleName = transformationRule.name || `Rule ${this.currentRuleId}`;
        this.loadGraphData();
      },
      error: (error) => {
        console.error('Error loading rule metadata:', error);
        console.log('Rule metadata error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
          error: error.error,
          fullError: error
        });

        this.isLoadingRule = false;

        // sTODO Error Handling
      }
    });
  }
  private loadGraphData() {
    this.isLoadingRule = true;
    this.loadingErrors = [];
    this.validationErrors = [];

    console.log('Loading graph data for rule ID:', this.currentRuleId);

    this.http.get<any>(`/api/config/transformation-rule/${this.currentRuleId}/graph`).subscribe({
      next: (graphResponse) => {
        console.log('Graph data loaded successfully:', graphResponse);
        this.isLoadingRule = false;

        if (graphResponse.errors && graphResponse.errors.length > 0) {
          console.log('Validation errors found in graph response:');
          console.log('Errors array:', graphResponse.errors);
          console.log('Error count:', graphResponse.errors.length);

          graphResponse.errors.forEach((error: any, index: number) => {
            console.log(`Error ${index + 1}:`, {
              message: error.message,
              nodeId: error.nodeId,
              errorType: error.errorType,
              fullError: error
            });
          });

          this.validationErrors = graphResponse.errors;
          this.showErrorPanel = true;
          console.log('Error panel will be shown with', this.validationErrors.length, 'validation errors');
        } else {
          console.log('No validation errors found in response');
          this.validationErrors = [];
          this.showErrorPanel = false;
        }

        if (graphResponse.nodes && graphResponse.nodes.length > 0) {
          this.nodes = this.convertVFlowNodesToFrontend(graphResponse.nodes);
          this.edges = graphResponse.edges || [];
        } else {
          this.nodes = [];
          this.edges = [];
        }
      },
      error: (error) => {
        console.error('Error loading graph data:', error);
        console.log('Error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
          error: error.error,
          fullError: error
        });

        this.isLoadingRule = false;
        // sTODO Error Handling
      }
    });
  }

  private convertVFlowNodesToFrontend(vflowNodes: any[]): Node[] {
    return vflowNodes.map(vflowNode => {
      let nodeComponent: any;
      switch (vflowNode.data.nodeType) {
        case 'PROVIDER':
          nodeComponent = ProviderNodeComponent;
          break;
        case 'LOGIC':
          nodeComponent = LogicNodeComponent;
          break;
        case 'CONSTANT':
          nodeComponent = ConstantNodeComponent;
          break;
        case 'FINAL':
          nodeComponent = ResultNodeComponent;
          break;
        default:
          nodeComponent = ProviderNodeComponent;
      }

      return {
        id: vflowNode.id,
        point: { x: vflowNode.point.x, y: vflowNode.point.y },
        type: nodeComponent,
        width: vflowNode.width || 220,
        height: vflowNode.height || 100,
        data: vflowNode.data
      };
    });
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

  onDragStartLogicOperator(event: DragEvent, operator: LogicOperator) {
    if (event.dataTransfer) {
      event.dataTransfer.setData('text/plain', `LOGIC_${operator.operatorName}`);
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
    console.log('Creating new node:', nodeType, 'at position:', x, y);

    const newId = this.getNextNodeId();
    let nodeComponent: any;
    let defaultName: string;
    let nodeData: any = {
      nodeType: nodeType.startsWith('LOGIC_') ? 'LOGIC' : nodeType,
      inputNodes: [],
      selected: false
    };

    // Handle specific logic operators
    if (nodeType.startsWith('LOGIC_')) {
      const operatorName = nodeType.replace('LOGIC_', '');
      const operator = this.logicOperators.find(op => op.operatorName === operatorName);

      nodeComponent = LogicNodeComponent;
      defaultName = operator ? operator.operatorName : `Logic ${newId}`;
      nodeData = {
        ...nodeData,
        name: defaultName,
        operatorType: operatorName,
        description: operator?.description || '',
        inputTypes: operator?.inputTypes || [],
        outputType: operator?.outputType || 'BOOLEAN'
      };
    } else {
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
    }

    const newNode: Node = {
      id: newId.toString(),
      point: { x: x - 110, y: y - 50 }, // Offset to center
      type: nodeComponent,
      width: 220,
      height: 100,
      data: nodeData
    };

    console.log('Adding node to array:', newNode);
    this.nodes = [...this.nodes, newNode];
    console.log('Total nodes now:', this.nodes.length);
  }

  //#region Type Safety Methods
  getTypeDisplayInfo(type: NodeDataType): { color: string; icon: string; label: string } {
    return this.typeDisplayInfo[type] || this.typeDisplayInfo['ANY'] || {
      color: '#757575',
      icon: '❓',
      label: type || 'UNKNOWN'
    };
  }

  getNodeAcceptedInputTypes(node: Node): NodeDataType[] {
    const nodeData = (node as any).data;

    switch (nodeData.nodeType) {
      case 'PROVIDER':
        return [];

      case 'CONSTANT':
        return [];

      case 'RESULT':
        return ['Boolean'];

      case 'LOGIC':
        const operator = this.logicOperators.find(op => op.operatorName === nodeData.operatorType);
        if (operator && operator.inputTypes) {
          return operator.inputTypes as NodeDataType[];
        }
        return ['ANY']; // Fallback

      default:
        return ['ANY'];
    }
  }

  /**
   * Returns the output type of a node
   */
  getNodeOutputType(node: Node): NodeDataType {
    const nodeData = (node as any).data;

    switch (nodeData.nodeType) {
      case 'PROVIDER':
        return 'ANY';

      case 'CONSTANT':
        return this.inferTypeFromValue(nodeData.value);

      case 'LOGIC':
        const operator = this.logicOperators.find(op => op.operatorName === nodeData.operatorType);
        return (operator?.outputType as NodeDataType) || 'BOOLEAN';

      default:
        return 'ANY';
    }
  }

  /**
   * Returns the expected input type for a logic node at a specific input index
   */
  private getExpectedInputType(node: Node, inputIndex: number): NodeDataType {
    const nodeData = (node as any).data;

    if (nodeData.nodeType !== 'LOGIC') {
      return 'ANY';
    }

    const operator = this.logicOperators.find(op => op.operatorName === nodeData.operatorType);
    if (!operator || !operator.inputTypes) return 'ANY';

    if (inputIndex >= operator.inputTypes.length) {
      return operator.inputTypes[operator.inputTypes.length - 1] as NodeDataType;
    }

    return operator.inputTypes[inputIndex] as NodeDataType;
  }

  /**
   * Infers the data type from a JavaScript value
   */
  private inferTypeFromValue(value: any): NodeDataType {
    if (value === null || value === undefined) return 'ANY';

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
   * Checks if two types are compatible for edge connection
   */
  private areTypesCompatible(sourceType: NodeDataType, targetType: NodeDataType): boolean {
    if (sourceType === 'ANY' || targetType === 'ANY') return true;
    if (sourceType === targetType) return true;
    return false;
  }

  /**
   * Validates a connection between two nodes and shows warning if incompatible
   */
  private validateConnection(sourceId: string, targetId: string): boolean {
    const sourceNode = this.nodes.find(n => n.id === sourceId);
    const targetNode = this.nodes.find(n => n.id === targetId);

    if (!sourceNode || !targetNode) return false;

    const sourceType = this.getNodeOutputType(sourceNode);

    const targetNodeData = (targetNode as any).data;
    const currentInputCount = targetNodeData.inputNodes?.length || 0;
    const expectedTargetType = this.getExpectedInputType(targetNode, currentInputCount);

    const isCompatible = this.areTypesCompatible(sourceType, expectedTargetType);

    if (!isCompatible) {
      // Store connection attempt details for the warning modal
      this.lastAttemptedConnection = {
        source: sourceId,
        target: targetId,
        sourceType: sourceType,
        targetType: expectedTargetType
      };

      this.typeIncompatibilityMessage = `Cannot connect ${sourceType} to ${expectedTargetType}.`;
      this.showTypeIncompatibilityModal = true;

      return false;
    }

    return true;
  }

  /**
   * Closes the type incompatibility modal
   */
  closeTypeIncompatibilityModal(): void {
    this.showTypeIncompatibilityModal = false;
    this.typeIncompatibilityMessage = '';
    this.lastAttemptedConnection = null;
  }

  //#endregion

  private getNextNodeId(): number {
    if (this.nodes.length === 0) return 1;
    const maxId = Math.max(...this.nodes.map((node: any) => parseInt(node.id)));
    return maxId + 1;
  }

  public createEdge({ source, target, targetHandle }: Connection) {
    if (!this.validateConnection(source, target)) {
      return;
    }

    const newEdge: Edge = {
      id: `${source} -> ${target}`,
      source,
      target,
      targetHandle,
      type: 'default'
    };

    // sTODO order index bug: creates a new anchor without connecting to the new anchor point
    // Add the source node to the target node's inputNodes array
    this.nodes = this.nodes.map((node: any) => {
      if (node.id === target) {
        const nodeData = node.data;
        const inputNodes = nodeData.inputNodes || [];

        const existingInput = inputNodes.find((input: any) => input.id === parseInt(source));
        if (!existingInput) {
          const maxOrderIndex = inputNodes.length > 0
            ? Math.max(...inputNodes.map((input: any) => input.orderIndex))
            : -1;

          const newInputNode = {
            id: parseInt(source),
            orderIndex: maxOrderIndex + 1
          };

          const updatedNode = {
            ...node,
            data: {
              ...nodeData,
              inputNodes: [...inputNodes, newInputNode]
            }
          };

          return updatedNode;
        }
      }
      return node;
    });

    this.edges = [...this.edges, newEdge];
    console.log('Valid connection created:', this.edges);
  }
  //#endregion

  //#region Node Palette Menu
  onCanvasClick(event?: any) {
    console.log('Canvas click detected, pendingNodeCreation:', this.pendingNodeCreation);

    // If pending node creation, create node at click location
    if (this.pendingNodeCreation) {
      console.log('Creating node from canvas click at cursor position');

      // Get the canvas element to calculate relative position
      const canvasElement = document.querySelector('.vflow-wrapper') as HTMLElement;
      if (canvasElement && event?.clientX && event?.clientY) {
        const rect = canvasElement.getBoundingClientRect();
        const x = event.clientX - rect.left;
        const y = event.clientY - rect.top;

        console.log('Canvas click position:', x, y);

        if (this.pendingNodeCreation.operator) {
          // Create Logic operator node
          this.createNewNode(`LOGIC_${this.pendingNodeCreation.operator.operatorName}`, x, y);
        } else {
          // Create PROVIDER or CONSTANT node
          this.createNewNode(this.pendingNodeCreation.type, x, y);
        }
      } else {
        // Fallback - create at center
        console.log('Creating node at center as fallback');
        if (this.pendingNodeCreation.operator) {
          this.createNewNode(`LOGIC_${this.pendingNodeCreation.operator.operatorName}`, 300, 200);
        } else {
          this.createNewNode(this.pendingNodeCreation.type, 300, 200);
        }
      }

      this.pendingNodeCreation = null;
      return;
    }

    // Normal canvas click behavior
    if (true) {
      console.log('Canvas click processed - clearing selection and closing menus');
      this.selectedNode = null;
      this.hideAllContextMenus();
      this.selectedEdge = null;

      // Also close our node creation menus
      this.showNodePaletteMenu = false;
      this.showLogicGroupMenu = false;
      this.showLogicOperatorsMenu = false;
      this.selectedLogicGroup = '';

      this.updateNodeSelection();
    }
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

  openNodePaletteMenu(event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();

    this.hideAllContextMenus();
    this.nodePaletteMenuPosition = {
      x: event.clientX,
      y: event.clientY
    };
    this.showNodePaletteMenu = true;

    // Use a timeout to avoid immediate closure
    setTimeout(() => {
      document.addEventListener('click', this.hideNodePaletteMenuHandler, { once: true });
    }, 10);
  }

  private hideNodePaletteMenuHandler = (event: Event) => {
    const target = event.target as HTMLElement;
    const menu = target.closest('.context-menu');
    if (!menu) {
      this.hideAllContextMenus();
    }
  }

  hideNodePaletteMenu() {
    this.showNodePaletteMenu = false;
  }

  selectProviderNode(event: MouseEvent) {
    console.log('Provider node selected - awaiting canvas click for positioning');
    event.stopPropagation();
    this.pendingNodeCreation = { type: 'PROVIDER' };

    // Close context menus
    this.showNodePaletteMenu = false;
    this.showLogicGroupMenu = false;
    this.showLogicOperatorsMenu = false;
    this.selectedLogicGroup = '';

    console.log('Provider node ready for placement - click on canvas to place it');
  }

  selectConstantNode(event: MouseEvent) {
    console.log('Constant node selected - awaiting canvas click for positioning');
    event.stopPropagation();
    this.pendingNodeCreation = { type: 'CONSTANT' };

    // Close context menus
    this.showNodePaletteMenu = false;
    this.showLogicGroupMenu = false;
    this.showLogicOperatorsMenu = false;
    this.selectedLogicGroup = '';

    console.log('Constant node ready for placement - click on canvas to place it');
  }

  selectLogicNode(event: MouseEvent) {
    console.log('Logic node selected - opening group menu');
    event.stopPropagation();
    // Open next logic node palette menu
    this.openLogicGroupMenu(event);
  }

  openLogicGroupMenu(event: MouseEvent) {
    // Position to the right of the node palette menu
    const nodePaletteMenuElement = document.querySelector('.node-palette-menu') as HTMLElement;
    let x = event.clientX;
    let y = event.clientY;

    if (nodePaletteMenuElement) {
      const rect = nodePaletteMenuElement.getBoundingClientRect();
      x = rect.right + 5;
      y = rect.top;
    }

    this.logicGroupMenuPosition = { x, y };
    this.showLogicGroupMenu = true;
  }

  hideLogicGroupMenu() {
    this.showLogicGroupMenu = false;
  }

  selectLogicGroup(groupName: string, event: MouseEvent) {
    event.stopPropagation();
    this.selectedLogicGroup = groupName;
    // Open logic group context menu
    this.openLogicOperatorsMenu(event);
  }

  openLogicOperatorsMenu(event: MouseEvent) {
    // Position to the right of the logic group menu
    const logicGroupMenuElement = document.querySelector('.logic-group-menu') as HTMLElement;
    let x = event.clientX;
    let y = event.clientY;

    if (logicGroupMenuElement) {
      const rect = logicGroupMenuElement.getBoundingClientRect();
      x = rect.right + 5;
      y = rect.top;
    }

    this.logicOperatorsMenuPosition = { x, y };
    this.showLogicOperatorsMenu = true;
  }

  hideLogicOperatorsMenu() {
    this.showLogicOperatorsMenu = false;
  }

  selectLogicOperator(operator: LogicOperator, event: MouseEvent) {
    event.stopPropagation();
    console.log('Logic operator selected - awaiting canvas click for positioning:', operator);
    this.pendingNodeCreation = { type: 'LOGIC', operator: operator };

    // Close context menus
    this.showNodePaletteMenu = false;
    this.showLogicGroupMenu = false;
    this.showLogicOperatorsMenu = false;
    this.selectedLogicGroup = '';

    console.log(`${operator.operatorName} ready for placement - click on canvas to place it`);
  }

  private hideAllContextMenus() {
    this.hideContextMenu();
    this.hideNodePaletteMenu();
    this.hideLogicGroupMenu();
    this.hideLogicOperatorsMenu();
    this.hideEdgeContextMenu();

    // Also close node creation menus and reset state
    this.showNodePaletteMenu = false;
    this.showLogicGroupMenu = false;
    this.showLogicOperatorsMenu = false;
    this.selectedLogicGroup = '';
  }

  // Get operators for selected group
  getOperatorsForGroup(groupName: string): LogicOperator[] {
    return this.logicOperatorGroups[groupName] || [];
  }

  getGroupDisplayName(groupName: string): string {
    const displayNames: { [key: string]: string } = {
      'general': '🔧 General',
      'number': '🔢 Number',
      'string': '📝 String',
      'boolean': '✅ Boolean',
      'array': '📋 Array',
      'object': '🗃️ Object',
      'datetime': '📅 DateTime'
    };
    return displayNames[groupName] || groupName;
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
      const operator = this.logicOperators.find(op => op.operatorName === this.editingOperator.trim());

      // Update node data
      const nodeData = (this.selectedNode as any).data;
      const updatedNode = {
        ...this.selectedNode,
        data: {
          ...nodeData,
          operatorType: this.editingOperator.trim(),
          description: operator?.description || '',
          inputTypes: operator?.inputTypes || [],
          outputType: operator?.outputType || 'BOOLEAN'
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
      // Parse the value to the respective type
      let parsedValue = this.editingConstantValue;
      if (!isNaN(Number(this.editingConstantValue))) {
        parsedValue = Number(this.editingConstantValue);
      }
      else if (this.editingConstantValue.toLowerCase() === 'true') {
        parsedValue = true;
      }
      else if (this.editingConstantValue.toLowerCase() === 'false') {
        parsedValue = false;
      }

      // Update node data
      const nodeData = (this.selectedNode as any).data;
      const updatedNode = {
        ...this.selectedNode,
        data: {
          ...nodeData,
          value: parsedValue
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

      // Update input node references for remaining nodes
      this.nodes = this.nodes.map((node: any) => {
        if (node.data.inputNodes) {
          const updatedInputNodes = node.data.inputNodes.filter((input: any) =>
            input.id !== parseInt(this.selectedNode!.id)
          );
          return {
            ...node,
            data: {
              ...node.data,
              inputNodes: updatedInputNodes
            }
          };
        }
        return node;
      });

      this.selectedNode = null;
      this.hideContextMenu();
    }
  }

  public openEdgeContextMenu(event?: any, edge?: Edge) {
    let selectedEdge: Edge | null = null;
    let menuPosition: { x: number; y: number } = { x: window.innerWidth / 2, y: window.innerHeight / 2 };

    if (Array.isArray(event) && event.length > 0 && event[0].id && event[0].selected) {
      selectedEdge = this.edges.find(e => e.id === event[0].id) || null;

      // Calculate menu position based on edge source and target node positions
      if (selectedEdge) {
        const sourceNode = this.nodes.find(n => n.id === selectedEdge!.source);
        const targetNode = this.nodes.find(n => n.id === selectedEdge!.target);

        if (sourceNode && targetNode) {
          // Calculate midpoint between source and target nodes
          const midX = (sourceNode.point.x + targetNode.point.x) / 2;
          const midY = (sourceNode.point.y + targetNode.point.y) / 2;

          // Convert to screen coordinates relative to the canvas
          const canvasElement = document.querySelector('vflow') as HTMLElement;
          if (canvasElement) {
            const rect = canvasElement.getBoundingClientRect();
            menuPosition = {
              x: rect.left + midX + 110, // Add node width offset
              y: rect.top + midY + 50    // Add node height offset
            };
          } else {
            // Fallback if canvas not found
            menuPosition = { x: midX + 110, y: midY + 50 };
          }
        }
      }
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

      // Update the target node's inputNodes array
      const targetNodeId = this.selectedEdge.target;
      const sourceNodeId = parseInt(this.selectedEdge.source);

      this.nodes = this.nodes.map((node: any) => {
        if (node.id === targetNodeId && node.data.inputNodes) {
          const updatedInputNodes = node.data.inputNodes.filter((input: any) =>
            input.id !== sourceNodeId
          );
          return {
            ...node,
            data: {
              ...node.data,
              inputNodes: updatedInputNodes
            }
          };
        }
        return node;
      });

      this.selectedEdge = null;
      this.hideEdgeContextMenu();
    }
  }

  /**
   * Converts the current graph to the VFlowGraphDTO format expected by the backend
   */
  private convertToVFlowGraphDTO(): VFlowGraphDTO {
    // Convert nodes to VFlowNodeDTO format
    const vflowNodes: VFlowNodeDTO[] = this.nodes.map((node: any) => {
      const nodeData = node.data;

      const vflowNodeData: VFlowNodeDataDTO = {
        nodeType: nodeData.nodeType,
        name: nodeData.name,
        inputNodes: nodeData.inputNodes || []
      };

      // Add node-type specific data
      if (nodeData.nodeType === 'PROVIDER') {
        vflowNodeData.arcId = nodeData.arcId;
        vflowNodeData.jsonPath = nodeData.jsonPath;
      } else if (nodeData.nodeType === 'CONSTANT') {
        vflowNodeData.value = nodeData.value;
      } else if (nodeData.nodeType === 'LOGIC') {
        vflowNodeData.operatorType = nodeData.operatorType;
      }

      return {
        id: node.id,
        point: { x: node.point.x, y: node.point.y },
        type: nodeData.nodeType, // Map component type to string
        width: node.width || 220,
        height: node.height || 100,
        data: vflowNodeData
      };
    });

    // Convert edges to VFlowEdgeDTO format
    const vflowEdges: VFlowEdgeDTO[] = this.edges.map((edge: any) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      targetHandle: edge.targetHandle,
      type: edge.type || 'default'
    }));

    return {
      nodes: vflowNodes,
      edges: vflowEdges
    };
  }
  //#endregion

  //#region Error
  closeErrorPanel(): void {
    console.log('Closing error panel');
    console.log('Current state before close:', {
      validationErrors: this.validationErrors.length,
      loadingErrors: this.loadingErrors.length,
      showErrorPanel: this.showErrorPanel
    });

    this.showErrorPanel = false;
    this.validationErrors = [];
    this.loadingErrors = [];

    console.log('Error panel closed and errors cleared');
  }

  retryLoadRule(): void {
    console.log('Retrying rule load...');
    this.closeErrorPanel();
    this.loadRuleData();
  }

  navigateToErrorNode(nodeId: string): void {
    console.log('Navigating to error node:', nodeId);
    const targetNode = this.nodes.find(n => n.id === nodeId);
    if (targetNode) {
      this.selectedNode = targetNode;
      this.updateNodeSelection();

      console.log('Successfully navigated to node:', {
        nodeId: nodeId,
        nodeName: (targetNode as any).data?.name,
        nodeType: (targetNode as any).data?.nodeType
      });

    } else {
      console.warn('Could not find node with ID:', nodeId);
      console.log('Available node IDs:', this.nodes.map(n => n.id));
    }
  }

  getErrorTypeDisplayName(errorType: string): string {
    const displayName = (() => {
      switch (errorType) {
        case 'VALIDATION_ERROR':
          return 'Validation Error';
        case 'TYPE_MISMATCH':
          return 'Type Mismatch';
        case 'MISSING_CONNECTION':
          return 'Missing Connection';
        case 'INVALID_CONFIGURATION':
          return 'Invalid Configuration';
        default:
          return errorType.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
      }
    })();

    console.log('Error type display name:', { errorType, displayName });
    return displayName;
  }

  hasErrors(): boolean {
    const hasErrors = this.validationErrors.length > 0 || this.loadingErrors.length > 0;
    console.log('🔍 Has errors check:', {
      validationErrors: this.validationErrors.length,
      loadingErrors: this.loadingErrors.length,
      hasErrors: hasErrors
    });
    return hasErrors;
  }

  getErrorCount(): number {
    const count = this.validationErrors.length + this.loadingErrors.length;
    console.log('📊 Total error count:', {
      validationErrors: this.validationErrors.length,
      loadingErrors: this.loadingErrors.length,
      total: count
    });
    return count;
  }
  //#endregion

  saveGraph(): void {
    this.isSaving = true;
    this.saveMessage = '';

    // Validate that we have a rule ID to save to
    if (!this.currentRuleId) {
      this.isSaving = false;
      this.saveMessage = 'Error: No rule ID found. Cannot save graph.';
      console.error('Cannot save graph: No currentRuleId');
      this.clearMessageAfter(5);
      return;
    }

    try {
      // Convert frontend graph format to backend VFlowGraphDTO format
      const vflowGraphData = this.convertToVFlowGraphDTO();
      console.log('Saving graph data to backend:', vflowGraphData);

      // Call the backend API to save the transformation rule graph
      this.http.put<GraphPersistenceResponseDTO>(`/api/config/transformation-rule/${this.currentRuleId}/graph`, vflowGraphData)
        .subscribe({
          next: (response) => {
            this.isSaving = false;

            // Check for validation errors
            if (response.validationErrors && response.validationErrors.length > 0) {
              console.log('Validation errors returned from save operation:');
              console.log('Save validation errors array:', response.validationErrors);
              console.log('Save error count:', response.validationErrors.length);

              // Log each save validation error individually
              response.validationErrors.forEach((error: any, index: number) => {
                console.log(`Save Error ${index + 1}:`, {
                  message: error.message,
                  nodeId: error.nodeId,
                  errorType: error.errorType,
                  fullError: error
                });
              });

              this.validationErrors = response.validationErrors;
              this.showErrorPanel = true;

              const errorMessages = response.validationErrors.map(err => err.message).join('; ');
              this.saveMessage = `Graph saved with ${response.validationErrors.length} warning(s). Check error panel for details.`;
              console.log('Error panel updated with save validation errors');
              this.clearMessageAfter(8);
            } else {
              console.log('Save successful with no validation errors');
              // Clear any existing validation errors on successful save
              this.validationErrors = [];
              if (this.loadingErrors.length === 0) {
                this.showErrorPanel = false;
                console.log('Error panel hidden - no errors remaining');
              }

              this.saveMessage = 'Graph saved successfully!';
              console.log('Graph saved successfully:', response);
              this.clearMessageAfter(3);
            }
          },
          error: (error) => {
            this.isSaving = false;
            this.saveMessage = 'Error saving graph. Please try again.';
            console.error('Error saving graph:', error);

            // Provide more specific error messages based on status
            if (error.status === 404) {
              this.saveMessage = 'Error: Transformation rule not found.';
            } else if (error.status === 400) {
              this.saveMessage = 'Error: Invalid graph data.';
            } else if (error.status >= 500) {
              this.saveMessage = 'Error: Server error. Please try again later.';
            }

            this.clearMessageAfter(5);
          }
        });
    } catch (error) {
      this.isSaving = false;
      this.saveMessage = 'Error preparing graph data for save.';
      console.error('Error converting graph data:', error);
      this.clearMessageAfter(5);
    }
  }
  //#endregion

}
