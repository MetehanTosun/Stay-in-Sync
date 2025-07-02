import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Contains All
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'array'" />
    <handle type="target" position="bottom" [id]="'elements'" />
  </div>
  `,
  selector: 'app-contains-all-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class ContainsAllNodeComponent extends CustomDynamicNodeComponent{}
