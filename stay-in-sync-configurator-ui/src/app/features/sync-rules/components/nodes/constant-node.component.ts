import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

@Component({
  selector: 'app-constant-node',
  imports: [CommonModule, HandleComponent],
  template: `
    <div class="constant-node"
         [class.selected]="node().data?.selected"
         (click)="onNodeClick($event)"
         (contextmenu)="onNodeContextMenu($event)">
      <div class="node-header">
        <span class="node-type">🔢 CONSTANT</span>
      </div>
      <div class="node-content">
        <h4>{{ node().data?.name || 'Constant Node' }}</h4>
        <div class="constant-value">Value: {{ node().data?.value || 'N/A' }}</div>
        <div class="arc-id" *ngIf="node().data?.arcId">Arc ID: {{ node().data?.arcId }}</div>
      </div>

      <handle type="source" position="right" />
    </div>
  `,
  styles: [`
    @use "../../shared/styles/mixins" as *;

    .constant-node {
      @include node-base;
      background: linear-gradient(135deg, #FF9800, #F57C00);
      border: 2px solid #E65100;
      min-width: 180px;
    }

    .node-header {
      @include node-header;
    }

    .node-content h4 {
      @include node-content-heading;
    }

    .constant-value {
      font-size: 12px;
      opacity: 0.9;
      margin: 4px 0;
      background: rgba(255,255,255,0.2);
      padding: 4px 8px;
      border-radius: 4px;
    }

    .arc-id {
      font-size: 11px;
      opacity: 0.9;
      margin-top: 4px;
    }

    .constant-node.selected {
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
  `]
})
export class ConstantNodeComponent extends CustomNodeComponent {
  onNodeClick(event: MouseEvent) {
    event.stopPropagation();
    event.preventDefault();

    const nodeClickEvent = new CustomEvent('nodeSelected', {
      detail: { node: this.node(), event: event },
      bubbles: true
    });

    event.target?.dispatchEvent(nodeClickEvent);
    console.log('Constant node clicked:', this.node());
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
    console.log('Constant node context menu:', this.node(), 'at position:', rect);
  }
}
