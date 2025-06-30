import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Is Not Null
    <handle type="source" position="right" />
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-is-not-null-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class IsNotNullNodeComponent extends CustomDynamicNodeComponent{}
