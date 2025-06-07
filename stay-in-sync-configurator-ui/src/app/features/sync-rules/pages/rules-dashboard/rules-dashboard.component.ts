import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CreateRuleComponent } from '../../popups/create-rule/create-rule.component';
import { EditRuleSettingsComponent } from '../../popups/edit-rule-settings/edit-rule-settings.component';

interface Rule {
  name: string
  type: string
  pollingRate: number
  pollingRateUnit: string
}

@Component({
  selector: 'rules-dashboard',
  imports: [CommonModule, CreateRuleComponent, EditRuleSettingsComponent],
  templateUrl: './rules-dashboard.component.html',
  styleUrl: './rules-dashboard.component.css'
})
export class RulesDashboardComponent {
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
        this.router.navigate(['/sync-rules/edit', rule.name], {state: ( rule )});
        break;
      case "Time":
        this.showSettingsPopup = true;
        break;
    }
  }
}
