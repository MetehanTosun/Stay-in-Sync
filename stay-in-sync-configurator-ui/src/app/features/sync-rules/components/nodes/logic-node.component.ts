import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

@Component({
  selector: 'app-logic-node',
  imports: [CommonModule, HandleComponent],
  template: `
    <div class="logic-node"
         [class.selected]="node().data?.selected"
         (click)="onNodeClick($event)"
         (contextmenu)="onNodeContextMenu($event)">
      <div class="node-header">
        <span class="node-type">⚙️ LOGIC</span>
      </div>
      <div class="node-content">
        <h4>{{ node().data?.name || 'Logic Node' }}</h4>
        <div class="operator-type">{{ node().data?.operatorType || 'OPERATION' }}</div>
        <p class="json-path" *ngIf="node().data?.jsonPath">{{ node().data?.jsonPath }}</p>
        <div class="arc-id" *ngIf="node().data?.arcId">Arc ID: {{ node().data?.arcId }}</div>
      </div>

      <div class="input-handles" *ngIf="hasMultipleInputs()">
        <div class="input-handle"
             *ngFor="let input of getInputHandles(); trackBy: trackByIndex"
             [style.top.%]="getInputHandlePosition(input.index)">
          <span class="input-label">{{ input.label }}</span>
          <handle type="target" position="left" [id]="input.id" />
        </div>
      </div>

      <div class="single-input" *ngIf="!hasMultipleInputs()">
        <handle type="target" position="left" />
      </div>

      <handle type="source" position="right" />
    </div>
  `,
  styles: [`
    @use "../../shared/styles/mixins" as *;

    .logic-node {
      @include node-base;
      background: linear-gradient(135deg, #2196F3, #1976D2);
      border: 2px solid #0D47A1;
      min-width: 200px;
    }

    .node-header {
      margin-bottom: 8px;
      font-size: 12px;
      font-weight: bold;
      opacity: 0.9;
    }

    .node-content h4 {
      margin: 0 0 8px 0;
      font-size: 14px;
      font-weight: 600;
    }

    .operator-type {
      font-size: 12px;
      font-weight: bold;
      background: rgba(255, 255, 255, 0.2);
      padding: 2px 6px;
      border-radius: 4px;
      margin-bottom: 8px;
      text-align: center;
    }

    .json-path {
      font-size: 10px;
      opacity: 0.8;
      margin: 4px 0;
      word-break: break-all;
    }

    .arc-id {
      font-size: 11px;
      opacity: 0.9;
      margin-top: 4px;
    }

    .input-handles {
      position: absolute;
      left: -8px;
      top: 0;
      height: 100%;
    }

    .input-handle {
      position: absolute;
      display: flex;
      align-items: center;
      z-index: 10;
    }

    .input-label {
      background: rgba(0,0,0,0.7);
      color: white;
      font-size: 10px;
      font-weight: bold;
      padding: 2px 6px;
      border-radius: 3px;
      margin-right: 8px;
      min-width: 16px;
      text-align: center;
      box-shadow: 0 1px 3px rgba(0,0,0,0.3);
    }

    .single-input {
      position: absolute;
      left: -8px;
      top: 50%;
      transform: translateY(-50%);
    }

    .input-handle:nth-child(1) .input-label {
      background: #E91E63; /* first input color (A) */
    }

    .input-handle:nth-child(2) .input-label {
      background: #9C27B0; /*  second input color (B) */
    }

    .input-handle:nth-child(3) .input-label {
      background: #673AB7; /* third input color (C) */
    }

    .input-handle:nth-child(4) .input-label {
      background: #3F51B5; /* fourth input color (D) */
    }

    .logic-node.selected {
      border-color: #FFD700;
      box-shadow: 0 0 0 3px rgba(255, 215, 0, 0.3), 0 4px 8px rgba(0,0,0,0.2);
      animation: selection-pulse 2s infinite;
    }

    @keyframes selection-pulse {
      0%, 100% {
        box-shadow: 0 0 0 3px rgba(255, 215, 0, 0.3), 0 4px 8px rgba(0,0,0,0.2);
      }
      50% {
        box-shadow: 0 0 0 6px rgba(255, 215, 0, 0.2), 0 4px 8px rgba(0,0,0,0.2);
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogicNodeComponent extends CustomNodeComponent {

  onNodeClick(event: MouseEvent) {
    event.stopPropagation();
    event.preventDefault();

    const nodeClickEvent = new CustomEvent('nodeSelected', {
      detail: { node: this.node(), event: event },
      bubbles: true
    });

    event.target?.dispatchEvent(nodeClickEvent);
    console.log('Logic node clicked:', this.node());
  }

  onNodeContextMenu(event: MouseEvent) {
    event.stopPropagation();
    event.preventDefault();

    const rect = (event.target as HTMLElement).getBoundingClientRect();

    const contextMenuEvent = new CustomEvent('nodeContextMenu', {
      detail: {
        node: this.node(),
        event: event,
        buttonRect: rect
      },
      bubbles: true
    });

    (event.target as HTMLElement).dispatchEvent(contextMenuEvent);
    console.log('Logic node context menu:', this.node(), 'at position:', rect);
  }

  hasMultipleInputs(): boolean {
    const inputNodes = this.node().data?.inputNodes;
    return (inputNodes?.length || 0) > 1;
  }

  getInputHandles(): Array<{index: number, label: string, id: string}> {
    const inputNodes = this.node().data?.inputNodes;
    if (!inputNodes || inputNodes.length <= 1) return [];

    const sortedInputs = [...inputNodes].sort((a, b) => a.orderIndex - b.orderIndex);

    return sortedInputs.map((input, idx) => ({
      index: idx,
      label: String.fromCharCode(65 + idx), // A, B, C, D... sTODO correct Label
      id: `input-${input.orderIndex}`
    }));
  }

  getInputHandlePosition(index: number): number {
    const handles = this.getInputHandles();
    if (handles.length <= 1) return 50;

    const padding = 20;
    const availableSpace = 100 - (2 * padding);
    const step = handles.length > 1 ? availableSpace / (handles.length - 1) : 0;

    return padding + (index * step);
  }

  trackByIndex(index: number): number {
    return index;
  }
}
