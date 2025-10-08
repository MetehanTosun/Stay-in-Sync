import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { RuleCreationDTO } from '../../../models';

/**
 * This component manages the modal for creating a new sync rule
 */
@Component({
  selector: 'app-create-rule-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, Dialog, Button],
  templateUrl: './create-rule-modal.component.html',
  styleUrls: ['./create-rule-modal.component.css']
})
export class CreateRuleModalComponent {
  /**
   * Controls dialog visibility (two-way binding with `visibleChange`)
   */
  @Input() visible = false;

  /**
   * Emits when dialog visibility changes (two-way binding with `visible`)
   */
  @Output() visibleChange = new EventEmitter<boolean>();

  /**
   * Emitted when the user successfully submits the form.
   * Payload: RuleCreationDTO
   */
  @Output() created = new EventEmitter<RuleCreationDTO>();

  /**
   * Emitted when the user cancels the dialog (no payload).
   */
  @Output() cancelled = new EventEmitter<void>();

  /**
   * RuleCreationDTO containing the currently chosen rule data
   */
  newRule: RuleCreationDTO = { name: '', description: '' };

  /**
   * Validate the user input for a new rule and hide the modal if user input is valid
   */
  onSave() {
    if (!this.newRule.name?.trim() || !this.newRule.description?.trim()) {
      return;
    }
    this.created.emit({ ...this.newRule });
    this.hide();
  }

  /**
   * Cancel the rule creation and hide the modal
   */
  onCancel() {
    this.cancelled.emit();
    this.hide();
  }

  /**
   * Hides the dialog and emits `visibleChange` with `false`
   */
  hide() {
    this.visible = false;
    this.visibleChange.emit(false);
  }
}
