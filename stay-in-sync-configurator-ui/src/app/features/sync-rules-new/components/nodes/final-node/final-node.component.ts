import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

/**
 * A node representing the final boolean result of a vflow graph
 */
@Component({
  selector: 'app-final-node',
  imports: [HandleComponent],
  templateUrl: './final-node.component.html',
  styleUrl: './final-node.component.css'
})
export class FinalNodeComponent extends CustomNodeComponent {

}
