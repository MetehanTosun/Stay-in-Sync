import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

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
}
