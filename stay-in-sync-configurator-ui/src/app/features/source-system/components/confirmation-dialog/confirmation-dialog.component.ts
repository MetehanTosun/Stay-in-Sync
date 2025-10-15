import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';

/** Data model for the confirmation dialog. */
export interface ConfirmationDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  severity?: 'warning' | 'danger' | 'info';
}

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule, DialogModule, ButtonModule],
  templateUrl: './confirmation-dialog.component.html',
  styleUrls: ['./confirmation-dialog.component.css']
})
/** Standalone confirmation dialog with severity, content, and actions. */
export class ConfirmationDialogComponent {
  /** Controls dialog visibility (two-way bound via `visibleChange`). */
  @Input() visible: boolean = false;
  /** Dialog content and presentation options. */
  @Input() data: ConfirmationDialogData = {
    title: 'Confirm Action',
    message: 'Are you sure you want to proceed?',
    confirmLabel: 'Confirm',
    cancelLabel: 'Cancel',
    severity: 'warning'
  };

  /** Emits on visibility changes to support two-way binding. */
  @Output() visibleChange = new EventEmitter<boolean>();
  /** Emitted when user confirms the action. */
  @Output() confirmed = new EventEmitter<void>();
  /** Emitted when user cancels the action. */
  @Output() cancelled = new EventEmitter<void>();

  /** Emit confirmation and close the dialog. */
  onConfirm(): void {
    this.confirmed.emit();
    this.closeDialog();
  }

  /** Emit cancellation and close the dialog. */
  onCancel(): void {
    this.cancelled.emit();
    this.closeDialog();
  }

  /** Close the dialog and propagate visibility change. */
  closeDialog(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  /** Map severity to PrimeNG button class. */
  getSeverityClass(): string {
    switch (this.data.severity) {
      case 'danger':
        return 'p-button-danger';
      case 'warning':
        return 'p-button-warning';
      case 'info':
        return 'p-button-info';
      default:
        return 'p-button-warning';
    }
  }

  /** Map severity to icon class. */
  getIconClass(): string {
    switch (this.data.severity) {
      case 'danger':
        return 'pi-exclamation-triangle';
      case 'warning':
        return 'pi-exclamation-triangle';
      case 'info':
        return 'pi-info-circle';
      default:
        return 'pi-exclamation-triangle';
    }
  }
} 