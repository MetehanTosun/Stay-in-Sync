import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    All Of
    <handle type="source" position="right" />
    <handle type="target" position="left" [id]="'inputs'" />
  </div>
  `,
  selector: 'app-all-of-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class AllOfNodeComponent extends CustomDynamicNodeComponent{}
