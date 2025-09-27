import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ValidationError } from '../../models/interfaces/validation-error.interface';

/**
 * This component allows the user to view all validation errors the backend gathers while saving the graph
 */
@Component({
  selector: 'app-error-panel',
  imports: [CommonModule],
  templateUrl: './error-panel.component.html',
  styleUrl: './error-panel.component.css'
})
export class ErrorPanelComponent {
  @Input() errors: ValidationError[] = [];
  @Output() errorClicked = new EventEmitter<number>();
  open = false;

  /**
   * expands or contracts the Error Panel
   */
  togglePanel() {
    this.open = !this.open;
  }

  /**
   * Redirects the canvas view to the problem node if applicable
   *
   * @param error
   */
  onErrorClick(error: ValidationError) {
    if (error.nodeId !== undefined) {
      this.errorClicked.emit(error.nodeId);
    }
  }
}
