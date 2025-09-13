import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/**
 * This component manages the modal for setting the values of a constant node
 */
@Component({
  selector: 'app-constant-node-modal',
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

    // Number and String
    if (!isNaN(Number(trimmedValue))) {
      return Number(trimmedValue);
    }
    return value.trim();
  }
  //#endregion
}
