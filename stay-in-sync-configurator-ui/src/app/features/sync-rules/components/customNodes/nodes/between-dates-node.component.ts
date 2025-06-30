import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Between Dates
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date'" />
    <handle type="target" position="left" [id]="'start'" />
    <handle type="target" position="bottom" [id]="'end'" />
  </div>
  `,
  selector: 'app-between-dates-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class BetweenDatesNodeComponent extends CustomDynamicNodeComponent{}
