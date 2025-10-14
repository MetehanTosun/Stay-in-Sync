import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

/**
 * A node representing an external input provider within a vflow graph
 */
@Component({
  selector: 'app-provider-node',
  standalone: true,
  imports: [HandleComponent, SelectableDirective, CommonModule],
  templateUrl: './provider-node.component.html',
  styleUrls: ['./provider-node.component.css']
})
export class ProviderNodeComponent extends CustomNodeComponent {
  displayTooltips = false;

  /**
   * @returns The entire JSON path with the leading 'source.' removed
   */
  getTrimmedJsonpath(): string {
    const path = this.node()?.data?.jsonPath || '';
    return path.replace(/^source\./, '');
  }
}
