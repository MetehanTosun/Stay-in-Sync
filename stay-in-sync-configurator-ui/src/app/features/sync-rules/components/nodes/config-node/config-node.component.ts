import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

/**
 * A node controlling the logic behavior of the transformation graph
 */
@Component({
  selector: 'app-config-node',
  imports: [HandleComponent, SelectableDirective, CommonModule],
  templateUrl: './config-node.component.html',
  styleUrl: './config-node.component.css'
})
export class ConfigNodeComponent extends CustomNodeComponent {
  displayTooltips = false;
}
