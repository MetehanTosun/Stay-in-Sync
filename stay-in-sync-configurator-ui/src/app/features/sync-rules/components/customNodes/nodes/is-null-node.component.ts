import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Is Null
    <handle type="source" position="right" />
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-is-null-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class IsNullNodeComponent extends CustomDynamicNodeComponent{}
