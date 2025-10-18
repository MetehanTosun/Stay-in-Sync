import { LogicOperatorMetadata } from '../../models';

/**
 * trackBy for group name ngFor to avoid re-rendering unchanged list items
 */
export function trackByGroupName(_index: number, groupName: string): string {
  return groupName;
}

/**
 * trackBy for operator ngFor to avoid re-rendering unchanged items
 */
export function trackByOperator(_index: number, operator: LogicOperatorMetadata): string {
  return operator.operatorName
}
