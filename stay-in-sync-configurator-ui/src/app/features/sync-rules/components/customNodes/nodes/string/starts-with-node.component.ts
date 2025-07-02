import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Starts With
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'string'" />
    <handle type="target" position="bottom" [id]="'prefix'" />
  </div>
  `,
  selector: 'app-starts-with-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class StartsWithNodeComponent extends CustomDynamicNodeComponent{}
