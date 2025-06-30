import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Between
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="left" [id]="'min'" />
    <handle type="target" position="bottom" [id]="'max'" />
  </div>
  `,
  selector: 'app-between-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class BetweenNodeComponent extends CustomDynamicNodeComponent{}
