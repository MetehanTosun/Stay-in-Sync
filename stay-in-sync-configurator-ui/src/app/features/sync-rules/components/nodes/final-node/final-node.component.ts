import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

/**
 * A node representing the final boolean result of a vflow graph
 */
@Component({
  selector: 'app-final-node',
  standalone: true,
  imports: [HandleComponent, SelectableDirective, CommonModule],
  templateUrl: './final-node.component.html',
  styleUrl: './final-node.component.css'
})
export class FinalNodeComponent extends CustomNodeComponent {
  displayTooltips = false;
}
