import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Same Month
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date1'" />
    <handle type="target" position="bottom" [id]="'date2'" />
  </div>
  `,
  selector: 'app-same-month-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class SameMonthNodeComponent extends CustomDynamicNodeComponent{}
