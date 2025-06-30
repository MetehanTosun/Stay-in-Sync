import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Month Is
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date'" />
    <handle type="target" position="bottom" [id]="'month'" />
  </div>
  `,
  selector: 'app-month-is-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class MonthIsNodeComponent extends CustomDynamicNodeComponent{}
