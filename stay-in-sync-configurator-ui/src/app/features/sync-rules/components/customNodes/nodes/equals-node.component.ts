import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Equals
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="bottom" [id]="'comparison'" />
  </div>
  `,
  selector: 'app-equals-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class EqualsNodeComponent extends CustomDynamicNodeComponent{}
