import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Weekday Is
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date'" />
    <handle type="target" position="bottom" [id]="'weekday'" />
  </div>
  `,
  selector: 'app-weekday-is-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class WeekdayIsNodeComponent extends CustomDynamicNodeComponent{}
