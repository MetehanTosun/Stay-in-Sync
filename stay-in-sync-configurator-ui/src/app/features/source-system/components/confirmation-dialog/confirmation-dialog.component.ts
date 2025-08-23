import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';

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
export class ConfirmationDialogComponent {
  @Input() visible: boolean = false;
  @Input() data: ConfirmationDialogData = {
    title: 'Confirm Action',
    message: 'Are you sure you want to proceed?',
    confirmLabel: 'Confirm',
    cancelLabel: 'Cancel',
    severity: 'warning'
  };

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onConfirm(): void {
    this.confirmed.emit();
    this.closeDialog();
  }

  onCancel(): void {
    this.cancelled.emit();
    this.closeDialog();
  }

  closeDialog(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

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