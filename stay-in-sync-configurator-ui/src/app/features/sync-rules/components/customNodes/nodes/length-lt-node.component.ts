import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Length Less Than
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'collection'" />
    <handle type="target" position="bottom" [id]="'value'" />
  </div>
  `,
  selector: 'app-length-lt-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class LengthLtNodeComponent extends CustomDynamicNodeComponent{}
