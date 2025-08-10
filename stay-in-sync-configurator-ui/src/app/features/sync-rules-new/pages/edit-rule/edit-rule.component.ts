import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { OperatorNodesApiService } from '../../service';
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
export class EditRuleComponent implements OnInit {
  ruleName = 'New Rule';
  operators: LogicOperator[] = [];
  operatorsGrouped: Map<string, LogicOperator[]> = new Map<string, LogicOperator[]>();

  // Palette States
  showMainNodePalette = false;
  selectedNodeType: NodeType | null = null;

  @ViewChild(NodePaletteComponent) nodePalette!: NodePaletteComponent;
  @ViewChild(VflowCanvasComponent) canvas!: VflowCanvasComponent;

  constructor(
    private router: Router,
    private nodesApi: OperatorNodesApiService
  ) { }

  ngOnInit() {
    this.loadOperators();
  }

  /**
   * Loads all operator nodes from the backend
   */
  loadOperators() { // Todo-s check if redundant
    this.nodesApi.getOperators().subscribe({
      next: (operators: LogicOperator[]) => {
        this.operators = operators;
      },
      error: (err) => {
        alert(err.error?.message || err.message);
        console.log(err); // TODO-s err
      },
    })
  }

  /**
   * Loads all operator nodes in a grouped format from the backend
   */
  loadGroupedOperators() { // Todo-s check if redundant
    this.nodesApi.getGroupedOperators().subscribe({
      next: (operatorsGrouped: Map<string, LogicOperator[]>) => {
        this.operatorsGrouped = operatorsGrouped;
      },
      error: (err) => {
        alert(err.error?.message || err.message);
        console.log(err); // TODO-s err
      },
    })
  }

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

  closeNodePalette() {
    this.showMainNodePalette = false;
    // TODO-s close the entire Palette
  }

  onNodeSelected(nodeType: NodeType) {
    this.selectedNodeType = nodeType;
    this.closeNodePalette();
    console.log('Selected Node Type: ', nodeType) // TODO-s DELETE
  }

  clearNodeSelection() {
    this.selectedNodeType = null;
  }

  onCanvasClick(pos: { x: number, y: number }) {
    if (this.selectedNodeType) {
      this.canvas.addNode(this.selectedNodeType, pos);
      this.clearNodeSelection();
      console.log("Node placed at: ", pos) // TODO-s DELETE
    }
  }
}
