import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../../../source-system/components/confirmation-dialog/confirmation-dialog.component';
import { TabViewModule } from 'primeng/tabview';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { CreateTargetSystemComponent } from '../create-target-system/create-target-system.component';
import { ManageTargetEndpointsComponent } from '../manage-target-endpoints/manage-target-endpoints.component';
import { ManageApiHeadersComponent } from '../../../source-system/components/manage-api-headers/manage-api-headers.component';
import { AasManagementComponent } from '../aas-management/aas-management.component';
import { ToastModule } from 'primeng/toast';
import { Inplace } from 'primeng/inplace';

/**
 * Component responsible for displaying, creating, editing, and managing Target Systems.
 * Provides CRUD operations, search filtering, and integration with AAS and API configuration.
 */
@Component({
  standalone: true,
  selector: 'app-target-system-base',
  templateUrl: './target-system-base.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    CardModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    ConfirmDialogModule,
    TabViewModule,
    CreateTargetSystemComponent,
    ManageTargetEndpointsComponent,
    ManageApiHeadersComponent,
    TextareaModule,
    ConfirmationDialogComponent,
    AasManagementComponent,
    ToastModule,
    Inplace
  ],
  styleUrls: ['./target-system-base.component.css'],
  providers: [MessageService]
})
export class TargetSystemBaseComponent implements OnInit {
  systems: TargetSystemDTO[] = [];
  displaySystems: TargetSystemDTO[] = [];
  searchTerm: string = '';
  loading = false;
  showDialog = false;
  dialogTitle = 'New Target System';
  form!: FormGroup;
  editing: TargetSystemDTO | null = null;
  wizardVisible = false;
  showDetailDialog = false;
  selectedSystem: TargetSystemDTO | null = null;
  manageForm!: FormGroup;

  constructor(
    private api: TargetSystemResourceService,
    private fb: FormBuilder,
    private confirm: ConfirmationService,
    private messageService: MessageService,
  ) {}

  /**
   * Initializes forms for creating and managing Target Systems.
   * Loads all available systems on component startup.
   */
  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      apiType: ['', Validators.required],
      description: ['']
    });
    this.load();
    this.manageForm = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: ['']
    });
  }

  /**
   * Fetches all Target Systems from the backend and updates the display list.
   * Handles loading states and error suppression.
   */
  load(): void {
    this.loading = true;
    this.api.getAll().subscribe({
      next: list => { this.systems = list; this.applyFilter(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  /**
   * Updates the search term and filters the displayed systems accordingly.
   * @param term The text entered in the search field.
   */
  onSearchChange(term: string): void {
    this.searchTerm = term || '';
    this.applyFilter();
  }

  /** Clears the current search filter and restores the full system list. */
  onSearchClear(): void {
    this.searchTerm = '';
    this.applyFilter();
  }

  /**
   * Applies a case-insensitive filter to the list of Target Systems based on name, description, or URL.
   */
  private applyFilter(): void {
    const t = this.searchTerm.toLowerCase().trim();
    if (!t) { this.displaySystems = this.systems.slice(); return; }
    this.displaySystems = this.systems.filter(s =>
      (s.name || '').toLowerCase().includes(t) ||
      (s.description || '').toLowerCase().includes(t) ||
      (s.apiUrl || '').toLowerCase().includes(t) ||
      (s.apiType || '').toLowerCase().includes(t)
    );
  }

  /** Opens the creation wizard for a new Target System. */
  openCreate(): void {
    this.editing = null;
    this.wizardVisible = true;
  }

  /**
   * Opens the edit dialog prefilled with the selected Target System data.
   * @param row The Target System to edit.
   */
  edit(row: TargetSystemDTO): void {
    this.editing = row;
    this.dialogTitle = 'Target System bearbeiten';
    this.form.reset({
      name: row.name,
      apiUrl: row.apiUrl,
      apiType: row.apiType,
      description: row.description || ''
    });
    this.showDialog = true;
  }

  /**
   * Saves a Target System (either creating a new one or updating an existing one).
   * Automatically reloads the list after saving.
   */
  save(): void {
    const payload: TargetSystemDTO = { ...this.editing, ...this.form.value } as TargetSystemDTO;
    if (this.editing?.id) {
      this.api.update(this.editing.id, payload).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    } else {
      this.api.create(payload).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    }
  }

  /** Opens the creation wizard and closes any open dialog. */
  openWizard(): void {
    this.showDialog = false;
    this.wizardVisible = true;
  }

  /**
   * Opens a confirmation dialog before deleting a Target System.
   * @param row The Target System selected for deletion.
   */
  confirmDelete(row: TargetSystemDTO): void {
    this.systemToDelete = row;
    this.confirmationData = {
      title: 'Confirm Delete',
      message: `Are you sure you want to delete "${row.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'danger'
    };
    this.showConfirmationDialog = true;
  }

  /**
   * Deletes a Target System and shows a success notification on completion.
   * @param row The Target System to delete.
   */
  remove(row: TargetSystemDTO): void {
    if (!row.id) return;
    this.api.delete(row.id).subscribe({ next: () => { this.load(); this.messageService.add({ key: 'targetBase', severity: 'success', summary: 'Target System Deleted', detail: 'Target system has been removed.', life: 3000 }); } });
  }

  /** Closes the current dialog window without saving changes. */
  closeDialog(): void { this.showDialog = false; }

  /**
   * Opens the management dialog for the selected Target System.
   * Allows editing of metadata and configuration details.
   * @param row The Target System selected for management.
   */
  manage(row: TargetSystemDTO): void {
    this.selectedSystem = row;
    this.manageForm = this.fb.group({
      name: [row.name || '', Validators.required],
      apiUrl: [row.apiUrl || '', [Validators.required, Validators.pattern('https?://.+')]],
      description: [row.description || '']
    });
    this.showDetailDialog = true;
  }

  
  showConfirmationDialog = false;
  confirmationData: ConfirmationDialogData = {
    title: 'Confirm Delete',
    message: 'Are you sure you want to delete this target system?',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
    severity: 'danger'
  };
  systemToDelete: TargetSystemDTO | null = null;

  /**
   * Executes deletion logic after the user confirms the action in the confirmation dialog.
   */
  onConfirmationConfirmed() {
    if (this.systemToDelete) {
      this.remove(this.systemToDelete);
      this.systemToDelete = null;
    }
    this.showConfirmationDialog = false;
  }

  /** Cancels the deletion process and closes the confirmation dialog. */
  onConfirmationCancelled() {
    this.systemToDelete = null;
    this.showConfirmationDialog = false;
  }

  /**
   * Closes the management dialog and refreshes the Target System list.
   */
  onManageFinished(): void {
    this.showDetailDialog = false;
    this.selectedSystem = null;
    this.load();
  }

  /**
   * Updates the metadata (name, URL, description) of the currently selected Target System.
   * Reloads the system list after successful update.
   */
  saveManagedMetadata(): void {
    if (!this.selectedSystem || this.manageForm.invalid) return;
    const payload: TargetSystemDTO = { ...this.selectedSystem, ...this.manageForm.value } as TargetSystemDTO;
    this.api.update(this.selectedSystem.id!, payload).subscribe({ next: () => { this.load(); } });
  }
  
  /**
   * Determines if the selected Target System is of type AAS.
   * @returns True if the API type is 'AAS', otherwise false.
   */
  isAasSelected(): boolean {
    const t = (this.selectedSystem?.apiType || '').trim().toUpperCase();
    return t === 'AAS';
  }

  /** Reloads the Target System list after AAS-related updates. */
  onAasRefreshRequested(): void {
    this.load();
  }

  /** Reloads the system list after a new Target System has been created. */
  onCreated(_created: TargetSystemDTO): void {
    this.load();
  }

  
  isSearchActive: boolean = false;

  /**
   * Returns a context-dependent message for the empty table state.
   * @returns Message depending on whether a search is active or not.
   */
  getEmptyMessage(): string {
    if (this.isSearchActive) {
      return 'No matching target systems found';
    }
    return 'No target systems available';
  }

  /** Displays a success toast message after a header is created. */
  onHeaderCreated(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Created',
      detail: 'Header has been successfully created.',
      life: 3000
    });
  }

  /** Displays a success toast message after a header is deleted. */
  onHeaderDeleted(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Deleted',
      detail: 'Header has been successfully deleted.',
      life: 3000
    });
  }

  
  private originalSystem: TargetSystemDTO | null = null;

  onInplaceActivate(): void {
    if (this.selectedSystem) {
      this.originalSystem = { ...this.selectedSystem };
    }
  }

  onSave(closeCallback: () => void): void {
    if (!this.selectedSystem?.id) return;
    this.api.update(this.selectedSystem.id, this.selectedSystem).subscribe({
      next: () => {
        closeCallback();
        this.load();
        this.messageService.add({
          key: 'targetBase',
          severity: 'success',
          summary: 'Updated',
          detail: 'Target System updated successfully.',
          life: 3000
        });
      },
      error: () => {
        if (this.originalSystem) {
          Object.assign(this.selectedSystem!, this.originalSystem);
        }
        closeCallback();
      }
    });
  }

  onClose(closeCallback: () => void): void {
    if (this.selectedSystem && this.originalSystem) {
      Object.assign(this.selectedSystem, this.originalSystem);
    }
    closeCallback();
  }

  
  aasTestLoading = false;
  aasTestError: string | null = null;

  aasTest(): void {
    if (!this.selectedSystem?.id) return;
    this.aasTestLoading = true;
    this.aasTestError = null;
    
    
    
    this.api.update(this.selectedSystem.id, this.selectedSystem).subscribe({
      next: () => {
        this.aasTestLoading = false;
        this.messageService.add({
          key: 'targetBase',
          severity: 'success',
          summary: 'AAS ID saved',
          detail: 'AAS ID has been updated.',
          life: 3000
        });
      },
      error: (err) => {
        this.aasTestLoading = false;
        this.aasTestError = err?.message || 'Test failed';
      }
    });
  }
}
