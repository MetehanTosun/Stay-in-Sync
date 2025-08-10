import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { LogicOperator, NodeType } from '../../models';
import { OperatorNodesApiService } from '../../service';

/**
 * Responsible for the main logic of the node selection/creation palette
 */
@Component({
  selector: 'app-node-palette',
  imports: [CommonModule],
  templateUrl: './node-palette.component.html',
  styleUrl: './node-palette.component.css'
})
export class NodePaletteComponent implements OnInit {
  //#region Setup
  @Input() showMainNodePalette = false;
  @Output() nodeSelected = new EventEmitter<{ nodeType: NodeType, operator?: LogicOperator }>();

  NodeType = NodeType; //* for .html file
  operatorsGrouped: Map<string, LogicOperator[]> = new Map<string, LogicOperator[]>();

  // Palette Status
  showLogicGroups = false;
  selectedLogicGroup: string | null = null;
  showGroupOperators = false;

  constructor(private nodesApi: OperatorNodesApiService) { }

  ngOnInit(): void {
    this.loadGroupedOperators();
  }
  //#endregion

  //#region Template Methods
  /**
   * This emits the to be created node
   *
   * @param nodeType The type of node that is to be created
   * @param operator The logic operator that is to be created, only for logic nodes
   */
  selectNode(nodeType: NodeType, operator?: LogicOperator) {
    this.nodeSelected.emit({ nodeType, operator });
  }

  /**
   * Opens the sub palette containing the logic groups
   */
  selectLogicNodePalette() {
    this.showLogicGroups = true;
  }

  /**
   * Closes all sub palettes and resets the Attributes
   */
  closeSubPalettes() {
    this.showLogicGroups = false;
    this.showGroupOperators = false;
    this.selectedLogicGroup = null;
  }

  /**
   * Opens the sub palette containing the given logic group's operators
   *
   * @param groupName
   */
  selectLogicGroup(groupName: string) {
    this.selectedLogicGroup = groupName;
    this.showGroupOperators = true;
  }
  //#endregion

  //#region REST Methods
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
  //#endregion

  //#region Getters
  /**
   * @returns the names of all logic groups
   */
  getLogicGroups(): string[] {
    return Array.from(this.operatorsGrouped.keys());
  }

  /**
   * Returns the operators of the given logic group
   *
   * @param groupName
   * @returns Array of logic operators
   */
  getOperatorsForGroup(groupName: string): LogicOperator[] {
    return this.operatorsGrouped.get(groupName) || [];
  }
  //#endregion
}
