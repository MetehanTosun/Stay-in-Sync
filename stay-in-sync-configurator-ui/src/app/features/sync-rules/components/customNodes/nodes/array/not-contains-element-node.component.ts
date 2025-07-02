import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Not Contains Element
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'array'" />
    <handle type="target" position="bottom" [id]="'element'" />
  </div>
  `,
  selector: 'app-not-contains-element-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class NotContainsElementNodeComponent extends CustomDynamicNodeComponent{}
