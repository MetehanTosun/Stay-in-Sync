import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Less Than
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="bottom" [id]="'threshold'" />
  </div>
  `,
  selector: 'app-less-than-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class LessThanNodeComponent extends CustomDynamicNodeComponent{}
