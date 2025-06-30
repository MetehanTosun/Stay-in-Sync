import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Not
    <handle type="source" position="right" />
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-not-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class NotNodeComponent extends CustomDynamicNodeComponent{}
