import { Component, Input } from '@angular/core';
import { ComponentDynamicNode, CustomDynamicNodeComponent, DynamicNode, Node } from 'ngx-vflow';
import { CustomDynamicNode } from './custom-node';

@Component({
  selector: 'app-provider-node',
  template: `
    <div class="node provider-node">
      <div class="node-header">
        <div class="node-icon">Provider</div>
        <div class="node-title">{{ node.data?.().name }}</div>
      </div>
      <div class="node-body">
        <div>ARC: {{ node.data?.().arcId }}</div>
        <div class="json-path">{{ node.data?.().jsonPath }}</div>
      </div>
    </div>
  `,
  styleUrls: ['../../pages/edit-rule/edit-rule.component.scss']
})
export class ProviderNodeComponent {
  @Input() node!: ComponentDynamicNode;
}
