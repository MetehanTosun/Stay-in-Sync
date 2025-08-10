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
  @Output() nodeSelected = new EventEmitter<{ nodeType: NodeType, operator?: LogicOperator }>();

  operatorsGrouped: Map<string, LogicOperator[]> = new Map<string, LogicOperator[]>();

  // Palette Status
  showLogicGroups = false;
  selectedLogicGroup: string | null = null;
  showGroupOperators = false;

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
  }

  selectLogicNodePalette() {
    this.showLogicGroups = true;
  }

  selectConstantNode() {
    this.nodeSelected.emit({ nodeType: NodeType.CONSTANT });
  }

  getLogicGroups(): string[] {
    return Array.from(this.operatorsGrouped.keys());
  }

  closeAllMenus() {
    this.showLogicGroups = false;
    this.showGroupOperators = false;
    this.selectedLogicGroup = null;
  }

  selectLogicGroup(groupName: string) {
    this.selectedLogicGroup = groupName;
    this.showGroupOperators = true;
  }

  selectLogicOperator(operator: LogicOperator) {
    this.nodeSelected.emit({ nodeType: NodeType.LOGIC, operator: operator });
  }

  getOperatorsForGroup(groupName: string): LogicOperator[] {
    return this.operatorsGrouped.get(groupName) || [];
  }

}
