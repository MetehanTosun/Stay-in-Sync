import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Not Empty
    <handle type="source" position="right" />
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-not-empty-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class NotEmptyNodeComponent extends CustomDynamicNodeComponent{}
