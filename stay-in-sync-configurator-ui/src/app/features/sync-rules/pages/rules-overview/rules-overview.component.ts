import { Component, OnInit } from '@angular/core';
import { TableModule } from 'primeng/table';
import { RuleCreationDTO, TransformationRule } from '../../models';
import { TransformationRulesApiService } from '../../service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { CreateRuleModalComponent } from '../../components/modals/create-rule-modal/create-rule-modal.component';
import { MessageService } from 'primeng/api';

/**
 * The page component responsible for viewing the list of transformation rule graphs
 */
@Component({
  selector: 'app-rules-overview',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    Button,
    Tag,
    Select,
    CreateRuleModalComponent
  ],
  templateUrl: './rules-overview.component.html',
  styleUrls: ['./rules-overview.component.css']
})
export class RulesOverviewComponent implements OnInit {
  //#region Fields
  rules: TransformationRule[] = [];
  isLoading = true;
  showCreateRule = false; // bound to the extracted modal's `visible`
  expandedRuleId: number | null = null;

  // Search and filter
  searchTerm = '';
  statusOptions = [
    { label: 'Draft', value: 'DRAFT' },
    { label: 'Finalized', value: 'FINALIZED' }
  ];
  //#endregion

  constructor(
    private router: Router,
    private rulesApi: TransformationRulesApiService,
    private messageService: MessageService
  ) { }

  //#region Lifecycle
  /**
   * Initialize the page by loading the available rules.
   */
  ngOnInit(): void {
    this.loadRules();
  }
  //#endregion

  //#region UI Events
  /**
   * Handler for the `(created)` event emitted by the Create Rule modal.
   * Forwards to the API to create the rule.
   * On success navigates to the newly created rule's editor.
   */
  onCreateRule(dto: RuleCreationDTO) {
    this.createRule(dto);
  }
  //#endregion

  //#region Navigation
  /**
   * Navigate to the rule editor for the given rule id.
   */
  editRule(ruleId: number) {
    this.router.navigate(['/sync-rules/edit-rule', ruleId]);
  }
  //#endregion

  //#region REST Methods
  /**
   * Load all transformation rules from the backend and update component state.
   */
  loadRules() {
    this.rulesApi.getRules().subscribe({
      next: (rules: TransformationRule[]) => (this.rules = rules),
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Loading Rules',
          detail: 'An error accurred while loading the rules. \n Please check the logs or console.'
        });
        console.error(err);
      },
      complete: () => (this.isLoading = false)
    });
  }

  /**
   * Creates a new rule with the data from the given DTO
   */
  createRule(dto: RuleCreationDTO) {
    this.rulesApi.createRule(dto).subscribe({
      next: (res: HttpResponse<TransformationRule>) => {
        if (!res.body?.id) {
          throw new Error('Rule ID was not return - unable to forward to editor');
        }
        this.editRule(res.body.id!);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Creating Rule',
          detail: 'An error accurred while creating a rule. \n Please check the logs or console.'
        });
        console.error(err);
      }
    });
  }

  /**
   * Deletes a specific transformation rule and reloads the list on success.
   */
  deleteRule(ruleId: number) {
    this.rulesApi.deleteRule(ruleId).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Deleting Rule',
          detail: 'Rule was successfully deleted'
        });
        this.loadRules();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Deleting Rule',
          detail: 'An error accurred while deleting a rule. \n Please check the logs or console.'
        });
        console.error(err);
      }
    });
  }
  //#endregion

  //#region Helpers
  /**
   * Map backend status to PrimeNG tag severity.
   */
  getSeverity(status: string): string {
    return status === 'FINALIZED' ? 'success' : 'warning';
  }

  /**
   * Returns the rules filtered by the active search term.
   */
  get filteredRules(): TransformationRule[] {
    if (!this.searchTerm.trim()) {
      return this.rules;
    }
    const term = this.searchTerm.trim().toLowerCase();
    return this.rules.filter((rule) => rule.name.toLowerCase().includes(term));
  }
  //#endregion
}
