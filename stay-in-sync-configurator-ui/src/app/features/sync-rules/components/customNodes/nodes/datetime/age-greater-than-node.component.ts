import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Age Greater Than
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date'" />
    <handle type="target" position="bottom" [id]="'threshold'" />
  </div>
  `,
  selector: 'app-age-greater-than-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class AgeGreaterThanNodeComponent extends CustomDynamicNodeComponent{}
