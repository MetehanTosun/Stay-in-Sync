import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Within Next
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date'" />
    <handle type="target" position="bottom" [id]="'timespan'" />
  </div>
  `,
  selector: 'app-within-next-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class WithinNextNodeComponent extends CustomDynamicNodeComponent{}
