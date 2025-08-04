import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface TransformationRule {
  id: number;
  name: string;
  description: string;
  graphStatus: 'FINALIZED' | 'DRAFT';
  transformationId: number; // s*TODO "The ID of the parent transformation this rule belongs to"
}

interface RuleCreationDTO {
  name: string;
  description: string;
}

@Component({
  selector: 'app-rules-overview',
  imports: [FormsModule],
  templateUrl: './rules-overview.html',
  styleUrl: './rules-overview.scss'
})
export class RulesOverview implements OnInit {

  rules: TransformationRule[] = [];
  isLoading = true;
  showCreateDialog = false;
  newRule = {
    name: '',
    description: ''
  };

  constructor(private router: Router, private http: HttpClient) { }

  ngOnInit() {
    this.loadRules();
  }

  // Load rules from the backend API
  loadRules() {
    this.isLoading = true;
    this.http.get<TransformationRule[]>('/api/config/transformation-rule').subscribe({
      next: (rules) => {
        this.rules = rules.map(rule => ({
          id: rule.id,
          name: rule.name,
          description: rule.description,
          graphStatus: rule.graphStatus,
          transformationId: rule.transformationId
        }));
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
      description: '', // sTODO add to client ui
    };
  }

  closeCreateDialog() {
    this.showCreateDialog = false;
  }

  createRule() {
    if (!this.newRule.name.trim() || !this.newRule.description.trim()) {
      return; // Errorhandling
    }

    const newTransformationRule: RuleCreationDTO = {
      name: this.newRule.name,
      description: this.newRule.description,
    }

    this.http.post('/api/config/transformation-rule', newTransformationRule, { observe: 'response' }).subscribe({
      next: (response) => {
        // Backend returns 201 Created with Location header
        const locationHeader = response.headers.get('Location');
        if (locationHeader) {
          // Extract ID from location header (e.g., "/api/config/transformation-rule/123")
          const idMatch = locationHeader.match(/\/(\d+)$/);
          if (idMatch) {
            const newRuleId = parseInt(idMatch[1], 10);
            this.loadRules();
            this.closeCreateDialog();
            // Navigate to the newly created rule
            this.editRule(newRuleId);
            return;
          }
        }

        // Fallback: reload rules
        this.loadRules();
        this.closeCreateDialog();
      },
      error: (error) => {
        console.error('Error creating rule:', error); // sTODO ErrorHandling
      }
    });
  }

  editRule(ruleId: number) {
    this.router.navigate(['/sync-rules/edit-rule', ruleId]);
  }

  deleteRule(ruleId: number) {
    if (confirm('Are you sure you want to delete this rule?')) {
      this.http.delete(`/api/config/transformation-rule/${ruleId}`).subscribe({
        next: () => {
          this.rules = this.rules.filter(rule => rule.id !== ruleId);
        },
        error: (error) => {
          console.error('Error deleting rule:', error); // sTODO Error handling
        }
      });
    }
  }
}
