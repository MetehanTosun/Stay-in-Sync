import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-create-rule-popup',
  imports: [FormsModule],
  templateUrl: './create-rule-popup.html',
  styleUrls: ['./create-rule-popup.css', '../_popup.css']
})
export class CreateRulePopup {
  ruleName = '';
  ruleType = '';
  rulePollingRate = 1;
  rulePollingRateUnit = '';

  @Output() closed = new EventEmitter<void>();
  @Output() created = new EventEmitter<{
    name: string,
    type: string,
    pollingRate: number,
    pollingRateUnit: string
  }>();

  close() {
    this.closed.emit();
  }

  /* TODO: Assert polling rate is positive and all values are set and names/ids are unique*/
  create() {
    this.created.emit({
      name: this.ruleName,
      type: this.ruleType,
      pollingRate: this.rulePollingRate,
      pollingRateUnit: this.rulePollingRateUnit
  });
    this.closed.emit();
  }
}
