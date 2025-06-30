import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Is True
    <handle type="source" position="right" />
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-is-true-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class IsTrueNodeComponent extends CustomDynamicNodeComponent{}
