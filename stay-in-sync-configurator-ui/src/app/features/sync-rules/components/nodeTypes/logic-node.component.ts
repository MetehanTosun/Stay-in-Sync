import { Component, Input } from '@angular/core';
import { ComponentDynamicNode, CustomDynamicNodeComponent, DynamicNode, Node } from 'ngx-vflow';
import { CustomDynamicNode } from './custom-node';

@Component({
  selector: 'app-logic-node',
  template: `
    <div class="node logic-node" [class]="'operator-' + node.data?.().operatorType?.toLowerCase()">
      Logic
      <div class="operator-symbol">
        {{ getOperatorSymbol(node.data?.().operatorType) }}
      </div>
    </div>
  `,
  styleUrls: ['../../pages/edit-rule/edit-rule.component.scss']
})
export class LogicNodeComponent {
  @Input() node!: ComponentDynamicNode;

  getOperatorSymbol(operatorType: string): string {
    const symbols: Record<string, string> = {
      'ADD': '+',
      'SUBTRACT': '-',
      'MULTIPLY': '×',
      'DIVIDE': '÷',
      'LESS_THAN': '<',
      'GREATER_THAN': '>',
      'LESS_EQUAL': '≤',
      'GREATER_EQUAL': '≥',
      'EQUAL': '=',
      'NOT_EQUAL': '≠',
      'AND': '∧',
      'OR': '∨',
      'XOR': '⊕',
      'NOT': '¬'
    };
    return symbols[operatorType] || operatorType;
  }
}
