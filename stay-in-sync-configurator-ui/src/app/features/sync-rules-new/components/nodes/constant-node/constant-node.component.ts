import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

@Component({
  selector: 'app-constant-node',
  imports: [HandleComponent],
  templateUrl: './constant-node.component.html',
  styleUrl: './constant-node.component.css'
})
export class ConstantNodeComponent extends CustomNodeComponent {

}
