import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div class="logic-gate xor-gate">
    XOR
    <handle type="source" position="right" />
    <handle type="target" position="left" [id]="'inputs'" />
  </div>
  `,
  selector: 'app-xor-node',
  imports: [Vflow],
  styleUrls: ['../../default.css', '../../logic-gate.css'],
})
export class XorNodeComponent extends CustomDynamicNodeComponent{}
