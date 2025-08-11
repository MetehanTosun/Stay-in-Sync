import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

/**
 * A node representing a logical operator within a vflow graph
 */
@Component({
  selector: 'app-logic-node',
  imports: [HandleComponent],
  templateUrl: './logic-node.component.html',
  styleUrl: './logic-node.component.css'
})
export class LogicNodeComponent extends CustomNodeComponent {

}
