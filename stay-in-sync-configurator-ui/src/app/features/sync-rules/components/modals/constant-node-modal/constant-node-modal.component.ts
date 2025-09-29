import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/**
 * This component manages the modal for setting the values of a constant node
 */
@Component({
  selector: 'app-constant-node-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './constant-node-modal.component.html',
  styleUrl: './constant-node-modal.component.css'
})
export class ConstantNodeModalComponent {
  @Input() currentValue: string = '';
  @Output() constantCreated = new EventEmitter<any>();
  @Output() save = new EventEmitter<string>();
  @Output() modalsClosed = new EventEmitter<void>();

  constantValue: string = '';

  ngOnInit() {
    this.constantValue = this.currentValue || '';
  }

  //#region Modal Methods
  /**
   * Concludes the constant node creation by forwarding the constant value to node creation
   *
   * @returns
   */
  submit() {
    if (!this.constantValue.trim()) {
      alert('Please enter a constant value');
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

  /**
   * Helper method to get current datetime in ISO 8601 format for user reference
   * Can be used in the UI to show an example
   */
  getCurrentDateTimeExample(): string {
    return new Date().toISOString();
  }
  //#endregion
}
