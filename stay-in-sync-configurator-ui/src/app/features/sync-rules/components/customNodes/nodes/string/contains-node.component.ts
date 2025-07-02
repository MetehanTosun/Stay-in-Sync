import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Contains
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'string'" />
    <handle type="target" position="bottom" [id]="'substring'" />
  </div>
  `,
  selector: 'app-contains-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class ContainsNodeComponent extends CustomDynamicNodeComponent{}
