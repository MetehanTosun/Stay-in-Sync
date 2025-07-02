import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Timezone Offset Equals
    <handle type="source" position="right" />
    <handle type="target" position="top" [id]="'date'" />
    <handle type="target" position="bottom" [id]="'offset'" />
  </div>
  `,
  selector: 'app-timezone-offset-equals-node',
  imports: [Vflow],
  styleUrl: '../../default.css',
})
export class TimezoneOffsetEqualsNodeComponent extends CustomDynamicNodeComponent{}
