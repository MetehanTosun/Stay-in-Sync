import { Component, ElementRef, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Connection, Edge, EdgeChange, NodeChange, Vflow, VflowComponent } from 'ngx-vflow';
import { GraphAPIService, OperatorNodesApiService } from '../../service';
import { ConfigNodeData, ConstantNodeData, CustomVFlowNode, LogicOperatorMetadata, NodeMenuItem, NodeType, ProviderNodeData, SchemaNodeData, VFlowGraphDTO } from '../../models';
import { getDefaultNodeSize, inferTypeFromValue, getExpectedInputType, getNodeType, calculateVFlowCoordinates, hasProp, hasPropOfType, getPropIfExists, buildNodeData, calculateNodeCenter, createNode } from './vflow-canvas.utils';
import { FinalNodeComponent, SetConstantValueModalComponent, SetJsonPathModalComponent, SetSchemaModalComponent, SetNodeNameModalComponent, ConfigNodeComponent } from '..';
import { CommonModule } from '@angular/common';
import { ValidationError } from '../../models';
import { MessageService } from 'primeng/api';
import { ClickOutsideDirective } from '../../directives/click-outside.directive';

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
    ClickOutsideDirective,
    SetJsonPathModalComponent,
    SetConstantValueModalComponent,
    SetSchemaModalComponent
  ],
  templateUrl: './vflow-canvas.component.html',
  styleUrls: ['./vflow-canvas.component.css']
})
export class VflowCanvasComponent implements OnInit {
  //#region Fields
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
  //* Don't merge positions

  // Modal Status
  editNodeNameModalOpen = false;
  editJsonPathModalOpen = false;
  editNodeValueModalOpen = false;
  editSchemaModalOpen = false;
  nodeBeingEdited: CustomVFlowNode | null = null;
  showRemoveEdgesModal: boolean = false;

  // Autocomplete Feature
  suggestions: LogicOperatorMetadata[] = []
  showSuggestions = false;

  // Pending Stuff
  pendingNodeValue: string | null = null;

  @Output() canvasClick = new EventEmitter<{ x: number, y: number }>();
  @Output() canvasRightClick = new EventEmitter<{
    viewportPos: { x: number, y: number },
    canvasPos: { x: number, y: number }
  }>();
  @Output() suggestionSelected = new EventEmitter<{ nodeType: NodeType, operator: LogicOperatorMetadata }>();
  @Output() graphErrors = new EventEmitter<ValidationError[]>();

  @ViewChild(VflowComponent) vflowInstance!: VflowComponent;
  @ViewChild('canvasContainer') canvasContainer!: ElementRef<HTMLDivElement>;
  //#endregion

  constructor(
    private route: ActivatedRoute,
    private graphApi: GraphAPIService,
    private nodesApi: OperatorNodesApiService,
    private messageService: MessageService
  ) { }

  //#region Lifecylce
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

  //#region Canvas Interaction
  /**
   * Caches the current mouse position
   */
  storeMousePosition(event: MouseEvent) {
    this.lastMousePosition = { x: event.clientX, y: event.clientY };
  }

  /**
   * Calculates and emits the mouse position on the vflow canvas
   * @param mouseEvent
   */
  onCanvasClick(mouseEvent: MouseEvent) {
    this.canvasClick.emit(calculateVFlowCoordinates(mouseEvent, this.vflowInstance.viewport()));
  }

  /**
   * Calculates and emits the mouse position (in viewport and canvas coordinates) for right-click
   * @param mouseEvent
   */
  onCanvasRightClick(mouseEvent: MouseEvent) {
    mouseEvent.preventDefault();

    const canvasPos = calculateVFlowCoordinates(mouseEvent, this.vflowInstance.viewport());
    const viewportPos = {
      x: mouseEvent.clientX,
      y: mouseEvent.clientY
    };

    this.canvasRightClick.emit({ viewportPos, canvasPos });
  }

  /**
   * Handler for clicks outside relevant canvas elements.
   * Closes menus if an outside click is registered
   */
  onOutsideCanvas(_event: MouseEvent) {
    // If the click happened inside the context menu, suggestions menu or modal, do not treat it as outside
    const target = (_event.target as HTMLElement) || null;
    const clickedInsideMenu = target && (
      target.closest('.context-menu')
      || target.closest('.suggestions-menu')
      || target.closest('.p-dialog')
    );
    if (clickedInsideMenu) return;

    if (this.showNodeContextMenu && this.selectedNode) {
      this.reinsertNode(this.selectedNode)
    }

    this.closeNodeContextMenu();
    this.closeEdgeContextMenu();
  }

  /**
   * Takes the positional change of a node and updates its point property
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
  //#endregion

  //#region Template Helpers
  /**
   * Used by the template to decide whether to render a clickable label entry / item of the contextmenu.
   *
   * @returns true when `item.label` is a string
   */
  hasLabel(item: any): item is { label: string; action?: () => void } {
    return hasPropOfType(item, 'label', 'string');
  }

  /**
   * Getter for a menu item's label.
   * Returns an empty string when the property is undefined/null
   */
  getLabel(item: any): string {
    return getPropIfExists(item, 'label', '');
  }

  hasSubmenu(item: any): item is { submenu: NodeMenuItem[] } {
    /**
     * Type guard: detects an embedded submenu
     */
    return hasPropOfType(item, 'submenu', 'array');
  }

  /**
   * Getter for a submenu.
   * Returns an empty array when submenu is not present
   */
  getSubmenu(item: any): NodeMenuItem[] {
    return getPropIfExists(item, 'submenu', [] as NodeMenuItem[]);
  }

  /**
   * Type guard: true if a node's data contains a `jsonPath` string (is provider node)
   */
  hasJsonPath(node?: CustomVFlowNode | null): node is CustomVFlowNode & { data: { jsonPath: string } } {
    return !!node && hasPropOfType(node.data, 'jsonPath', 'string');
  }

  /**
   * Returns the node's jsonPath or an empty string when missing
   */
  getNodeJsonPath(node?: CustomVFlowNode | null): string {
    return getPropIfExists(node?.data, 'jsonPath', '');
  }

  /**
   * Returns the node's display name or an empty string when missing
   */
  getNodeName(node?: CustomVFlowNode | null): string {
    return getPropIfExists(node?.data, 'name', '');
  }

  /**
   * Type guard: true when the node's data contains a `value` property (is constant or schema node)
   */
  hasValue(node?: CustomVFlowNode | null): node is CustomVFlowNode & { data: { value: unknown } } {
    return !!node && !!node.data && hasProp(node.data, 'value');
  }

  /**
   * Returns the node's value or `null` when absent
   */
  getNodeValue(node?: CustomVFlowNode | null): any {
    return getPropIfExists(node?.data, 'value', null as any);
  }

  /**
   * Predicate: determines whether this node is a schema node
   */
  isSchemaNode(node?: CustomVFlowNode | null): node is CustomVFlowNode & { data: { value: string } } {
    return !!node && getPropIfExists(node.data, 'outputType', null) === 'JSON';
  }

  /**
   * Returns the schema string stored on a schema node or an empty string
   * if the node is not a schema node
   */
  getNodeSchema(node?: CustomVFlowNode | null): string {
    return this.isSchemaNode(node) ? (node!.data as any).value : '';
  }
  //#endregion

  //#region REST
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
          type: getNodeType(node.type),
          width: getDefaultNodeSize(node.type).width,
          height: getDefaultNodeSize(node.type).height,
          contextMenuItems: this.getNodeMenuItems.call(this, node as unknown as CustomVFlowNode)
        }));

        // caches the largest node ID
        this.lastNodeId = parseInt(this.nodes[this.nodes.length - 1].id);

        this.edges = graph.edges;

        this.graphErrors.emit(graph.errors ? graph.errors : []);
        const finalNodeIndex = this.nodes.findIndex(n => n.type === FinalNodeComponent)
        this.centerOnNode(finalNodeIndex);
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
      edges: this.edges
    }

    this.graphApi.updateGraph(this.ruleId!, graphDTO).subscribe({
      next: (res) => {
        this.hasUnsavedChanges = false;
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

  //#region Element Selection
  /**
   * Opens the context menu of the selected node
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
   * Moves the canvas view to the given node
   */
  centerOnNode(nodeId: number) {
    const node = this.nodes.find(n => n.id === nodeId.toString());
    if (node && this.vflowInstance && this.canvasContainer) {
      const canvasElement = this.canvasContainer.nativeElement;
      const canvasWidth = canvasElement.clientWidth;
      const canvasHeight = canvasElement.clientHeight;

      const targetX = node.point.x - canvasWidth / 2 + (node.width ?? getDefaultNodeSize(node.data.nodeType).width) / 2;
      const targetY = node.point.y - canvasHeight / 2 + (node.height ?? getDefaultNodeSize(node.data.nodeType).height) / 2;

      this.vflowInstance.panTo({ x: -targetX, y: -targetY });
    }
  }

  /**
   * Opens the edge context menu for an edge
   */
  onEdgeSelect(changes: EdgeChange[]) {
    const change = changes.pop()!;
    if (change.type === 'select' && (change as any).selected) {
      const edge = this.edges.find(e => e.id === change.id);
      const configNodeIndex = this.nodes.findIndex(n => n.type === ConfigNodeComponent)
      if (edge && edge.source != configNodeIndex.toString()) {
        this.selectedEdge = edge;
        this.edgeContextMenuPosition = this.lastMousePosition;
        this.showEdgeContextMenu = true;
      }
    } else if (this.selectedEdge?.id === change.id) {
      this.closeEdgeContextMenu();
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
   * @param providerData Optional: The JSON path of the new (provider) node
   * @param constantValue Optional: The value of the new (constant/schema) node
   * @param operatorData Optional: The operator of the new (logic) node
   */
  addNode(
    nodeType: NodeType,
    pos: { x: number, y: number },
    providerData?: { jsonPath: string, outputType: string },
    constantValue?: any,
    operatorData?: LogicOperatorMetadata,
  ) {
    const nodeData = buildNodeData(nodeType, { providerData, constantValue, operatorData }, this.messageService);
    if (!nodeData) return;

    const nodeCenter = calculateNodeCenter(nodeType, pos);

    const newNode = createNode(nodeType, (++this.lastNodeId).toString(), nodeCenter, nodeData);
    newNode.contextMenuItems = this.getNodeMenuItems(newNode);
    this.nodes = [...this.nodes, newNode];
    this.hasUnsavedChanges = true;
  }

  /**
   * Creates and adds a new edge to the vflow canvas
   * * This does not persist the edge in the database
   *
   * @param param0 Connection object emmitted by vflow
   */
  addEdge({ source, target, targetHandle }: Connection) {
    if (!this.validateEdge({ source, target, targetHandle })) return;

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

  //#region Node Name Edit
  /**
   * Caches the node to be edited from the nodes array
   * and opens the modal to edit the nodes name
   */
  startEditingNodeName(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    this.nodeBeingEdited = latestNode ?? node;
    this.editNodeNameModalOpen = true;
  }

  /**
   * Updates the the currently selected node's name
   */
  onNodeNameSaved(newName: string) {
    if (this.nodeBeingEdited) {
      const nodeData = this.nodeBeingEdited.data;
      nodeData.name = newName;

      this.reinsertNode(this.nodeBeingEdited);

      this.editNodeNameModalOpen = false;
      this.nodeBeingEdited = null;
      this.closeNodeContextMenu();
    }
  }
  //#endregion

  //#region Constant Value Edit
  /**
   * Caches the node to be edited from the nodes array
   * and opens the modal to edit the nodes value
   */
  startEditingNodeValue(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    this.nodeBeingEdited = latestNode ?? node;
    this.editNodeValueModalOpen = true;
  }

  /**
   * Handler for when constant node value update save is triggered.
   * Checks if the new value has a different data type from the
   * current value stored in the node and requests user confirmation
   * for removing the connected nodes if needed
   */
  onValueSaved(newValue: string) {
    if (this.nodeBeingEdited && "outputType" in this.nodeBeingEdited.data) { // Cannot be Final Node
      if (inferTypeFromValue(newValue) !== this.nodeBeingEdited.data.outputType) {
        this.pendingNodeValue = newValue;
        this.showRemoveEdgesModal = true;
      }
      else {
        this.updateNodeValue(newValue);
      }
    }
  }

  /**
   * Updates the the currently selected constant node's value
   */
  updateNodeValue(newValue: string) {
    if (this.nodeBeingEdited) {
      const nodeData = this.nodeBeingEdited.data as ConstantNodeData;
      nodeData.outputType = inferTypeFromValue(newValue);
      nodeData.value = newValue;

      // Remove edges if data type changed
      const outgoingEdges = this.edges.filter(e => e.source === this.nodeBeingEdited?.id);
      if (this.showRemoveEdgesModal) {
        this.edges = this.edges.filter(e => !outgoingEdges.includes(e));
      }

      this.reinsertNode(this.nodeBeingEdited);

      this.editNodeValueModalOpen = false;
      this.nodeBeingEdited = null;
      this.closeNodeContextMenu();
    }
  }
  //#endregion

  //#region Json Path Edit
  /**
   * Caches the node to be edited from the nodes array
   * and opens the modal to edit the nodes JSON path
   */
  startEditingJsonPath(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    this.nodeBeingEdited = latestNode ?? node;
    this.editJsonPathModalOpen = true;
  }

  /**
   * Updates the the currently selected node's JSON path with the new value
   *
   * @param nodeData - Object containing jsonPath and outputType
   */
  onJsonPathSaved(nodeData: { jsonPath: string, outputType: string }) {
    if (this.nodeBeingEdited) {
      const oldNodeData = this.nodeBeingEdited.data as ProviderNodeData;
      oldNodeData.jsonPath = nodeData.jsonPath;
      oldNodeData.outputType = nodeData.outputType;

      this.reinsertNode(this.nodeBeingEdited)
    }
    this.editJsonPathModalOpen = false;
    this.nodeBeingEdited = null;
    this.closeNodeContextMenu();

  }
  //#endregion

  //#region Config Node Edit
  /**
   * Toggles the settings of the config node
   *
   * @param configuration sets the config node setting to be toggled
   */
  toggleConfiguration(configuration: "MODE" | "STATUS") {
    const configNode = this.nodes.find(n => n.data.nodeType === NodeType.CONFIG)!;
    const configData = configNode.data as ConfigNodeData;

    if (configuration === "MODE") {
      configData.changeDetectionMode = configData.changeDetectionMode === "AND" ? "OR" : "AND";
    } else {
      configData.changeDetectionActive = !configData.changeDetectionActive;
    }

    this.reinsertNode(configNode);
    this.closeNodeContextMenu();
  }

  /**
   * Sets the time window of the config node
   * @param timeWindowMillis Time window in milliseconds
   */
  setTimeWindow(timeWindowMillis: number) {
    const configNode = this.nodes.find(n => n.data.nodeType === NodeType.CONFIG)!;
    (configNode.data as ConfigNodeData).timeWindowMillis = timeWindowMillis;

    this.reinsertNode(configNode);
    this.closeNodeContextMenu();
  }
  //#endregion

  //#region Schema Node Edit
  /**
   * Caches the node to be edited from the nodes array
   * and opens the schema edit modal for the given schema node
   */
  startEditingSchema(node: CustomVFlowNode) {
    const latestNode = this.nodes.find(n => n.id === node.id);
    this.nodeBeingEdited = latestNode ?? node;
    this.editSchemaModalOpen = true;
  }

  /**
   * Handler when schema node save is triggered
   */
  onSchemaSaved(schema: string) {
    if (this.nodeBeingEdited) {
      const nodeData = this.nodeBeingEdited.data as SchemaNodeData;
      nodeData.value = schema;

      this.reinsertNode(this.nodeBeingEdited);

      this.editSchemaModalOpen = false;
      this.nodeBeingEdited = null;
      this.closeNodeContextMenu();
    }
  }
  //#endregion

  //#region Node Deletion
  /**
   * Deletes the given node and all edges connected to it
   */
  deleteNode(node: CustomVFlowNode) {
    this.nodes = this.nodes.filter(n => n.id !== node.id);
    this.edges = this.edges.filter(e => e.source !== node.id && e.target !== node.id);
    this.hasUnsavedChanges = true;

    if (this.selectedNode?.id === node.id) this.closeNodeContextMenu();
  }
  //#endregion

  //#region Edge Deletion
  /**
   * Removes the selected edge from the edges array
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

  //#region Menu
  /**
   * Opens up a sub menu containing suggested nodes that accept
   * the data type of the given nodes output as their input type
   */
  openSuggestionsMenu(node: CustomVFlowNode) {
    const outputType = (node.data as { outputType?: string }).outputType;
    if (!outputType) return;
        console.log(this.nodeContextMenuPosition)
    this.nodesApi.getOperators().subscribe({
      next: (operators: LogicOperatorMetadata[]) => {
        console.log(this.nodeContextMenuPosition)
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

  /**
   * Executes the axtion associated with the item from the suggestions menu
   */
  runAction(item: any) {
    if (this.hasLabel(item) && typeof item.action === 'function') {
      try {
        item.action();
      } catch (e) {
        console.error('Error running menu action', e);
      }
    }
  }

  /**
   * Loads up the node suggestions sub menu
   */
  onSuggestionSelected(selection: LogicOperatorMetadata) {
    this.suggestionSelected.emit({ nodeType: NodeType.LOGIC, operator: selection });
    this.closeNodeContextMenu();
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
  //#endregion

  //#region Modal
  /**
   * Closes the modal that displays the data type incompatibility error of a new drawn edge
   */
  closeTypeIncompatibilityModal() {
    this.showTypeIncompatibilityModal = false;
    this.typeIncompatibilityMessage = '';
    this.lastAttemptedConnection = null;
  }

  /**
   * Closes all modals
   */
  closeModals() {
    this.editNodeNameModalOpen = false;
    this.editJsonPathModalOpen = false;
    this.editNodeValueModalOpen = false;
    this.editSchemaModalOpen = false;
  }
  //#endregion

  //#region Helpers
  /**
   * Checks if the given to be created edge is valid,
   * that is connects two nodes with compatible data types.
   *
   * @param param0 Partial vflow edge object
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

    let sourceType = (sourceNode?.data as { outputType: string }).outputType ?? 'ANY';

    const targetType = getExpectedInputType(targetNode!, targetHandle);

    if (sourceType === 'ANY' || targetType === 'ANY') return true;
    if (sourceType === targetType) return true;

    this.showTypeIncompatibilityModal = true;
    this.typeIncompatibilityMessage = `Type mismatch: cannot connect ${sourceType} to ${targetType}`;
    this.lastAttemptedConnection = { sourceType, targetType };
    return false;
  }

  /**
   * Reinserts the given node into the given nodes array.
   * *Needed for rerendering a vflow graph after changing node data
   */
  private reinsertNode(node: CustomVFlowNode): void {
    const index = this.nodes.findIndex(n => n === node);
    const nodeCopy = { ...node }

    if (index !== -1) {
      this.nodes = [
        ...this.nodes.slice(0, index),
        nodeCopy,
        ...this.nodes.slice(index + 1)
      ];
      this.hasUnsavedChanges = true;
    }
  }

  /**
   * Returns the context menu of the given node
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
          { label: 'Suggestions', action: () => this.openSuggestionsMenu(node) },
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
      case NodeType.SCHEMA:
        return [
          { label: 'Edit Schema', action: () => this.startEditingSchema(node) },
          { label: 'Suggestions', action: () => this.openSuggestionsMenu(node) },
          { label: 'Delete Node', action: () => this.deleteNode(node) }
        ];
    }
  }
  //#endregion
}
