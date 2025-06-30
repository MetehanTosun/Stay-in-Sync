import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Equals (Ignore Case)
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'left'" />
    <handle type="target" position="bottom" [id]="'right'" />
  </div>
  `,
  selector: 'app-equals-ignore-case-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class EqualsIgnoreCaseNodeComponent extends CustomDynamicNodeComponent{}
