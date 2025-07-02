import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Length Equals
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'array'" />
    <handle type="target" position="bottom" [id]="'length'" />
  </div>
  `,
  selector: 'app-length-equals-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class LengthEqualsNodeComponent extends CustomDynamicNodeComponent{}
