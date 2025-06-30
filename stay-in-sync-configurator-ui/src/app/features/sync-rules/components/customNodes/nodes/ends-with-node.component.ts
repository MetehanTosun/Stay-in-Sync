import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Ends With
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'string'" />
    <handle type="target" position="bottom" [id]="'suffix'" />
  </div>
  `,
  selector: 'app-ends-with-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class EndsWithNodeComponent extends CustomDynamicNodeComponent{}
