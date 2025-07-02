import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Type Is
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="bottom" [id]="'type'" />
  </div>
  `,
  selector: 'app-type-is-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class TypeIsNodeComponent extends CustomDynamicNodeComponent{}
