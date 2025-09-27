import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-rule-configuration',
  imports: [FormsModule],
  templateUrl: './rule-configuration.component.html',
  styleUrl: './rule-configuration.component.css'
})
export class RuleConfigurationComponent {
  @Input() ruleName: string = '';
  @Input() ruleDescription: string = '';

  @Output() save = new EventEmitter<{name: string, description: string}>();
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
