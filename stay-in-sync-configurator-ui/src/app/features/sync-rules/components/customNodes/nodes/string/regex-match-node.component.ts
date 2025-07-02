import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Regex Match
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'text'" />
    <handle type="target" position="bottom" [id]="'pattern'" />
  </div>
  `,
  selector: 'app-regex-match-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class RegexMatchNodeComponent extends CustomDynamicNodeComponent{}
