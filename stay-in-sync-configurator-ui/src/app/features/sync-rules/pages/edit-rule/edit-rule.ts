import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { delay, of, Observable } from 'rxjs';
import { Vflow, Node, Edge, Connection } from 'ngx-vflow';
import { ProviderNodeComponent, LogicNodeComponent, ConstantNodeComponent } from '../../components/nodes';

// Dynamic data types extracted from backend
type NodeDataType = string;

interface LogicOperator {
  operatorName: string;
  description: string;
  inputTypes: string[];
  outputType: string;
  operatorType: string; // Groups: general, number, string, boolean, array, object, datetime
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
    private router: Router
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

  // Load logic operators from mock API
  private loadLogicOperators() {
    this.mockApiGetLogicOperators().subscribe({
      next: (operators: LogicOperator[]) => {
        this.logicOperators = operators;
        this.groupLogicOperators();
        this.extractAndBuildTypeSystem(operators);
        console.log('Logic operators loaded:', operators);
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
      if (!this.logicOperatorGroups[operator.operatorType]) {
        this.logicOperatorGroups[operator.operatorType] = [];
      }
      this.logicOperatorGroups[operator.operatorType].push(operator);
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

          setTimeout(() => {
            console.log('Graph loaded and ready');
          }, 100);
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

  // Mock API method for logic operators
  private mockApiGetLogicOperators(): Observable<LogicOperator[]> {
    const operators: LogicOperator[] = [
      {
        "operatorName": "ADD",
        "description": "Adds two or more numbers.",
        "inputTypes": ["NUMBER", "NUMBER"],
        "outputType": "NUMBER",
        "operatorType": "number"
      },
      {
        "operatorName": "AFTER",
        "description": "Checks if a date is after another date.",
        "inputTypes": ["DATE", "DATE"],
        "outputType": "BOOLEAN",
        "operatorType": "datetime"
      },
      {
        "operatorName": "AGE_GREATER_THAN",
        "description": "Checks if a date is older than a specific duration.",
        "inputTypes": ["DATE", "NUMBER", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "datetime"
      },
      {
        "operatorName": "ALL_OF",
        "description": "Succeeds if all inputs are true (predicative).",
        "inputTypes": ["BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "AND",
        "description": "Performs a strict logical AND operation.",
        "inputTypes": ["BOOLEAN", "BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "AVG",
        "description": "Calculates the average of all numbers in a list.",
        "inputTypes": ["ARRAY"],
        "outputType": "NUMBER",
        "operatorType": "array"
      },
      {
        "operatorName": "BEFORE",
        "description": "Checks if a date is before another date.",
        "inputTypes": ["DATE", "DATE"],
        "outputType": "BOOLEAN",
        "operatorType": "datetime"
      },
      {
        "operatorName": "BETWEEN",
        "description": "Checks if a number is between two bounds (inclusive).",
        "inputTypes": ["NUMBER", "NUMBER", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "number"
      },
      {
        "operatorName": "BETWEEN_DATES",
        "description": "Checks if a date is between two other dates.",
        "inputTypes": ["DATE", "DATE", "DATE"],
        "outputType": "BOOLEAN",
        "operatorType": "datetime"
      },
      {
        "operatorName": "CONTAINS",
        "description": "Checks if a string contains another string.",
        "inputTypes": ["STRING", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "CONTAINS_ALL",
        "description": "Checks if a list contains all elements from another list.",
        "inputTypes": ["ARRAY", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "CONTAINS_ANY",
        "description": "Checks if a list contains at least one element from another list.",
        "inputTypes": ["ARRAY", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "CONTAINS_ELEMENT",
        "description": "Checks if an element is present in a list.",
        "inputTypes": ["ARRAY", "ANY"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "CONTAINS_NONE",
        "description": "Checks if a list contains no elements from another list.",
        "inputTypes": ["ARRAY", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "ENDS_WITH",
        "description": "Checks if a string ends with a specific suffix.",
        "inputTypes": ["STRING", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "EQUALS",
        "description": "Checks if two or more values are equal.",
        "inputTypes": ["ANY", "ANY"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "EQUALS_CASE_SENSITIVE",
        "description": "Performs a case-sensitive comparison of two strings.",
        "inputTypes": ["STRING", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "EQUALS_IGNORE_CASE",
        "description": "Performs a case-insensitive comparison of two strings.",
        "inputTypes": ["STRING", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "EXISTS",
        "description": "Checks if one or more JSON paths exist.",
        "inputTypes": ["PROVIDER"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "GREATER_OR_EQUAL",
        "description": "Checks if a number is greater than or equal to another.",
        "inputTypes": ["NUMBER", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "number"
      },
      {
        "operatorName": "GREATER_THAN",
        "description": "Checks if a number is greater than another.",
        "inputTypes": ["NUMBER", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "number"
      },
      {
        "operatorName": "HAS_ALL_KEYS",
        "description": "Checks if a JSON object has all specified keys.",
        "inputTypes": ["JSON", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "object"
      },
      {
        "operatorName": "HAS_ANY_KEY",
        "description": "Checks if a JSON object has at least one of the specified keys.",
        "inputTypes": ["JSON", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "object"
      },
      {
        "operatorName": "HAS_KEY",
        "description": "Checks if a JSON object has a specific key.",
        "inputTypes": ["JSON", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "object"
      },
      {
        "operatorName": "HAS_NO_KEYS",
        "description": "Checks if a JSON object has none of the specified keys.",
        "inputTypes": ["JSON", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "object"
      },
      {
        "operatorName": "IN_SET",
        "description": "Checks if a value is present in a set of values.",
        "inputTypes": ["ANY", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "IS_FALSE",
        "description": "Checks if a value is exactly false.",
        "inputTypes": ["BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "IS_NOT_NULL",
        "description": "Checks if a value exists and is not null.",
        "inputTypes": ["ANY"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "IS_NULL",
        "description": "Checks if a value is explicitly null.",
        "inputTypes": ["ANY"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "IS_TRUE",
        "description": "Checks if a value is exactly true.",
        "inputTypes": ["BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "LACKS_KEY",
        "description": "Checks if a JSON object lacks a specific key.",
        "inputTypes": ["JSON", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "object"
      },
      {
        "operatorName": "LENGTH_BETWEEN",
        "description": "Checks if a string's length is between two numbers.",
        "inputTypes": ["STRING", "NUMBER", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "LENGTH_EQUALS",
        "description": "Compares the length of a list with a number.",
        "inputTypes": ["ARRAY", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "LENGTH_GT",
        "description": "Checks if the length of a list is greater than a number.",
        "inputTypes": ["ARRAY", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "LENGTH_LT",
        "description": "Checks if the length of a list is less than a number.",
        "inputTypes": ["ARRAY", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "LESS_OR_EQUAL",
        "description": "Checks if a number is less than or equal to another.",
        "inputTypes": ["NUMBER", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "number"
      },
      {
        "operatorName": "LESS_THAN",
        "description": "Checks if a number is less than another.",
        "inputTypes": ["NUMBER", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "number"
      },
      {
        "operatorName": "MATCHES_SCHEMA",
        "description": "Validates a JSON document against a JSON schema.",
        "inputTypes": ["JSON", "SCHEMA"],
        "outputType": "BOOLEAN",
        "operatorType": "object"
      },
      {
        "operatorName": "MAX",
        "description": "Finds the largest number in a list.",
        "inputTypes": ["ARRAY"],
        "outputType": "NUMBER",
        "operatorType": "array"
      },
      {
        "operatorName": "MIN",
        "description": "Finds the smallest number in a list.",
        "inputTypes": ["ARRAY"],
        "outputType": "NUMBER",
        "operatorType": "array"
      },
      {
        "operatorName": "NONE_OF",
        "description": "Succeeds if no input is true (predicative).",
        "inputTypes": ["BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "NOT",
        "description": "Negates a single boolean value (strict).",
        "inputTypes": ["BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "NOT_BETWEEN",
        "description": "Checks if a number is outside of two bounds.",
        "inputTypes": ["NUMBER", "NUMBER", "NUMBER"],
        "outputType": "BOOLEAN",
        "operatorType": "number"
      },
      {
        "operatorName": "NOT_CONTAINS",
        "description": "Checks if a string does not contain another string.",
        "inputTypes": ["STRING", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "NOT_CONTAINS_ELEMENT",
        "description": "Checks if an element is not present in a list.",
        "inputTypes": ["ARRAY", "ANY"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "NOT_EMPTY",
        "description": "Checks if a list or array is not empty.",
        "inputTypes": ["ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "array"
      },
      {
        "operatorName": "NOT_EQUALS",
        "description": "Checks if two or more values are not equal.",
        "inputTypes": ["ANY", "ANY"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "NOT_EXISTS",
        "description": "Checks if one or more JSON paths do not exist.",
        "inputTypes": ["PROVIDER"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "NOT_IN_SET",
        "description": "Checks if a value is not present in a set of values.",
        "inputTypes": ["ANY", "ARRAY"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "ONE_OF",
        "description": "Succeeds if at least one input is true (predicative).",
        "inputTypes": ["BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "OR",
        "description": "Performs a strict logical OR operation.",
        "inputTypes": ["BOOLEAN", "BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      },
      {
        "operatorName": "REGEX_MATCH",
        "description": "Checks if a string matches a regular expression pattern.",
        "inputTypes": ["STRING", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "SAME_DAY",
        "description": "Checks if two dates are on the same calendar day.",
        "inputTypes": ["DATE", "DATE"],
        "outputType": "BOOLEAN",
        "operatorType": "datetime"
      },
      {
        "operatorName": "STARTS_WITH",
        "description": "Checks if a string starts with a specific prefix.",
        "inputTypes": ["STRING", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "string"
      },
      {
        "operatorName": "SUM",
        "description": "Calculates the sum of all numbers in a list.",
        "inputTypes": ["ARRAY"],
        "outputType": "NUMBER",
        "operatorType": "array"
      },
      {
        "operatorName": "TYPE_IS",
        "description": "Checks the Java type of a value.",
        "inputTypes": ["STRING", "ANY"],
        "outputType": "BOOLEAN",
        "operatorType": "general"
      },
      {
        "operatorName": "WITHIN_LAST",
        "description": "Checks if a date is within the last specified duration.",
        "inputTypes": ["DATE", "NUMBER", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "datetime"
      },
      {
        "operatorName": "WITHIN_NEXT",
        "description": "Checks if a date is within the next specified duration.",
        "inputTypes": ["DATE", "NUMBER", "STRING"],
        "outputType": "BOOLEAN",
        "operatorType": "datetime"
      },
      {
        "operatorName": "XOR",
        "description": "Checks if exactly one input is true (strict).",
        "inputTypes": ["BOOLEAN", "BOOLEAN"],
        "outputType": "BOOLEAN",
        "operatorType": "boolean"
      }
    ];

    return of(operators).pipe(delay(300));
  }
  //#endregion

}
