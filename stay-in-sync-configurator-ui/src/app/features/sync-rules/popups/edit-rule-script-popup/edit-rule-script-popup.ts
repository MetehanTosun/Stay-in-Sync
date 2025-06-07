import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-edit-rule-script-popup',
  imports: [],
  templateUrl: './edit-rule-script-popup.html',
  styleUrls: ['./edit-rule-script-popup.css', '../_popup.css']
})
export class EditRuleScriptPopup {
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  save() {
    this.saved.emit();
  }

  close() {
    this.closed.emit();
  }
}
