import { Component, OnInit } from '@angular/core';
import { TableModule } from 'primeng/table';
import { RuleCreationDTO, TransformationRule } from '../../models';
import { TransformationRulesApiService } from '../../service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import {Button} from 'primeng/button';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {Toolbar} from 'primeng/toolbar';
import {Tag} from 'primeng/tag';
import {Select} from 'primeng/select';

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
    Dialog,
    InputText,
    Toolbar,
    Tag,
    Select
  ],
  templateUrl: './rules-overview.component.html',
  styleUrl: './rules-overview.component.css'
})
export class RulesOverviewComponent implements OnInit {
  //#region Setup
  rules: TransformationRule[] = [];
  isLoading = true;
  showCreateRule = false;
  expandedRuleId: number | null = null;

  // Search
  searchTerm: string = '';

  // Input Data
  newRule: RuleCreationDTO = {
    name: '',
    description: ''
  };

  constructor(
    private router: Router,
    private rulesApi: TransformationRulesApiService
  ) { }

  ngOnInit() : void {
    this.loadRules();
  }
  //#endregion

  //#region Template Methods
  /**
   * Opens the modal to receive user input for the creation of a new rule
   */
  openCreateRuleModal() {
    this.showCreateRule = true;
    this.newRule = {
      name: '',
      description: ''
    };
  }

  /**
   * Closes the rule creation modal
   */
  closeCreateRuleModal() {
    this.showCreateRule = false;
  }

  /**
   * Navigates to the editor of the given rule
   * @param ruleId
   */
  editRule(ruleId: number) {
    this.router.navigate(['/sync-rules/edit-rule', ruleId]);
  }
  //#endregion

  //#region REST Methods
  /**
   * Creates a new rule with the name and description chosen by the user
   */
  createRule() {
    if (!this.isInputValid()) {
      alert("Invalid Input");// TODO-s err user
      return
    }

    this.rulesApi.createRule(this.newRule).subscribe({
      next: (res: HttpResponse<TransformationRule>) => {
        if (!res.body?.id) {
          throw new Error("Rule ID was not return - unable to forward to editor")
        }
        this.editRule(res.body?.id)
      },
      error: (err) => {
        alert(err.error?.message || err.message);
        console.log(err); // TODO-s err
      },
    })
  }

  /**
   * Loads all transformation rules from the backend
   */
  loadRules() {
    this.rulesApi.getRules().subscribe({
      next: (rules: TransformationRule[]) => {
        this.rules = rules;
      },
      error: (err) => {
        alert(err.error?.message || err.message);
        console.log(err); // TODO-s err
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  /**
   * Deletes a specific transformation rule
   * @param ruleId
   */
  deleteRule(ruleId: number) {
    this.rulesApi.deleteRule(ruleId).subscribe({
      next: () => {
        alert("deletion successful"); // TODO-s notify
      },
      error: (err) => {
        alert(err.error?.message || err.message);
        console.log(err); // TODO-s err
      }
    })
  }
  //#endregion

  //#region Helpers
  /**
   * Checks the current user input within the rule creation form
   * @returns true if the user input is valid
   */
  isInputValid(): boolean {
    if (this.newRule.name.trim() && this.newRule.description.trim()) {
      return true;
    }
    return false;
  }
  //#endregion
  selectedStatus: any;

  getSeverity(status: string): string {
    return status === 'FINALIZED' ? 'success' : 'warning';
  }
  /**
   * Returns the rules filtered by the search term
   */
  get filteredRules(): TransformationRule[] {
    if (!this.searchTerm.trim()) {
      return this.rules;
    }
    const term = this.searchTerm.trim().toLowerCase();
    return this.rules.filter(rule => rule.name.toLowerCase().includes(term));
  }
}
