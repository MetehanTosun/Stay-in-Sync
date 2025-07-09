import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

@Component({
  selector: 'app-provider-node',
  imports: [CommonModule, HandleComponent],
  template: `
    <div class="provider-node"
         [class.selected]="node().data?.selected"
         (click)="onNodeClick($event)"
         (contextmenu)="onNodeContextMenu($event)">
      <div class="node-header">
        <span class="node-type">📡 PROVIDER</span>
      </div>
      <div class="node-content">
        <h4>{{ node().data?.name || 'Provider Node' }}</h4>
        <p class="json-path">{{ node().data?.jsonPath }}</p>
        <div class="arc-id">Arc ID: {{ node().data?.arcId }}</div>
      </div>

      <handle type="source" position="right" />
    </div>
  `,
  styles: [`
    @use "../../shared/styles/mixins" as *;

    .provider-node {
      @include node-base;
      background: linear-gradient(135deg, #4CAF50, #45a049);
      border: 2px solid #2E7D32;
      min-width: 200px;
    }

    .node-header {
      @include node-header;
    }

    .node-content h4 {
      @include node-content-heading;
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

    .provider-node.selected {
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
export class ProviderNodeComponent extends CustomNodeComponent {
  onNodeClick(event: MouseEvent) {
    event.stopPropagation();
    event.preventDefault();

    const nodeClickEvent = new CustomEvent('nodeSelected', {
      detail: { node: this.node(), event: event },
      bubbles: true
    });

    event.target?.dispatchEvent(nodeClickEvent);
    console.log('Provider node clicked:', this.node());
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
    console.log('Provider node context menu:', this.node(), 'at position:', rect);
  }
}
