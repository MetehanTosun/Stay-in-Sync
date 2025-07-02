import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Exists
    <handle type="source" position="right" />
    <handle type="target" position="left" />
  </div>
  `,
  selector: 'app-exists-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class ExistsNodeComponent extends CustomDynamicNodeComponent{}
