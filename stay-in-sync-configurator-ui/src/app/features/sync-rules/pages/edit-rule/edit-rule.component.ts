import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConstantNodeModalComponent, NodePaletteComponent, ProviderNodeModalComponent, VflowCanvasComponent } from '../../components';
import { LogicOperatorMeta, NodeType } from '../../models';
import { CommonModule } from '@angular/common';
import { TransformationRulesApiService } from '../../service';
import { ErrorPanelComponent } from '../../components/error-panel/error-panel.component';
import { ValidationError } from '../../models/interfaces/validation-error.interface';

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
    ErrorPanelComponent
  ],
  templateUrl: './edit-rule.component.html',
  styleUrl: './edit-rule.component.css'
})
export class EditRuleComponent implements OnInit {
  //#region Setup
  ruleName = 'New Rule';
  ruleId: number | undefined = undefined;

  // Palette Attributes
  showMainNodePalette = false;
  selectedNodeType: NodeType | null = null;
  selectedOperator: LogicOperatorMeta | null = null;

  // Modal states
  showProviderModal = false;
  showConstantModal = false;
  pendingNodePos: { x: number, y: number } | null = null;

  // Error Panel Attribute
  //* Needed as an intermediate attribute
  graphErrors: ValidationError[] = [];

  private documentClickListener: any;

  @ViewChild(NodePaletteComponent) nodePalette!: NodePaletteComponent;
  @ViewChild(VflowCanvasComponent) canvas!: VflowCanvasComponent;
  @ViewChild('nodePalette', { static: true }) nodePaletteRef!: ElementRef;

  constructor(private route: ActivatedRoute, private router: Router, private rulesApi: TransformationRulesApiService) { }

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    this.ruleId = routeId ? Number(routeId) : undefined;

    if (this.ruleId) {
      this.rulesApi.getRule(this.ruleId).subscribe(rule => {
        this.ruleName = rule.name;
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
      this.nodePalette.closeSubPalettes();
    }
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
    this.pendingNodePos = null;
    this.clearNodeSelection();
  }

  ngOnDestroy(): void {
    if (this.documentClickListener) {
      document.removeEventListener('mousedown', this.documentClickListener, true);
    }
  }
  //#endregion
}
