import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { MessageService } from 'primeng/api';

/**
 * This component manages the modal for setting the values of a constant node
 */
@Component({
  selector: 'app-set-constant-value-modal',
  standalone: true,
  imports: [FormsModule, CommonModule, Dialog, Button],
  templateUrl: './set-constant-value-modal.component.html',
  styleUrls: ['../modal-shared.component.css', './set-constant-value-modal.component.css']
})
export class SetConstantValueModalComponent implements OnChanges {
  //#region Fields
  /** Controls dialog visibility (two-way binding with `visibleChange`) */
  @Input() visible = true;

  /** Emits when dialog visibility changes (two-way binding with `visible`) */
  @Output() visibleChange = new EventEmitter<boolean>();

  /** The current value passed into the modal when editing an existing constant */
  @Input() currentValue = '';

  /** Emitted when a new constant value is created (payload: parsed value) */
  @Output() constantCreated = new EventEmitter<any>();

  /** Emitted when saving an edited constant (payload: parsed value) */
  @Output() save = new EventEmitter<string>();

  /** Emitted when the modal closes */
  @Output() modalsClosed = new EventEmitter<void>();

  /** String containing the user entered value */
  constantValue = '';

  /** Controls whether the tip is displayed in the UI */
  isTipPopupVisible = false;
  //#endregion

  //#region Lifecylce
  /**
   * Syncs editor content when the modal visibility or provided `currentValue` change.
   */
  ngOnChanges(changes: SimpleChanges) {
    if (!changes['visible'] && !changes['currentValue']) return;
    this.constantValue = this.currentValue || '';
  }
  //#endregion

  constructor(private messageService: MessageService) { }

  //#region Modal Methods
  /**
   * Concludes the constant node creation by validating and forwarding the constant value.
   * Emits either `save` (when editing) or `constantCreated` (when creating new).
   */
  submit() {
    if (!this.constantValue.trim()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Invalid Constant Value',
        detail: "Please enter a constant value"
      })
      return;
    }

    (this.currentValue !== '' ? this.save : this.constantCreated).emit(this.parseValue(this.constantValue));
    this.closeModal();
  }

  /**
   * Closes this modal
   */
  closeModal() {
    this.constantValue = '';
    this.modalsClosed.emit();
    this.visible = false;
    this.visibleChange.emit(false);
  }

  toggleTipPopup() {
    this.isTipPopupVisible = !this.isTipPopupVisible;
  }
  //#endregion

  //#region Helpers
  /**
   * Parse the string input to the appropriate JavaScript type
   *
   * @param value
   * @returns
   */
  private parseValue(value: string): any {
    const trimmedValue = value.trim().toLowerCase();

    // Null and Boolean
    switch (trimmedValue) {
      case 'null':
        return null;
      case 'true':
        return true;
      case 'false':
        return false;
      default:
        break;
    }

    // Array
    if (this.isValidArray(trimmedValue)) {
      return JSON.parse(trimmedValue);
    }

    // DateTime (ISO 8601 format check)
    if (this.isValidDateTime(value.trim())) {
      return value.trim(); // Return as string, backend will parse it
    }

    // Number and String
    if (!isNaN(Number(trimmedValue))) {
      return Number(trimmedValue);
    }
    return value.trim();
  }

  /**
   * Validates if the input string is a valid JSON Array format
   * Supported formats:
   * - [1, 2, 3]
   * - ["hello", "world"]
   * - [true, false]
   * - [] (empty array)
   * - [{"key": "value"}] (array of objects)
   *
   * @param value The string to validate
   * @returns true if valid JSON array format, false otherwise
   */
  private isValidArray(value: string): boolean {
    // Must start with [ and end with ]
    if (!value.startsWith('[') || !value.endsWith(']')) {
      return false;
    }

    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed);
    } catch (error) {
      return false;
    }
  }

  /**
   * Validates if the input string is a valid ISO 8601 DateTime format
   * Supported formats:
   * - 2024-09-18T14:30:00Z (UTC)
   * - 2024-09-18T14:30:00+02:00 (with timezone)
   * - 2024-09-18T14:30:00.123Z (with milliseconds)
   * - 2024-09-18T14:30:00 (without timezone)
   *
   * @param value The string to validate
   * @returns true if valid ISO 8601 format, false otherwise
   */
  private isValidDateTime(value: string): boolean {
    // Basic ISO 8601 regex pattern
    const iso8601Pattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,3})?(Z|[+-]\d{2}:\d{2})?$/;

    if (!iso8601Pattern.test(value)) {
      return false;
    }

    // Try to parse with JavaScript Date to validate actual date values
    try {
      const date = new Date(value);
      return !isNaN(date.getTime());
    } catch (error) {
      return false;
    }
  }
  //#endregion
}
