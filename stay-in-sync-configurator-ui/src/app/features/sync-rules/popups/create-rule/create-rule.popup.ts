import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-create-rule',
  imports: [FormsModule],
  templateUrl: './create-rule.popup.html',
  styleUrls: ['./create-rule.popup.css', '../_popup.css']
})
export class CreateRulePopup {
  name = '';
  type = '';
  pollingRate = 1;
  pollingRateUnit = '';

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

  /* TODO: Assert id (maybe name) is unique*/
  create() {
    if (!this.name || !this.type ||  (!this.pollingRate && this.pollingRate != 0) || !this.pollingRateUnit) {
      alert('Please fill in all fields.');
      return;
    }
    if (this.pollingRate < 1) {
      alert('Polling rate must be positive.');
      return;
    }
    if (this.type !== 'Graph' && this.type !== 'Time') {
      alert('Invalid rule type. Please select either "Graph" or "Time".');
      return;
    }
    if (!['seconds', 'minutes', 'hours'].includes(this.pollingRateUnit)) {
      alert('Invalid polling rate unit. Please select either "seconds", "minutes", or "hours".');
      return;
    }

    this.created.emit({
      name: this.name.trim(),
      type: this.type,
      pollingRate: this.pollingRate,
      pollingRateUnit: this.pollingRateUnit
  });
    this.closed.emit();
  }
}
