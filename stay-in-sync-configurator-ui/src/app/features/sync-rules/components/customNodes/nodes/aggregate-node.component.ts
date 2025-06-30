import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Aggregate
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'array'" />
    <handle type="target" position="left" [id]="'operation'" />
    <handle type="target" position="bottom" [id]="'predicate'" />
  </div>
  `,
  selector: 'app-aggregate-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class AggregateNodeComponent extends CustomDynamicNodeComponent{}
