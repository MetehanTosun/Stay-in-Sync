import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { GroupedOperators, LogicOperatorMetadata, NodeType } from '../../models';
import { OperatorNodesApiService } from '../../service';
import { trackByGroupName, trackByOperator } from './node-palette.utils';
import { MessageService } from 'primeng/api';

/**
 * Responsible for the main logic of the node selection/creation palette
 */
@Component({
  selector: 'app-node-palette',
  imports: [CommonModule],
  templateUrl: './node-palette.component.html',
  styleUrls: ['./node-palette.component.css']
})
export class NodePaletteComponent implements OnInit {
  //#region  Fields
  /** Whether the main palette is visible */
  @Input() showMainNodePalette = false;

  /** Coordinates where the palette should be rendered. */
  @Input() position?: { x: number, y: number } | null = null;

  /** Emitted when the user selects a node to create. Payload: { nodeType, operator? } */
  @Output() nodeSelected = new EventEmitter<{ nodeType: NodeType, operator?: LogicOperatorMetadata }>();
  //#endregion


  /** Expose enum to template */
  NodeType = NodeType;

  /** Operators grouped by category */
  private operatorsGrouped: GroupedOperators = {};

  /** Cached group names for template iteration (prevents repeated Object.keys calls) */
  logicGroupKeys: string[] = [];

  /** Whether the logic groups sub-palette is shown */
  showLogicGroups = false;

  /** Currently selected logic group */
  selectedLogicGroup: string | null = null;

  /** Whether the operators submenu is shown */
  showGroupOperators = false;


  constructor(
    private nodesApi: OperatorNodesApiService,
    private messageService: MessageService
  ) { }

  //#region Lifecycle
  ngOnInit(): void {
    this.loadGroupedOperators();
  }
  //#endregion

  //#region Style helpers
  get paletteStyle() {
    if (this.showMainNodePalette && this.position && this.position.x > 0) {
      return {
        position: 'fixed',
        left: `${this.position.x}px`,
        top: `${this.position.y}px`
      } as { [key: string]: string };
    }
    return {} as { [key: string]: string };
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

  //#region Template Helpers
  trackByGroupName = trackByGroupName;
  trackByOperator = trackByOperator;
  //#endregion

  //#region REST Methods
  /**
   * Loads all operator nodes in a grouped format from the backend
   */
  loadGroupedOperators() {
    this.nodesApi.getGroupedOperators().subscribe({
      next: (operatorsGrouped: GroupedOperators) => {
        this.operatorsGrouped = operatorsGrouped;
        this.logicGroupKeys = Object.keys(operatorsGrouped);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Loading operator nodes',
          detail: 'An error occurred while loading the logic operator nodes.'
        });
        console.error(err);
      },
    });
  }
  //#endregion

  //#region Getters
  /**
   * Returns the operators of the given logic group
   *
   * @param groupName
   * @returns Array of logic operators
   */
  getOperatorsForGroup(groupName: string): LogicOperatorMetadata[] {
    return this.operatorsGrouped[groupName] || [];
  }
  //#endregion
}
