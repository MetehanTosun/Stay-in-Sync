import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

/**
 * A node representing a logical operator within a vflow graph
 */
@Component({
  selector: 'app-logic-node',
  imports: [HandleComponent, SelectableDirective],
  templateUrl: './logic-node.component.html',
  styleUrl: './logic-node.component.css'
})
export class LogicNodeComponent extends CustomNodeComponent {

}
