import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ValidationError } from '../../models/interfaces/validation-error.interface';

/**
 * This component allows the user to view all validation errors the backend gathers while saving the graph
 */
@Component({
  selector: 'app-error-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './error-panel.component.html',
  styleUrl: './error-panel.component.css'
})
export class ErrorPanelComponent {
  /**
   * Array of validation errors provided by the backend.
   */
  @Input() errors: ValidationError[] = [];

  /**
   * Event emitted when the user selects on an applicable error to focus the canvas view on the contained node id
   */
  @Output() errorClicked = new EventEmitter<number>();

  /**
   * Describes the state of the error panel (i.e. expanded or contracted)
   */
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
