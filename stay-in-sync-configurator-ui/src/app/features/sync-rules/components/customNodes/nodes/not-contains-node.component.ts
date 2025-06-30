import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Not Contains
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'text'" />
    <handle type="target" position="bottom" [id]="'substring'" />
  </div>
  `,
  selector: 'app-not-contains-node',
  imports: [Vflow],
  styleUrl: '../default.css',
})
export class NotContainsNodeComponent extends CustomDynamicNodeComponent{}
