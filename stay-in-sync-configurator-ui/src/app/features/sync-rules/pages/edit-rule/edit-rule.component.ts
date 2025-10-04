import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConstantNodeModalComponent, NodePaletteComponent, ProviderNodeModalComponent, VflowCanvasComponent } from '../../components';
import { LogicOperatorMeta, NodeType } from '../../models';
import { CommonModule } from '@angular/common';
import { TransformationRulesApiService } from '../../service';
import { ErrorPanelComponent } from '../../components/error-panel/error-panel.component';
import { ValidationError } from '../../models/interfaces/validation-error.interface';
import { RuleConfigurationComponent } from '../../components/modals/rule-configuration/rule-configuration.component';
import { Button } from 'primeng/button';
import { Toolbar } from 'primeng/toolbar';
import { MessageService } from 'primeng/api';

/**
 * The rule editor page component in which the user can view and edit a transformation rule graph
 */
@Component({
  selector: 'app-edit-rule',
  imports: [
    CommonModule,
    VflowCanvasComponent,
    NodePaletteComponent,
    ProviderNodeModalComponent,
    ConstantNodeModalComponent,
    ErrorPanelComponent,
    Button,
    Toolbar,
    RuleConfigurationComponent
  ],
  templateUrl: './edit-rule.component.html',
  styleUrl: './edit-rule.component.css'
})
export class EditRuleComponent implements OnInit {
  //#region Setup
  ruleName = 'New Rule';
  ruleId: number | undefined = undefined;
  ruleDescription = '';

  // Palette Attributes
  showMainNodePalette = false;
  nodePalettePosition = { x: 0, y: 0 };
  selectedNodeType: NodeType | null = null;
  selectedOperator: LogicOperatorMeta | null = null;

  // Modal states
  showProviderModal = false;
  showConstantModal = false;
  pendingNodePos: { x: number, y: number } | null = null;
  showRuleConfiguration = false;

  // Error Panel Attribute
  //* Needed as an intermediate attribute
  graphErrors: ValidationError[] = [];

  private documentClickListener: any;

  @ViewChild(NodePaletteComponent) nodePalette!: NodePaletteComponent;
  @ViewChild(VflowCanvasComponent) canvas!: VflowCanvasComponent;
  @ViewChild('nodePalette', { static: true }) nodePaletteRef!: ElementRef;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rulesApi: TransformationRulesApiService,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    this.ruleId = routeId ? Number(routeId) : undefined;

    if (this.ruleId) {
      this.rulesApi.getRule(this.ruleId).subscribe(rule => {
        this.ruleName = rule.name;
        this.ruleDescription = rule.description;
      });
    }

    document.addEventListener('mousedown', this.handlePageClick, true);
  }
  //#endregion

  //#region Navigation
  /**
   * Navigates back to the rules overview page
   */
  return() {
    this.router.navigate(['/sync-rules']);
  }
  //#endregion

  //#region Page Events
  /**
   * Checks the target of the mouse click and closes the node palette accordingly
   * *Not closing context menus of the vflow canvas,
   * *since that framework does not expose a way to deselect the element within
   *
   * @param event
   */
  private handlePageClick = (event: MouseEvent) => {
    if (!this.nodePaletteRef.nativeElement.contains(event.target)) {
      this.showMainNodePalette = false;
      this.showMainNodePalette = false;
      this.nodePalette.closeSubPalettes();
    }
  }

  /**
   * Moves the canvas view to the node associated with the given node ID
   *
   * @param nodeId
   */
  onErrorClicked(nodeId: number) {
    this.canvas.centerOnNode(nodeId);
  }
  //#endregion

  //#region Node Palette
  /**
   * Opens the main palette of the node selection,
   * that being the selection between provider, constant and logic node
   *
   * @param mouseEvent
   */
  openMainNodePalette() {
    this.nodePalettePosition = { x: 0, y: 0 };
    this.showMainNodePalette = true;
  }

  /**
   * Cashes the selected node's data for further use and closes the node selection palette
   *
   * @param selection
   */
  onNodeSelected(selection: { nodeType: NodeType, operator?: LogicOperatorMeta }) {
    this.selectedNodeType = selection.nodeType;
    this.selectedOperator = selection.operator || null;
    this.showMainNodePalette = false
    this.nodePalette.closeSubPalettes();

    if (this.pendingNodePos) {
      switch (this.selectedNodeType) {
        case NodeType.PROVIDER:
          this.showProviderModal = true;
          break;
        case NodeType.CONSTANT:
          this.showConstantModal = true;
          break;
        case NodeType.LOGIC:
          this.canvas.addNode(this.selectedNodeType, this.pendingNodePos, undefined, undefined, this.selectedOperator!);
          this.clearNodeSelection();
          this.pendingNodePos = null;
          break;
      }
    }
  }

  /**
   * Empties the attributes used for the node selection
   */
  clearNodeSelection() {
    this.selectedNodeType = null;
    this.selectedOperator = null;
  }
  //#endregion

  //#region Canvas Interaction
  /**
   * Places the selected node on the canvas if applicable
   * and closes the node selection palette
   *
   * @param pos
   */
  onCanvasClick(pos: { x: number, y: number }) {
    if (this.selectedNodeType)
      switch (this.selectedNodeType) {
        case NodeType.PROVIDER:
          this.pendingNodePos = pos;
          this.showProviderModal = true;
          break;
        case NodeType.CONSTANT:
          this.pendingNodePos = pos;
          this.showConstantModal = true;
          break;
        case NodeType.LOGIC:
          this.canvas.addNode(this.selectedNodeType, pos, undefined, undefined, this.selectedOperator!);
          this.clearNodeSelection();
          break;
      }

    this.showMainNodePalette = false;
    this.nodePalette.closeSubPalettes();
  }

  /**
   * Opens the node palette on the given viewport position
   *
   * @param positions contains the viewport and canvas positions of the last right click
   */
  onCanvasRightClick(positions: { viewportPos: { x: number, y: number }, canvasPos: { x: number, y: number } }) {
    this.pendingNodePos = positions.canvasPos;

    // Position the palette at the viewport location
    this.nodePalettePosition = positions.viewportPos;
    this.showMainNodePalette = true;
    this.nodePalette.closeSubPalettes();
  }
  openRuleConfiguration() {
    this.showRuleConfiguration = true;
  }
  //#endregion

  //#region Modal Events
  /**
   * Forwards the to be created provider node's JSON path to node creation
   * @param providerJsonPath
   */
  onProviderCreated(providerJsonPath: any) {
    this.canvas.addNode(NodeType.PROVIDER, this.pendingNodePos!, providerJsonPath);
    this.onModalsClosed();
  }

  /**
   * Forwards the to be created constant node's value to node creation
   * @param constantData
   */
  onConstantCreated(constantValue: any) {
    this.canvas.addNode(NodeType.CONSTANT, this.pendingNodePos!, undefined, constantValue);
    this.onModalsClosed();
  }

  /**
   * Closes all modals of this page
   */
  onModalsClosed() {
    this.showConstantModal = false;
    this.showProviderModal = false;
    this.showRuleConfiguration = false;
    this.pendingNodePos = null;
    this.clearNodeSelection();
  }

  ngOnDestroy(): void {
    if (this.documentClickListener) {
      document.removeEventListener('mousedown', this.documentClickListener, true);
    }
  }

  /**
   * Updates the current rule with the given rule configuration
   *
   * @param ruleConfiguration
   */
  onRuleConfigurationSaved(ruleConfiguration: { name: string, description: string }) {
    this.rulesApi.updateRule(this.ruleId!, ruleConfiguration).subscribe({
      next: (updatedRule) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Updated Rule Configurations',
          detail: 'Rule was successfully updated'
        });
        console.log('Rule updated successfully', updatedRule); // TODO-s DELETE

        this.ruleName = ruleConfiguration.name;
        this.ruleDescription = ruleConfiguration.description;
      },
      error: (error) => {
        console.error('Failed to update rule:', error);
      }
    });
    this.onModalsClosed();
  }
  //#endregion
}
