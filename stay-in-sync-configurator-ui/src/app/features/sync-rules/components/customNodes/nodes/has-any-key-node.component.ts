import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Has Any Key
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'object'" />
    <handle type="target" position="bottom" [id]="'keys'" />
  </div>
  `,
  selector: 'app-has-any-key-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class HasAnyKeyNodeComponent extends CustomDynamicNodeComponent{}
