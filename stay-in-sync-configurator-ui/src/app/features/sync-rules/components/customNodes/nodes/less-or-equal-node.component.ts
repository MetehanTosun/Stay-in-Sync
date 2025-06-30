import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Less Or Equal
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'left'" />
    <handle type="target" position="bottom" [id]="'right'" />
  </div>
  `,
  selector: 'app-less-or-equal-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class LessOrEqualNodeComponent extends CustomDynamicNodeComponent{}
