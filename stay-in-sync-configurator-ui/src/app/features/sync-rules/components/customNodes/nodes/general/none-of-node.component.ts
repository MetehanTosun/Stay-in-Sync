import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    None Of
    <handle type="source" position="right" />
    <handle type="target" position="left" [id]="'inputs'" />
  </div>
  `,
  selector: 'app-none-of-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class NoneOfNodeComponent extends CustomDynamicNodeComponent{}
