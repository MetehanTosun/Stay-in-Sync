import { Component, ElementRef, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Connection, Edge, EdgeChange, EdgeType, NodeChange, Vflow, VflowComponent } from 'ngx-vflow';
import { GraphAPIService, OperatorNodesApiService } from '../../service';
import { CustomVFlowNode, LogicOperatorMeta, NodeMenuItem, NodeType, VFlowGraphDTO } from '../../models';
import { ConstantNodeComponent, ConstantNodeModalComponent, FinalNodeComponent, LogicNodeComponent, ProviderNodeComponent, ProviderNodeModalComponent } from '..';
import { CommonModule } from '@angular/common';
import { SetNodeNameModalComponent } from '../modals/set-node-name-modal/set-node-name-modal.component';
import { ValidationError } from '../../models/interfaces/validation-error.interface';
import { ConfigNodeComponent } from '../nodes/config-node/config-node.component';
import { MessageService } from 'primeng/api';

/**
 * The canvas of the rule editor on which the rule graph is visualized
 */
@Component({
  selector: 'app-vflow-canvas',
  standalone: true,
  imports: [
    Vflow,
    CommonModule,
    SetNodeNameModalComponent,
    ProviderNodeModalComponent,
    ConstantNodeModalComponent
  ],
  templateUrl: './vflow-canvas.component.html',
  styleUrl: './vflow-canvas.component.css'
})
export class VflowCanvasComponent implements OnInit {
  //#region Setup
  nodes: CustomVFlowNode[] = [];
  edges: Edge[] = [];
  hasUnsavedChanges: boolean = false;
  ruleId: number | undefined = undefined;
  lastNodeId = 0;

  // Edge Validation Attributes
  showTypeIncompatibilityModal = false;
  typeIncompatibilityMessage = '';
  lastAttemptedConnection: { sourceType: string, targetType: string } | null = null;

  // Graph Element Selection Attributes
  selectedNode: CustomVFlowNode | null = null;
  selectedEdge: Edge | null = null;
  showNodeContextMenu = false;
  showEdgeContextMenu = false;
  edgeContextMenuPosition = { x: 0, y: 0 };
  nodeContextMenuPosition = { x: 0, y: 0 };
  lastMousePosition = { x: 0, y: 0 };
  //* Don't merge the positions

  // Modal Status
  editNodeNameModalOpen = false;
  editJsonPathModalOpen = false;
  editNodeValueModalOpen = false;
  nodeBeingEdited: CustomVFlowNode | null = null;
  showRemoveEdgesModal: boolean = false;

  // Autocomplete Feature
  suggestions: LogicOperatorMeta[] = []
  showSuggestions = false;

  // Pending Stuff
  pendingNodeValue: string | null = null;

  @Output() canvasClick = new EventEmitter<{ x: number, y: number }>();
  @Output() canvasRightClick = new EventEmitter<{
    viewportPos: { x: number, y: number },
    canvasPos: { x: number, y: number }
  }>();
  @Output() suggestionSelected = new EventEmitter<{ nodeType: NodeType, operator: LogicOperatorMeta }>();
  @Output() graphErrors = new EventEmitter<ValidationError[]>();

  @ViewChild(VflowComponent) vflowInstance!: VflowComponent;
  @ViewChild('canvasContainer') canvasContainer!: ElementRef<HTMLDivElement>;

  constructor(
    private route: ActivatedRoute,
    private graphApi: GraphAPIService,
    private nodesApi: OperatorNodesApiService,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    this.ruleId = routeId ? Number(routeId) : undefined;

    if (this.ruleId) {
      this.loadGraph(this.ruleId);
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'No rule id',
        detail: "Unable to load graph - cannot read rule id"
      })
    }
  }
  //#endregion

  //#region Template Methods
  /**
   * Calculates and emits the mouse position on the vflow canvas
   * @param mouseEvent
   */
  onCanvasClick(mouseEvent: MouseEvent) {
    // Calculate screen position relative to canvas
    const rect = (mouseEvent.currentTarget as HTMLElement).getBoundingClientRect();
    const x = mouseEvent.clientX - rect.left;
    const y = mouseEvent.clientY - rect.top;

    this.canvasClick.emit(this.screenToVFlowCoordinate(x, y));
  }

  /**
 * Calculates and emits the mouse position (in viewport and canvas coordinates) for right-click
 * @param mouseEvent
 */
  onCanvasRightClick(mouseEvent: MouseEvent) {
    mouseEvent.preventDefault();

    // Calculate screen position relative to canvas
    const rect = (mouseEvent.currentTarget as HTMLElement).getBoundingClientRect();
    const screenX = mouseEvent.clientX - rect.left;
    const screenY = mouseEvent.clientY - rect.top;

    const canvasPos = this.screenToVFlowCoordinate(screenX, screenY);
    const viewportPos = {
      x: mouseEvent.clientX,
      y: mouseEvent.clientY
    };

    this.canvasRightClick.emit({ viewportPos, canvasPos });
  }

  /**
   * Closes the modal that displays the data type incompatibility error of a new drawn edge
   */
  closeTypeIncompatibilityModal() {
    this.showTypeIncompatibilityModal = false;
    this.typeIncompatibilityMessage = '';
    this.lastAttemptedConnection = null;
  }

  /**
   * @param node
   * @returns the context menu of the given node
   */
  private getNodeMenuItems(node: CustomVFlowNode): NodeMenuItem[] {
    switch (node.data.nodeType) {
      case NodeType.CONSTANT:
        return [
          { label: 'Set Value', action: () => this.startEditingNodeValue(node) },
          { label: 'Suggestions', action: () => this.openSuggestionsMenu(node) },
          { label: 'Delete Node', action: () => this.deleteNode(node) }
        ];
      case NodeType.PROVIDER:
        return [
          { label: 'Set Name', action: () => this.startEditingNodeName(node) },
          { label: 'Edit JSON Path', action: () => this.startEditingJsonPath(node) },
          // TODO-s
          // { label: 'Suggestions', action: () => this.openSuggestionsMenu(node) },
          { label: 'Delete Node', action: () => this.deleteNode(node) }
        ];
      case NodeType.LOGIC:
        return [
          { label: 'Set Name', action: () => this.startEditingNodeName(node) },
          { label: 'Suggestions', action: () => this.openSuggestionsMenu(node) },
          { label: 'Delete Node', action: () => this.deleteNode(node) }
        ];
      case NodeType.FINAL:
        return [
          { label: 'Empty Menu', action: () => { } }
        ];
      case NodeType.CONFIG:
        return [
          { label: 'Toggle Mode', action: () => { this.toggleConfiguration("MODE") } },
          { label: 'Toggle Status', action: () => { this.toggleConfiguration("STATUS") } },
          {
            submenu: [
              { label: '5s', action: () => { this.setTimeWindow(5000) } },
              { label: '10s', action: () => { this.setTimeWindow(10000) } },
              { label: '15s', action: () => { this.setTimeWindow(15000) } },
              { label: '20s', action: () => { this.setTimeWindow(20000) } },
              { label: '25s', action: () => { this.setTimeWindow(25000) } },
              { label: '30s', action: () => { this.setTimeWindow(30000) } }
            ]
          }
        ];
    }
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
    providerData?: { jsonPath: string, outputType: string },
    constantValue?: any,
    operatorData?: LogicOperatorMeta,
  ) {
    let nodeData: any;

    if (nodeType === NodeType.PROVIDER && providerData) {
      nodeData = {
        name: providerData.jsonPath,
        jsonPath: providerData.jsonPath,
        outputType: providerData.outputType
      }
    } else if (nodeType === NodeType.CONSTANT && constantValue !== undefined) {
      nodeData = {
        name: `Constant: ${constantValue}`,
        value: constantValue,
        outputType: this.inferTypeFromValue(constantValue)
      }
    } else if (nodeType === NodeType.LOGIC && operatorData) {
      nodeData = {
        ...operatorData,
        name: operatorData.operatorName,
        operatorType: operatorData.operatorName,  // Map operatorName to operatorType for the Backend
      }
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Unable to create node',
        detail: 'Unable to create node. \n Please check the logs.'
      });
    }

    const size = this.getDefaultNodeSize(nodeType);
    const nodeCenter = {
      x: pos.x - size.width / 2,
      y: pos.y - size.height * (nodeType === NodeType.LOGIC ? 1.4 : nodeType === NodeType.PROVIDER ? 1.2 : 1.1)

    }

    // Create and add new node
    const newNode: CustomVFlowNode = {
      id: (++this.lastNodeId).toString(),
      point: nodeCenter,
      type: this.getNodeType(nodeType),
      width: this.getDefaultNodeSize(nodeType).width,
      height: this.getDefaultNodeSize(nodeType).height,
      data: {
        ...nodeData,
        nodeType: nodeType
      },
      contextMenuItems: []
    };
    newNode.contextMenuItems = this.getNodeMenuItems(newNode);
    this.nodes = [...this.nodes, newNode];
    this.hasUnsavedChanges = true;


    console.log(providerData)
    console.log(newNode)
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
    this.hasUnsavedChanges = true;
  }
  //#endregion

  //#region Element Selection
  /**
   * Opens the context menu of the selected node and closes the existing one if applicable
   *
   * @param changes
   */
  onNodeSelect(changes: NodeChange[]) {
    const change = changes.pop()!;
    if (change.type === 'select' && (change as any).selected) {
      this.closeNodeContextMenu();
      this.closeEdgeContextMenu();

      const node = this.nodes.find(e => e.id === change.id);
      if (node) {
        this.selectedNode = node;
        this.nodeContextMenuPosition = this.lastMousePosition;
        this.showNodeContextMenu = true;
      }
    } else if (this.selectedNode?.id === change.id) {
      this.closeNodeContextMenu();
    }
  }

  /**
   * Takes the positional change of a node and updates its point property
   *
   * @param changes
   */
  onNodePositionChange(changes: NodeChange[]) {
    this.closeNodeContextMenu();
    this.closeEdgeContextMenu();

    const change = changes.pop()!;
    if (change.type === 'position') {
      const node = this.nodes.find(n => n.id === change.id);
      if (node) {
        node.point = change.point;
        this.hasUnsavedChanges = true;
      }
    }
  }

  /**
   * Opens the edge context menu for an edge
   *
   * @param changes
   */
  onEdgeSelect(changes: EdgeChange[]) {
    const change = changes.pop()!;
    if (change.type === 'select' && (change as any).selected) {
      const edge = this.edges.find(e => e.id === change.id);
      if (edge) {
        this.selectedEdge = edge;
        this.edgeContextMenuPosition = this.lastMousePosition;
        this.showEdgeContextMenu = true;
      }
    } else if (this.selectedEdge?.id === change.id) {
      this.closeEdgeContextMenu();
    }
  }

  /**
   * Loads up the node suggestions sub menu
   *
   * @param selection
   */
  onSuggestionSelected(selection: LogicOperatorMeta) {
    this.suggestionSelected.emit({ nodeType: NodeType.LOGIC, operator: selection });
    this.closeNodeContextMenu();
  }

  /**
   * Moves the canvas view to the given node
   * @param nodeId
   */
  centerOnNode(nodeId: number) {
    const node = this.nodes.find(n => n.id === nodeId.toString());
    if (node && this.vflowInstance && this.canvasContainer) {
      const canvasElement = this.canvasContainer.nativeElement;
      const canvasWidth = canvasElement.clientWidth;
      const canvasHeight = canvasElement.clientHeight;

      const targetX = node.point.x - canvasWidth / 2 + (node.width ?? this.getDefaultNodeSize(node.data.nodeType).width) / 2;
      const targetY = node.point.y - canvasHeight / 2 + (node.height ?? this.getDefaultNodeSize(node.data.nodeType).height) * 2;

      this.vflowInstance.panTo({ x: -targetX, y: -targetY });
    }
  }
  //#endregion

  //#region Element Edit
  /**
   * Updates the the currently selected node's name with the new value
   *
   * @param newName
   */
  onNodeNameSaved(newName: string) {
    if (this.nodeBeingEdited) {
      const index = this.nodes.findIndex(n => n.id === this.nodeBeingEdited!.id);
      if (index !== -1) {
        // Update intern node data
        const updatedNode = {
          ...this.nodes[index],
          data: {
            ...this.nodes[index].data,
            name: newName
          }
        };

        // Re-insert the node into the nodes array to rerender it correctly
        this.nodes = [
          ...this.nodes.slice(0, index),
          updatedNode,
          ...this.nodes.slice(index + 1)
        ];
        this.hasUnsavedChanges = true;
      }
      this.editNodeNameModalOpen = false;
      this.nodeBeingEdited = null;
      this.closeNodeContextMenu();
    }
  }

  /**
   * Updates the the currently selected node's JSON path with the new value
   *
   * @param nodeData - Object containing jsonPath and outputType
   */
  onJsonPathSaved(nodeData: { jsonPath: string, outputType: string }) {
    if (this.nodeBeingEdited) {
      const index = this.nodes.findIndex(n => n.id === this.nodeBeingEdited!.id);
      if (index !== -1) {
        // Update intern node data
        const updatedNode = {
          ...this.nodes[index],
          data: {
            ...this.nodes[index].data,
            jsonPath: nodeData.jsonPath,
            outputType: nodeData.outputType
          }
        };

        // Re-insert the node into the nodes array to rerender it correctly
        this.nodes = [
          ...this.nodes.slice(0, index),
          updatedNode,
          ...this.nodes.slice(index + 1)
        ];
        this.hasUnsavedChanges = true;
      }
      this.editJsonPathModalOpen = false;
      this.nodeBeingEdited = null;
      this.closeNodeContextMenu();
    }
  }

  /**
   * Checks if the new value has a different data type from the
   * current value stored in the node and requests user confirmation
   * for removing the connected nodes if needed
   *
   * @param newValue
   */
  onValueSaved(newValue: string) {
    if (this.nodeBeingEdited) {
      if (this.inferTypeFromValue(newValue) !== this.nodeBeingEdited.data.outputType) {
        this.pendingNodeValue = newValue;
        this.showRemoveEdgesModal = true;
      }
      else {
        this.updateNodeValue(newValue);
      }
    }
  }

  /**
   * Updates the the currently selected node's value with the new one
   *
   * @param newValue
   */
  updateNodeValue(newValue: string) {
    if (this.nodeBeingEdited) {
      const index = this.nodes.findIndex(n => n.id === this.nodeBeingEdited!.id);
      // Update internal node data
      const updatedNode = {
        ...this.nodes[index],
        data: {
          ...this.nodes[index].data,
          outputType: this.inferTypeFromValue(newValue),
          value: newValue
        }
      };

      // Re-input node into nodes array to rerender
      this.nodes = [
        ...this.nodes.slice(0, index),
        updatedNode,
        ...this.nodes.slice(index + 1)
      ];
      this.hasUnsavedChanges = true;

      // Remove edges if needed
      const outgoingEdges = this.edges.filter(e => e.source === updatedNode.id);
      if (this.showRemoveEdgesModal) {
        this.edges = this.edges.filter(e => !outgoingEdges.includes(e));
      }

      this.editNodeValueModalOpen = false;
      this.nodeBeingEdited = null;
      this.closeNodeContextMenu();
    }
  }

  /**
   * Toggles the settings of the config node
   *
   * @param configuration
   */
  toggleConfiguration(configuration: "MODE" | "STATUS") {
    //* The config node should always be on the index 1
    const configData = this.nodes.at(1)!.data;

    if (configuration === "MODE") {
      configData.changeDetectionMode === "AND" ? configData.changeDetectionMode = "OR" : configData.changeDetectionMode = "AND";
    } else {
      configData.changeDetectionActive = !configData.changeDetectionActive;
    }

    // Re-insert as new node to rerender it correctly
    this.nodes = [
      ...this.nodes.slice(0, 1),
      { ...this.nodes[1] },
      ...this.nodes.slice(2)
    ];
    this.hasUnsavedChanges = true;
    this.closeNodeContextMenu();
  }

  setTimeWindow(timeWindowMillis: number) {
    //* The config node should always be on the index 1
    const configNode = this.nodes.at(1)!;
    configNode.data.timeWindowMillis = timeWindowMillis;

    // Re-insert the node into the nodes array to rerender it correctly
    this.nodes = [
      ...this.nodes.slice(0, 1),
      configNode,
      ...this.nodes.slice(2)
    ];
    this.hasUnsavedChanges = true;
    this.closeNodeContextMenu();

  }
  //#endregion

  //#region Element Deletion
  /**
   * Removes the selected edge from the edges array
   *
   * @param event
   */
  deleteSelectedEdge(event: MouseEvent) {
    event.stopPropagation();
    if (this.selectedEdge) {
      this.edges = this.edges.filter(e => e.id !== this.selectedEdge!.id);
      this.hasUnsavedChanges = true;
      this.closeEdgeContextMenu();
    }
  }

  /**
   * Deletes the given node and all edges connected to it
   * @param node The node to delete
   */
  deleteNode(node: CustomVFlowNode) {
    this.nodes = this.nodes.filter(n => n.id !== node.id);
    this.edges = this.edges.filter(e => e.source !== node.id && e.target !== node.id);
    this.hasUnsavedChanges = true;

    if (this.selectedNode?.id === node.id)
      this.closeNodeContextMenu();
  }

  /**
   * Proceeds with update of the node value and the removal of all connected edges
   */
  confirmRemoveEdges() {
    this.updateNodeValue(this.pendingNodeValue!);
    this.showRemoveEdgesModal = false;
    this.pendingNodeValue = null;
  }

  /**
   * Cancels the update of the node value and the removal of all connected edges
   */
  cancelRemoveEdges() {
    this.pendingNodeValue = null;
    this.showRemoveEdgesModal = false;
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
        console.log("Received:", graph); // TODO-s DELETE

        // loads nodes
        this.nodes = graph.nodes.map(node => ({
          ...node,
          type: this.getNodeType(node.type),
          width: this.getDefaultNodeSize(node.type).width,
          height: this.getDefaultNodeSize(node.type).height,
          contextMenuItems: this.getNodeMenuItems.call(this, node as unknown as CustomVFlowNode)
        }));

        // caches the largest node ID
        this.lastNodeId = graph.nodes.length > 0
          ? Math.max(...graph.nodes.map(n => parseInt(n.id)))
          : 0;

        // loads edges
        this.edges = graph.edges.map(edge => {
          return {
            ...edge,
            type: edge.type as EdgeType,
          }
        });

        // emits backend graph errors to the page component
        this.graphErrors.emit(graph.errors ? graph.errors : []);


        //* The final node should always be the first node to be instantiated by the backend
        this.centerOnNode(0);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Loading the graph',
          detail: 'An error accurred while loading the graph. \n Please check the logs or the console.'
        });
        console.error(err);
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
        this.hasUnsavedChanges = false;
        console.log('Received graphDTO:', JSON.stringify(res, null, 2)); // TODO-s DELETE

        this.graphErrors.emit(res.errors ? res.errors : []);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Saving the graph',
          detail: 'An error accurred while saving the graph. \n Please check the logs or the console.'
        });
        console.error('Error response body:', err.error);
      }
    });
  }
  //#endregion

  //#region Helpers
  /**
 * Converts screen coordinates to VFlow coordinates
 * @param screenX
 * @param screenY
 * @returns VFlow coordinates
 */
  private screenToVFlowCoordinate(screenX: number, screenY: number): { x: number, y: number } {
    const viewport = this.vflowInstance.viewport();

    const flowX = (screenX - viewport.x) / viewport.zoom;
    const flowY = (screenY - viewport.y) / viewport.zoom;

    return { x: flowX, y: flowY };
  }

  /**
   * @param nodeType
   * @returns the default node size of given node type
   */
  getDefaultNodeSize(nodeType: NodeType): { width: number, height: number } {
    switch (nodeType) {
      case NodeType.CONSTANT:
        return { width: 220, height: 60 };
      case NodeType.PROVIDER:
        return { width: 320, height: 80 };
      case NodeType.LOGIC:
        return { width: 320, height: 60 };
      case NodeType.FINAL:
        return { width: 320, height: 60 };
      case NodeType.CONFIG:
        return { width: 220, height: 60 };
    }
  }

  /**
   * Opens up a sub menu containing suggested nodes that accept
   * the data type of the given nodes output as their input type
   *
   * @param node
   */
  openSuggestionsMenu(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    if (latestNode) {
      const outputType = latestNode.data.outputType ?? this.inferTypeFromValue(latestNode.data.value);
      this.nodesApi.getOperators().subscribe({
        next: (operators: LogicOperatorMeta[]) => {
          this.suggestions = operators.filter(o => o.inputTypes.includes(outputType));
          this.showSuggestions = true;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Loading the logic operators',
            detail: 'An error accurred while accessing the the logic operators. \n Please check the logs or the console.'
          });
          console.error(err);
        }
      })
    }
  }

  /**
   * Caches the node to be edited from the nodes array
   * and opens up the modal to edit the nodes value
   *
   * @param node
   */
  startEditingNodeValue(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    this.nodeBeingEdited = latestNode ?? node;
    this.editNodeValueModalOpen = true;
  }

  /**
   * Caches the node to be edited from the nodes array
   * and opens up the modal to edit the nodes JSON path
   *
   * @param node
   */
  startEditingJsonPath(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    this.nodeBeingEdited = latestNode ?? node;
    this.editJsonPathModalOpen = true;
  }

  /**
   * Caches the node to be edited from the nodes array
   * and opens up the modal to edit the nodes name
   *
   * @param node
   */
  startEditingNodeName(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    this.nodeBeingEdited = latestNode ?? node;
    this.editNodeNameModalOpen = true;
  }

  /**
   * Caches the current mouse position
   *
   * @param event
   */
  storeMousePosition(event: MouseEvent) {
    this.lastMousePosition = { x: event.clientX, y: event.clientY };
  }

  /**
   * Closes all modals
   */
  closeModals() {
    this.editNodeNameModalOpen = false;
    this.editJsonPathModalOpen = false;
    this.editNodeValueModalOpen = false;
  }

  /**
   * Closes the current context menu of the previously selected node
   */
  closeNodeContextMenu() {
    this.selectedNode = null;
    this.showNodeContextMenu = false;
    this.showSuggestions = false;
    this.nodeContextMenuPosition = { x: 0, y: 0 };
  }

  /**
   * Closes the current context menu of the previously selected edge
   */
  closeEdgeContextMenu() {
    this.selectedEdge = null;
    this.showEdgeContextMenu = false;
    this.edgeContextMenuPosition = { x: 0, y: 0 };
  }

  /**
   * Checks if the given to be created edge is valid,
   * that is connects two nodes with compatible data types.
   *
   * @param param0
   * @returns true if given edge is valid
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

    // Restrict config node inputs to provider nodes only
    if (targetNode?.data.nodeType === NodeType.CONFIG && sourceNode?.data.nodeType !== NodeType.PROVIDER) {
      this.showTypeIncompatibilityModal = true;
      this.typeIncompatibilityMessage = 'Config nodes only accept edges from Provider nodes.';
      this.lastAttemptedConnection = null;
      return false;
    }

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
        // Check if string is a valid DateTime in ISO 8601 format
        if (this.isValidDateTime(value)) {
          return 'DATE';
        }
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

  private isValidDateTime(value: string): boolean {
    const iso8601Pattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,3})?(Z|[+-]\d{2}:\d{2})?$/;

    if (!iso8601Pattern.test(value)) {
      return false;
    }

    try {
      const date = new Date(value);
      return !isNaN(date.getTime());
    } catch (error) {
      return false;
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
   * @param nodeType
   * @returns the code component class of a given node type
   */
  private getNodeType(nodeType: NodeType) {
    switch (nodeType) {
      case NodeType.PROVIDER: return ProviderNodeComponent;
      case NodeType.CONSTANT: return ConstantNodeComponent;
      case NodeType.LOGIC: return LogicNodeComponent;
      case NodeType.FINAL: return FinalNodeComponent;
      case NodeType.CONFIG: return ConfigNodeComponent;
      default:
        this.messageService.add({
          severity: 'error',
          summary: 'Unknown NodeType',
          detail: 'An unknown node type was attempted to be accessed. \n Please check the logs or the console.'
        });
        throw Error("Unknown NodeType");
    }
  }
  //#endregion

}
