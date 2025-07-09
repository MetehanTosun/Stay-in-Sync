import { JsonPipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ComponentDynamicNode, CustomDynamicNodeComponent, DynamicNode, Node } from 'ngx-vflow';
import { CustomDynamicNode } from './custom-node';

@Component({
  selector: 'app-constant-node',
  imports: [JsonPipe],
  template: `
    <div class="node constant-node">
      <div class="node-header">
        <div class="node-icon">Constant</div>
        <div class="node-title">{{ node.data?.().name }}</div>
      </div>
      <div class="node-value">
        {{ node.data?.().value | json }}
      </div>
    </div>
  `,
  styleUrls: ['../../pages/edit-rule/edit-rule.component.scss']
})
export class ConstantNodeComponent {
  @Input() node!: ComponentDynamicNode;
}
