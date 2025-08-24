import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';

/**
 * A node representing an external input provider within a vflow graph
 */
@Component({
  selector: 'app-provider-node',
  imports: [HandleComponent, SelectableDirective],
  templateUrl: './provider-node.component.html',
  styleUrl: './provider-node.component.css'
})
export class ProviderNodeComponent extends CustomNodeComponent {

}
