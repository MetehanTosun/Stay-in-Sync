import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-edit-rule-settings',
  imports: [FormsModule],
  templateUrl: './edit-rule-settings.popup.html',
  styleUrls: ['./edit-rule-settings.popup.css', '../_popup.css']
})
export class EditRuleSettingsPopup {
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
