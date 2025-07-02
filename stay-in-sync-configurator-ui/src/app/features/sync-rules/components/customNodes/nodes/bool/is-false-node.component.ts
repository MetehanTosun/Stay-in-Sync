import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Is False
    <handle type="source" position="right" />
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-is-false-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class IsFalseNodeComponent extends CustomDynamicNodeComponent{}
