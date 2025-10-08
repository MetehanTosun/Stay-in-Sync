import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { LogicOperatorMetadata, NodeType } from '../../models';
import { OperatorNodesApiService } from '../../service';
import { MessageService } from 'primeng/api';

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
  @Output() nodeSelected = new EventEmitter<{ nodeType: NodeType, operator?: LogicOperatorMetadata }>();

  NodeType = NodeType; //* for .html file
  operatorsGrouped: Map<string, LogicOperatorMetadata[]> = new Map<string, LogicOperatorMetadata[]>();

  // Palette Status
  showLogicGroups = false;
  selectedLogicGroup: string | null = null;
  showGroupOperators = false;

  constructor(
    private nodesApi: OperatorNodesApiService,
    private messageService: MessageService
  ) { }

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
  selectNode(nodeType: NodeType, operator?: LogicOperatorMetadata) {
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
      next: (operatorsGrouped: Map<string, LogicOperatorMetadata[]>) => {
        this.operatorsGrouped = operatorsGrouped;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Loading operator nodes',
          detail: 'An error accurred while loading the logic operators nodes. \n Please check the logs or the console.'
        });
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
  getOperatorsForGroup(groupName: string): LogicOperatorMetadata[] {
    return this.operatorsGrouped.get(groupName) || [];
  }
  //#endregion
}
