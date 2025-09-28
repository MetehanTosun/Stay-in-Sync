import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

/**
 * A node representing an external input provider within a vflow graph
 */
@Component({
  selector: 'app-provider-node',
  imports: [HandleComponent, SelectableDirective, CommonModule],
  templateUrl: './provider-node.component.html',
  styleUrl: './provider-node.component.css'
})
export class ProviderNodeComponent extends CustomNodeComponent {
  displayTooltips = false;

  /**
   * @returns The Property whose value is read by this provider node
   */
  getSourceProperty(): string {
    const parts = this.node().data?.jsonPath.split('.');
    return parts[parts.length - 1];
  }

  /**
   * @returns The entire JSON path with the leading 'source.' removed
   */
  getTrimmedJsonpath(): string {
    return this.node().data?.jsonPath.replace(/^source\./, '');
  }
}
