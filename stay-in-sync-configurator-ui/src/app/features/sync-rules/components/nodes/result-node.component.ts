import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomNodeComponent, HandleComponent } from 'ngx-vflow';

@Component({
  selector: 'app-result-node',
  imports: [CommonModule, HandleComponent],
  template: `
    <div class="result-node"
         [class.selected]="node().data?.selected"
         (click)="onNodeClick($event)"
         (contextmenu)="onNodeContextMenu($event)">
      <div class="node-header">
        <span class="node-type">🎯 RESULT</span>
      </div>
      <div class="node-content">
        <h4>{{ node().data?.name || 'Result' }}</h4>
        <p class="description">Final output of the transformation</p>
      </div>

      <!-- Only input handle, no output -->
      <handle type="target" position="left" />
    </div>
  `,
  styles: [`
    @use "../../shared/styles/mixins" as *;

    .result-node {
      @include node-base;
      background: linear-gradient(135deg, #FF9800, #F57C00);
      border: 2px solid #E65100;
      min-width: 200px;
    }

    .node-header {
      @include node-header;
    }

    .node-content h4 {
      @include node-content-heading;
    }

    .description {
      font-size: 11px;
      opacity: 0.9;
      margin: 4px 0;
      font-style: italic;
    }

    .result-node.selected {
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
export class ResultNodeComponent extends CustomNodeComponent {
  onNodeClick(event: MouseEvent) {
    event.stopPropagation();
    event.preventDefault();

    const nodeClickEvent = new CustomEvent('nodeSelected', {
      detail: { node: this.node(), event: event },
      bubbles: true
    });

    event.target?.dispatchEvent(nodeClickEvent);
    console.log('Result node clicked:', this.node());
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
    console.log('Result node context menu:', this.node(), 'at position:', rect);
  }
}
