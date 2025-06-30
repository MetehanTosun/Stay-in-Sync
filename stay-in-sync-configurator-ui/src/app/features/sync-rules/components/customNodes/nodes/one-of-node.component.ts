import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    One Of
    <handle type="source" position="right" />
    <handle type="target" position="left" [id]="'inputs'" />
  </div>
  `,
  selector: 'app-one-of-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class OneOfNodeComponent extends CustomDynamicNodeComponent{}
