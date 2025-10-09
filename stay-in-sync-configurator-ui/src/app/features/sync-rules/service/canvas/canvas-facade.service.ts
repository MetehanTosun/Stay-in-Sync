import { Injectable } from '@angular/core';
import { VflowCanvasComponent } from '../../components/vflow-canvas/vflow-canvas.component';
import { NodeType } from '../../models';
import { MessageService } from 'primeng/api';

/**
 * The EditRule page registers the canvas instance using `register()` after the
 * ViewChild becomes available and calls the facade methods to access the canvas
 */
@Injectable({ providedIn: 'root' })
export class CanvasFacadeService {
  private canvas: VflowCanvasComponent | null = null;

  constructor(private messageService: MessageService) { }

  /**
   * Registers the canvas
   * @param canvas VflowCanvasComponent instance (ViewChild)
   */
  register(canvas: VflowCanvasComponent) {
    this.canvas = canvas;
  }

  /**
   * Unregisters the canvas reference
   * *Call this from the hosting component's ngOnDestroy to avoid holding references across component lifecycles.
   */
  unregister() {
    this.canvas = null;
  }

  /**
   * Adds a node to the canvas
   */
  addNode(nodeType: NodeType, pos: { x: number, y: number }, providerData?: { jsonPath: string; outputType: string }, constantValue?: any, operator?: any) {
    if (!this.canvas) {
      this.messageService.add({
        severity: 'error',
        summary: 'Unable to access canvas',
        detail: 'Cannot add node: Canvas is not available',
      });
      return null;
    }
    return this.canvas.addNode(nodeType, pos, providerData, constantValue, operator);
  }

  /**
   * Centers the viewport on the provided nodeId
   */
  centerOnNode(nodeId: number) {
    if (!this.canvas) {
      this.messageService.add({
        severity: 'error',
        summary: 'Unable to access canvas',
        detail: 'Cannot center on node: Canvas is not available',
      });
      return;
    }
    this.canvas.centerOnNode(nodeId);
  }

  /**
   * Triggers graph save on the registered canvas
   */
  saveGraph() {
    if (!this.canvas) {
      this.messageService.add({
        severity: 'error',
        summary: 'Unable to access canvas',
        detail: 'Cannot save graph: Canvas is not available',
      });
      return;
    }
    return this.canvas.saveGraph();
  }
}
