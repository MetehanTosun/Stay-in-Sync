import { Component, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ConstantNodeModalComponent, NodePaletteComponent, ProviderNodeModalComponent, VflowCanvasComponent } from '../../components';
import { LogicOperatorMeta, NodeType } from '../../models';
import { CommonModule } from '@angular/common';

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
    ConstantNodeModalComponent
  ],
  templateUrl: './edit-rule.component.html',
  styleUrl: './edit-rule.component.css'
})
export class EditRuleComponent {
  //#region Setup
  ruleName = 'New Rule';

  // Palette Attributes
  showMainNodePalette = false;
  selectedNodeType: NodeType | null = null;
  selectedOperator: LogicOperatorMeta | null = null;

  // Modal states
  showProviderModal = false;
  showConstantModal = false;
  pendingNodePos: { x: number, y: number } | null = null;

  @ViewChild(NodePaletteComponent) nodePalette!: NodePaletteComponent;
  @ViewChild(VflowCanvasComponent) canvas!: VflowCanvasComponent;

  constructor(private router: Router) { }
  //#endregion

  //#region Navigation
  /**
   * Navigates back to the rules overview page
   */
  return() {
    this.router.navigate(['/sync-rules-new']); // TODO-s update
  }
  //#endregion

  //#region Node Palette
  /**
   * Opens the main palette of the node selection,
   * that being the selection between provider, constant and logic node
   *
   * @param mouseEvent
   */
  openMainNodePalette(mouseEvent: MouseEvent) {
    this.showMainNodePalette = true;
    // TODO-s mouse position
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

    this.showMainNodePalette = false
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
  //#endregion
}
