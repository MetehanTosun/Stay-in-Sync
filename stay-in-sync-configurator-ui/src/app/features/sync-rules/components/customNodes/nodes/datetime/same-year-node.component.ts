import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Same Year
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date1'" />
    <handle type="target" position="bottom" [id]="'date2'" />
  </div>
  `,
  selector: 'app-same-year-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class SameYearNodeComponent extends CustomDynamicNodeComponent{}
