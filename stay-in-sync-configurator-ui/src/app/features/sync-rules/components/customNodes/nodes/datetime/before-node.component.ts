import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Before
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="bottom" [id]="'reference'" />
  </div>
  `,
  selector: 'app-before-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class BeforeNodeComponent extends CustomDynamicNodeComponent{}
