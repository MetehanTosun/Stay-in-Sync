import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-set-rule-configuration-modal',
  standalone: true,
  imports: [FormsModule, CommonModule, Dialog, Button],
  templateUrl: './set-rule-configuration-modal.component.html',
  styleUrls: ['../modal-shared.component.css', './set-rule-configuration-modal.component.css']
})
export class SetRuleConfigurationModal {
  /** Controls dialog visibility (two-way binding with `visibleChange`) */
  @Input() visible = true;

  /** Emits when dialog visibility changes (two-way binding with `visible`) */
  @Output() visibleChange = new EventEmitter<boolean>();

  /** Current rule name */
  @Input() ruleName: string = '';

  /** Current rule description */
  @Input() ruleDescription: string = '';

  /** Emitted when the user saves the rule configuration (payload: { name, description }) */
  @Output() save = new EventEmitter<{name: string, description: string}>();

  /** Emitted when the modal is closed without saving */
  @Output() close = new EventEmitter<void>();

  //#region Modal Methods
  /**
   * Saves the new rule name and description
   */
  submit() {
    this.save.emit({
      name: this.ruleName,
      description: this.ruleDescription
    });
  }

  /**
   * Closes this modal
   */
  closeModal() {
    this.close.emit();
  }
  //#endregion
}
