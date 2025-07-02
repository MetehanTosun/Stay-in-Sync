import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Has No Keys
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'object'" />
    <handle type="target" position="bottom" [id]="'keys'" />
  </div>
  `,
  selector: 'app-has-no-keys-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class HasNoKeysNodeComponent extends CustomDynamicNodeComponent{}
