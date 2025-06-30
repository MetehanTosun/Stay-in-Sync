import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Output
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-output-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class OutputNodeComponent extends CustomDynamicNodeComponent{}
