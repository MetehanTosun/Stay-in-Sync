import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { delay, of, Observable } from 'rxjs';

interface Rule {
  id: number;
  name: string;
  status: 'Finalized' | 'Draft';
}

@Component({
  selector: 'app-rules-overview',
  imports: [FormsModule],
  templateUrl: './rules-overview.html',
  styleUrl: './rules-overview.scss'
})
export class RulesOverview implements OnInit {

  rules: Rule[] = [];
  isLoading = true;
  showCreateDialog = false;
  newRule = {
    name: '',
    pollingRate: 3,
    pollingUnit: 'Hours'
  };

  constructor(private router: Router) { }

  ngOnInit() {
    this.loadRules();
  }

  // sTODO REST
  loadRules() {
    this.isLoading = true;
    this.mockApiGetRules().subscribe({
      next: (rules) => {
        this.rules = rules;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading rules:', error);
        this.isLoading = false;
      }
    });
  }

  openCreateDialog() {
    this.showCreateDialog = true;
    this.newRule = {
      name: '',
      pollingRate: 3,
      pollingUnit: 'Hours'
    };
  }

  closeCreateDialog() {
    this.showCreateDialog = false;
  }

  createRule() {
    if (!this.newRule.name.trim()) {
      return;
    }

    const rule: Rule = {
      id: this.rules.length + 1,
      name: this.newRule.name,
      status: 'Draft',
    };

    this.rules.push(rule);
    this.closeCreateDialog();
    this.editRule(rule.id);
  }

  editRule(ruleId: number) {
    this.router.navigate(['/sync-rules/edit-rule', ruleId]);
  }

  deleteRule(ruleId: number) {
    if (confirm('Are you sure you want to delete this rule?')) {
      this.rules = this.rules.filter(rule => rule.id !== ruleId);
    }
  }

  // sTODO REST
  private mockApiGetRules(): Observable<Rule[]> {
    const mockRules: Rule[] = [
      {
        id: 1,
        name: 'Temp Check',
        status: 'Finalized',
      },
      {
        id: 2,
        name: 'Temp Check 2',
        status: 'Draft',
      },
      {
        id: 3,
        name: 'Sync Request',
        status: 'Finalized',
      },
      {
        id: 4,
        name: 'Daily Sync',
        status: 'Finalized',
      }
    ];

    return of(mockRules).pipe(delay(500));
  }
}
