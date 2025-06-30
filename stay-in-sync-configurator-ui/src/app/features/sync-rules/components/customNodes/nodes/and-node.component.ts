import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div class="logic-gate">
    <div class="gate-symbol">∧</div>
    <div class="gate-label">AND</div>
    <handle type="source" position="right" />
    <handle type="target" position="left" [id]="'inputs'" />
  </div>
  `,
  selector: 'app-and-node',
  imports: [Vflow],
  styleUrl: '../logic-gate.css',
})
export class AndNodeComponent extends CustomDynamicNodeComponent{}
