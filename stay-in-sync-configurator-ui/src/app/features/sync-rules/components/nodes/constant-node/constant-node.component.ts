import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

/**
 * A node representing a static constant within a vflow graph
 */
@Component({
  selector: 'app-constant-node',
  standalone: true,
  imports: [HandleComponent, SelectableDirective, CommonModule],
  templateUrl: './constant-node.component.html',
  styleUrl: './constant-node.component.css'
})
export class ConstantNodeComponent extends CustomNodeComponent {
  displayTooltips = false;
}
