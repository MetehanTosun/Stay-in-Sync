import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div class="logic-gate">
    <div class="gate-symbol">∨</div>
    <div class="gate-label">OR</div>
    <handle type="source" position="right" [id]="'output'" />
    <handle type="target" position="left" [id]="'inputs'" />
  </div>
  `,
  selector: 'app-or-node',
  imports: [Vflow],
  styleUrl: '../../logic-gate.css',
})
export class OrNodeComponent extends CustomDynamicNodeComponent{}
