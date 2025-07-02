import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Input
    <handle type="source" position="right" />
  </div>
  `,
  selector: 'app-input-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class InputNodeComponent extends CustomDynamicNodeComponent{}
