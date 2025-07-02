import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Length Between
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'collection'" />
    <handle type="target" position="left" [id]="'min'" />
    <handle type="target" position="bottom" [id]="'max'" />
  </div>
  `,
  selector: 'app-length-between-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class LengthBetweenNodeComponent extends CustomDynamicNodeComponent{}
