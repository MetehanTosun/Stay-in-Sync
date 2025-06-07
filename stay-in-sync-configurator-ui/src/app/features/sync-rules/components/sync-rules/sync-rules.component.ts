import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CreateRulePopup } from '../../popups/create-rule/create-rule.popup';
import { EditRuleSettingsPopup } from '../../popups/edit-rule-settings/edit-rule-settings.popup';

interface Rule {
  name: string
  type: string
  pollingRate: number
  pollingRateUnit: string
}

@Component({
  selector: 'sync-rules',
  imports: [CommonModule, CreateRulePopup, EditRuleSettingsPopup],
  templateUrl: './sync-rules.component.html',
  styleUrl: './sync-rules.component.css'
})

export class SyncRulesComponent {
  protected title = 'stay-in-sync-polling-logic-ui';
  showCreationPopup = false;
  showSettingsPopup = false;
  rules: Rule[] = [];

  addRule(rule: Rule) {
    this.rules.push(rule);
  }

  constructor(private router: Router) { }

  editRule(rule: Rule) {
    switch (rule.type) {
      case "Graph":
        this.router.navigate(['/edit', rule.name], {state: ( rule )});
        break;
      case "Time":
        this.showSettingsPopup = true;
        break;
    }
  }
}
