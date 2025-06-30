import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Not In Set
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="bottom" [id]="'set'" />
  </div>
  `,
  selector: 'app-not-in-set-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class NotInSetNodeComponent extends CustomDynamicNodeComponent{}
