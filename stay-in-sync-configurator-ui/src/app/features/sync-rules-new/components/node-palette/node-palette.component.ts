import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { LogicOperator, NodeType } from '../../models';
import { OperatorNodesApiService } from '../../service';

@Component({
  selector: 'app-node-palette',
  imports: [CommonModule],
  templateUrl: './node-palette.component.html',
  styleUrl: './node-palette.component.css'
})
export class NodePaletteComponent implements OnInit {
  @Input() showMainNodePalette = false;
  @Output() exit = new EventEmitter<void>();
  @Output() nodeSelected = new EventEmitter<{ nodeType: NodeType, operator?: LogicOperator }>();

  operatorsGrouped: Map<string, LogicOperator[]> = new Map<string, LogicOperator[]>();

  // Palette Status
  showLogicGroups = false;
  selectedLogicGroup: string | null = null;
  showLogicGroupOperators = false;

  constructor(private nodesApi: OperatorNodesApiService) { }

  ngOnInit(): void {
    this.loadGroupedOperators();
  }

  /**
   * Loads all operator nodes in a grouped format from the backend
   */
  loadGroupedOperators() {
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

  selectProviderNode() {
    this.nodeSelected.emit({ nodeType: NodeType.PROVIDER });
    this.exit.emit();
  }

  selectLogicNodePalette() {
    this.showLogicGroups = true;
  }

  selectConstantNode() {
    this.nodeSelected.emit({ nodeType: NodeType.CONSTANT });
    this.exit.emit();
  }

  getLogicGroups(): string[] {
    return Array.from(this.operatorsGrouped.keys());
  }

  closeAllMenus() {
    this.showMainNodePalette = false;
    this.showLogicGroups = false;
    this.showLogicGroupOperators = false;
    this.selectedLogicGroup = null;
  }

  selectLogicGroup(groupName: string) {
    this.selectedLogicGroup = groupName;
    console.log('Selected logic group:', groupName); // TODO-s
  }

}
