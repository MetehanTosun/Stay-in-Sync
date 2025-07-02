import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Matches Schema
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'value'" />
    <handle type="target" position="bottom" [id]="'schema'" />
  </div>
  `,
  selector: 'app-matches-schema-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class MatchesSchemaNodeComponent extends CustomDynamicNodeComponent{}
