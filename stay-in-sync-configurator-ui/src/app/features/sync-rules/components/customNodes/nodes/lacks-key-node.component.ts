import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Lacks Key
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'object'" />
    <handle type="target" position="bottom" [id]="'key'" />
  </div>
  `,
  selector: 'app-lacks-key-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class LacksKeyNodeComponent extends CustomDynamicNodeComponent{}
