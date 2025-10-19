import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';
import { LogicNodeData } from '../../../models';

/**
 * A node representing a logical operator within a vflow graph
 */
@Component({
  selector: 'app-logic-node',
  standalone: true,
  imports: [HandleComponent, SelectableDirective, CommonModule],
  templateUrl: './logic-node.component.html',
  styleUrls: ['./logic-node.component.css']
})
export class LogicNodeComponent extends CustomNodeComponent {
  displayTooltips = false;

  /**
   * Returns the node name if it differs from the operator type (default value)
   */
  getNodeName(): string {
    const data = this.node().data as LogicNodeData;
    if (data.name === data.operatorType) {
      return "";
    }
    return data.name;
  }
}
