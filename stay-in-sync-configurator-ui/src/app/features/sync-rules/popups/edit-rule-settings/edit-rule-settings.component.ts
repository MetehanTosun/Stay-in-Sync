import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-edit-rule-settings',
  imports: [FormsModule],
  templateUrl: './edit-rule-settings.component.html',
  styleUrls: ['./edit-rule-settings.component.css', '../_popup.css']
})
export class EditRuleSettingsComponent {
  ruleName = '';
  ruleType = '';
  rulePollingRate = '';
  rulePollingRateUnit = '';

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  save() {
    this.saved.emit();
  }

  close() {
    this.closed.emit();
  }
}
