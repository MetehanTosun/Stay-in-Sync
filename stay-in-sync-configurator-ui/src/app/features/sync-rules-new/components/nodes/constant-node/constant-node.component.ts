import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

/**
 * A node representing a static constant within a vflow graph
 */
@Component({
  selector: 'app-constant-node',
  imports: [HandleComponent],
  templateUrl: './constant-node.component.html',
  styleUrl: './constant-node.component.css'
})
export class ConstantNodeComponent extends CustomNodeComponent {

}
