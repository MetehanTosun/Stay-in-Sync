import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

@Component({
  selector: 'app-provider-node',
  imports: [HandleComponent],
  templateUrl: './provider-node.component.html',
  styleUrl: './provider-node.component.css'
})
export class ProviderNodeComponent extends CustomNodeComponent {

}
