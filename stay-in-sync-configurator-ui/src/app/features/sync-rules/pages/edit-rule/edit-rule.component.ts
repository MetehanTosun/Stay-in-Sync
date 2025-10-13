import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild } from '@angular/core';
import { ClickOutsideDirective } from '../../directives/click-outside.directive';
import { ActivatedRoute, Router } from '@angular/router';
import { NodePaletteComponent, VflowCanvasComponent, SetConstantValueModalComponent, SetJsonPathModalComponent, SetSchemaModalComponent } from '../../components';
import { CanvasFacadeService } from '../../service/canvas/canvas-facade.service';
import { LogicOperatorMetadata, NodeType } from '../../models';
import { CommonModule } from '@angular/common';
import { TransformationRulesApiService } from '../../service';
import { ErrorPanelComponent } from '../../components/error-panel/error-panel.component';
import { ValidationError } from '../../models/interfaces/validation-error.interface';
import { SetRuleConfigurationModal } from '../../components/modals/set-rule-configuration-modal/set-rule-configuration-modal.component';
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
    SetJsonPathModalComponent,
    SetConstantValueModalComponent,
    SetSchemaModalComponent,
    ErrorPanelComponent,
    Button,
    Toolbar,
    SetRuleConfigurationModal,
    ClickOutsideDirective
  ],
  templateUrl: './edit-rule.component.html',
  styleUrls: ['./edit-rule.component.css']
})
export class EditRuleComponent implements OnInit, AfterViewInit, OnDestroy {
  // #region Fields
  ruleName = 'New Rule';
  ruleId: number | undefined = undefined;
  ruleDescription: string = '';

  // Palette Attributes
  showMainNodePalette = false;
  nodePalettePosition = { x: 0, y: 0 };
  selectedNodeType: NodeType | null = null;
  selectedOperator: LogicOperatorMetadata | null = null;

  // Node Creation
  pendingNodePos: { x: number, y: number } | null = null;
  showProviderModal: boolean = false;
  showConstantModal: boolean = false;
  showSchemaModal: boolean = false;
  showConfigModal: boolean = false;

  // Error Panel Attribute
  //* Needed as an intermediate attribute
  graphErrors: ValidationError[] = [];

  @ViewChild(NodePaletteComponent, { static: false }) nodePalette?: NodePaletteComponent;
  @ViewChild(VflowCanvasComponent, { static: false }) canvas?: VflowCanvasComponent;
  // #endregion

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rulesApi: TransformationRulesApiService,
    private messageService: MessageService,
    public canvasFacade: CanvasFacadeService
  ) { }

  //#region Lifecycle
  /**
   * Loads the rule data into the page
   */
  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    this.ruleId = routeId ? Number(routeId) : undefined;

    if (this.ruleId) {
      this.rulesApi.getRule(this.ruleId).subscribe(rule => {
        this.ruleName = rule.name;
        this.ruleDescription = rule.description || '';
      });
    }

  }

  /**
   * Registers this component's canvas to the canvas facade.
   */
  ngAfterViewInit(): void {
    if (this.canvas) {
      this.canvasFacade.register(this.canvas);
    }
  }

  /**
   * Attempts to unregister this component's canvas from the canvas facade.
   */
  ngOnDestroy(): void {
    try {
      this.canvasFacade.unregister();
    } catch (err) {
      console.warn('canvasFacade.unregister failed', err);
    }
  }
  //#endregion

  //#region Page Events
  /**
   * Navigates back to the rules overview page
   */
  goBack(): void {
    this.router.navigate(['/sync-rules']);
  }

  /**
   * Moves the canvas view to the node associated with the given node ID
   *
   * @param nodeId
   */
  onErrorClicked(nodeId: number): void {
    this.canvasFacade.centerOnNode(nodeId);
  }
  //#endregion

  //#region Node Palette
  /**
   * Opens the main palette of the node selection,
   * that being the selection between provider, constant and logic node
   *
   * @param mouseEvent
   */
  openMainNodePalette(): void {
    this.nodePalettePosition = { x: 0, y: 0 };
    this.showMainNodePalette = true;
  }

  /**
   * Closes the main node palette and any open sub-palettes.
   */
  closeNodePalette(): void {
    this.showMainNodePalette = false;
    this.nodePalette?.closeSubPalettes?.();
  }

  /**
   * Cashes the selected node's data for further use and closes the node selection palette
   *
   * @param selection
   */
  onNodeSelected(selection: { nodeType: NodeType, operator?: LogicOperatorMetadata }): void {
    this.selectedNodeType = selection.nodeType;
    this.selectedOperator = selection.operator || null;
    this.closeNodePalette();

    if (this.pendingNodePos) {
      switch (this.selectedNodeType) {
        case NodeType.PROVIDER:
          this.closeAllModals();
          this.showProviderModal = true;
          break;
        case NodeType.CONSTANT:
          this.closeAllModals();
          this.showConstantModal = true;
          break;
        case NodeType.LOGIC:
          if (this.pendingNodePos) {
            this.canvasFacade.addNode(this.selectedNodeType, this.pendingNodePos, undefined, undefined, this.selectedOperator);
          }
          this.clearNodeSelection();
          this.pendingNodePos = null;
          break;
        case NodeType.SCHEMA:
          this.closeAllModals();
          this.showSchemaModal = true;
          break;
      }
    }
  }

  /**
   * Empties the attributes used for the node selection
   */
  clearNodeSelection(): void {
    this.selectedNodeType = null;
    this.selectedOperator = null;
  }
  //#endregion

  //#region Canvas Interaction
  /**
   * Places the selected node on the canvas if applicable
   * and closes the node selection palette
   *
   * @param pos Page coordinates where the user clicked
   */
  onCanvasClick(pos: { x: number, y: number }): void {
    if (this.selectedNodeType)
      switch (this.selectedNodeType) {
        case NodeType.PROVIDER:
          this.pendingNodePos = pos;
          this.closeAllModals();
          this.showProviderModal = true;
          break;
        case NodeType.CONSTANT:
          this.pendingNodePos = pos;
          this.closeAllModals();
          this.showConstantModal = true;
          break;
        case NodeType.SCHEMA:
          this.pendingNodePos = pos;
          this.closeAllModals();
          this.showSchemaModal = true;
          break;
        case NodeType.LOGIC:
          this.canvasFacade.addNode(this.selectedNodeType, pos, undefined, undefined, this.selectedOperator || undefined);
          this.clearNodeSelection();
          break;
      }

    this.closeNodePalette();
  }

  /**
   * Opens the node palette on the given viewport position
   *
   * @param positions contains the viewport and canvas positions of the last right click
   */
  onCanvasRightClick(positions: { viewportPos: { x: number, y: number }, canvasPos: { x: number, y: number } }): void {
    this.pendingNodePos = positions.canvasPos;

    // Position the palette at the viewport location
    this.nodePalettePosition = positions.viewportPos;
    this.showMainNodePalette = true;
    this.nodePalette?.closeSubPalettes?.();
  }

  /**
   * Opens the rule configuration modal
   */
  openRuleConfiguration(): void {
    this.closeAllModals();
    this.showConfigModal = true;
  }
  //#endregion


  //#region Modal Events
  /**
   * Forwards the to be created provider node's JSON path to node creation
   * @param providerJsonPath
   */
  onProviderCreated(providerData: { jsonPath: string; outputType: string }): void {
    if (this.pendingNodePos) {
      this.canvasFacade.addNode(NodeType.PROVIDER, this.pendingNodePos, providerData);
    }
    this.onModalsClosed();
  }

  /**
   * Forwards the to be created constant node's value to node creation
   * @param constantData
   */
  onConstantCreated(constantValue: any): void {
    if (this.pendingNodePos) {
      this.canvasFacade.addNode(NodeType.CONSTANT, this.pendingNodePos, undefined, constantValue);
    }
    this.onModalsClosed();
  }

  /**
   * Forwards the to be created schema node's schema string to node creation
   */
  onSchemaCreated(value: string): void {
    if (this.pendingNodePos) {
      // Pass schema string to the canvas via the constantValue parameter
      this.canvasFacade.addNode(NodeType.SCHEMA, this.pendingNodePos, undefined, value);
    }
    this.onModalsClosed();
  }

  /**
   * Closes all modals of this page and reverts node creation process
   */
  onModalsClosed(): void {
    this.pendingNodePos = null;
    this.closeAllModals();
    this.clearNodeSelection();
  }

  /**
   * loses all modals of this page
   */
  private closeAllModals(): void {
    this.showProviderModal = false;
    this.showConstantModal = false;
    this.showConfigModal = false;
  }

  /**
   * Updates the current rule with the given rule configuration
   *
   * @param ruleConfiguration
   */
  onRuleConfigurationSaved(ruleConfiguration: { name: string, description: string }): void {
    if (!this.ruleId) {
      this.messageService.add({ severity: 'error', summary: 'Update failed', detail: 'Rule id missing' });
      return;
    }

    this.rulesApi.updateRule(this.ruleId, ruleConfiguration).subscribe({
      next: (updatedRule) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Updated Rule Configurations',
          detail: 'Rule was successfully updated'
        });

        this.ruleName = updatedRule.name;
        this.ruleDescription = updatedRule.description || '';
      },
      error: (error) => {
        console.error('Failed to update rule:', error);
        this.messageService.add({ severity: 'error', summary: 'Update failed', detail: 'Could not update rule' });
      }
    });
    this.onModalsClosed();
  }
  //#endregion
}
