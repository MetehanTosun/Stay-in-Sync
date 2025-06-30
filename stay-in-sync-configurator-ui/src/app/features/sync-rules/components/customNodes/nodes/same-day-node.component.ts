import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Same Day
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date1'" />
    <handle type="target" position="bottom" [id]="'date2'" />
  </div>
  `,
  selector: 'app-same-day-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class SameDayNodeComponent extends CustomDynamicNodeComponent{}
