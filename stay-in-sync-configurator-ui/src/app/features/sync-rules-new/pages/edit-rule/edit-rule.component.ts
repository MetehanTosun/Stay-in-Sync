import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { VflowCanvasComponent } from '../../components';
import { LogicOperator, NodeType } from '../../models';
import { NodePaletteComponent } from '../../components/node-palette/node-palette.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-edit-rule',
  imports: [
    CommonModule,
    VflowCanvasComponent,
    NodePaletteComponent
  ],
  templateUrl: './edit-rule.component.html',
  styleUrl: './edit-rule.component.css'
})
export class EditRuleComponent {
  ruleName = 'New Rule';

  // Palette Attributes
  showMainNodePalette = false;
  selectedNodeType: NodeType | null = null;
  selectedOperator: LogicOperator | null = null;

  @ViewChild(NodePaletteComponent) nodePalette!: NodePaletteComponent;
  @ViewChild(VflowCanvasComponent) canvas!: VflowCanvasComponent;

  constructor(private router: Router) { }

  /**
   * Navigates back to the rules overview page
   */
  return() {
    this.router.navigate(['/sync-rules-new']); // TODO-s update
  }

  openMainNodePalette(event: MouseEvent) {
    this.showMainNodePalette = true;
    // TODO-s mouse position
  }

  onNodeSelected(selection: { nodeType: NodeType, operator?: LogicOperator }) {
    this.selectedNodeType = selection.nodeType;
    this.selectedOperator = selection.operator || null;
    this.nodePalette.closeAllMenus();
    console.log('Selected: ', selection) // TODO-s DELETE
  }

  clearNodeSelection() {
    this.selectedNodeType = null;
    this.selectedOperator = null;
  }

  onCanvasClick(pos: { x: number, y: number }) {
    if (this.selectedNodeType) {
      this.canvas.addNode(this.selectedNodeType, pos, this.selectedOperator || undefined);
      this.clearNodeSelection();
      console.log("Node placed at: ", pos) // TODO-s DELETE
    }
  }
}
