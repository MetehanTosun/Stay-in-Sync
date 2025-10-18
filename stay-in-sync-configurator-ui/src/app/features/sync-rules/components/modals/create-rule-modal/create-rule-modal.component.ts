import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { RuleCreationDTO, TransformationRule } from '../../../models';
import { MessageService } from 'primeng/api';

/**
 * This component manages the modal for creating a new sync rule
 */
@Component({
  selector: 'app-create-rule-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, Dialog, Button],
  templateUrl: './create-rule-modal.component.html',
  styleUrls: ['../modal-shared.component.css', './create-rule-modal.component.css']
})
export class CreateRuleModalComponent {
  //#region Fields
  /**
   * Controls dialog visibility (two-way binding with `visibleChange`)
   */
  @Input() visible = false;

  /**
   * Emits when dialog visibility changes (two-way binding with `visible`)
   */
  @Output() visibleChange = new EventEmitter<boolean>();

  /**
   * List of current rules. Used to check rule name availability
   */
  @Input() rules: TransformationRule[] = [];

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
  //#endregion

  constructor(private messageService: MessageService) { }

  //#region Handlers
  /**
   * Validate the user input for a new rule and hide the modal if user input is valid
   */
  onSave() {
    if (this.nameIsEmtpy() || !this.nameIsAvailable()) return

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
  //#endregion

  //#region Helpers
  /**
   * Hides the dialog and emits `visibleChange` with `false`
   */
  private hide() {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  /**
   * Returns true if rule name is empty
   */
  private nameIsEmtpy(): boolean {
    if (!this.newRule.name?.trim()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Invalid Rule Name',
        detail: `Please enter a rule name`
      });
      return true
    }
    return false
  }

  /**
   * Returns true if rule name is unique
   */
  private nameIsAvailable(): boolean {
    const existingRule = this.rules.find(rule => rule.name.trim().toLowerCase() === this.newRule.name.trim().toLowerCase());
    if (existingRule) {
      this.messageService.add({
        severity: 'error',
        summary: 'Name Conflict',
        detail: `A rule with the name "${this.newRule.name}" already exists.`
      });
      return false;
    }
    return true;
  }
  //#endregion
}
