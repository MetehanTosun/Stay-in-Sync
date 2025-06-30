import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    After
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="bottom" [id]="'reference'" />
  </div>
  `,
  selector: 'app-after-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class AfterNodeComponent extends CustomDynamicNodeComponent{}
