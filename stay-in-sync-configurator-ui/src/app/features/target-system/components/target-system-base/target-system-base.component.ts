import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ToolbarModule } from 'primeng/toolbar';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../../../source-system/components/confirmation-dialog/confirmation-dialog.component';
import { TabViewModule } from 'primeng/tabview';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { CreateTargetSystemComponent } from '../create-target-system/create-target-system.component';
import { ManageTargetEndpointsComponent } from '../manage-target-endpoints/manage-target-endpoints.component';
import { ManageApiHeadersComponent } from '../../../source-system/components/manage-api-headers/manage-api-headers.component';
import { SearchBarComponent } from '../../../source-system/components/search-bar/search-bar.component';
import { AasManagementComponent } from '../aas-management/aas-management.component';

@Component({
  standalone: true,
  selector: 'app-target-system-base',
  templateUrl: './target-system-base.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
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
    SearchBarComponent,
    TextareaModule,
    ConfirmationDialogComponent,
    AasManagementComponent
  ],
  styleUrls: ['./target-system-base.component.css']
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

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      apiType: ['', Validators.required],
      description: ['']
    });
    this.load();
    // initialize manageForm to avoid undefined on first render
    this.manageForm = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: ['']
    });
  }

  load(): void {
    this.loading = true;
    this.api.getAll().subscribe({
      next: list => { this.systems = list; this.applyFilter(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  onSearchChange(term: string): void {
    this.searchTerm = term || '';
    this.applyFilter();
  }

  onSearchClear(): void {
    this.searchTerm = '';
    this.applyFilter();
  }

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

  openCreate(): void {
    this.editing = null;
    this.wizardVisible = true;
  }

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

  save(): void {
    const payload: TargetSystemDTO = { ...this.editing, ...this.form.value } as TargetSystemDTO;
    if (this.editing?.id) {
      this.api.update(this.editing.id, payload).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    } else {
      this.api.create(payload).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    }
  }

  openWizard(): void {
    this.showDialog = false;
    this.wizardVisible = true;
  }

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

  remove(row: TargetSystemDTO): void {
    if (!row.id) return;
    this.api.delete(row.id).subscribe({ next: () => this.load() });
  }

  closeDialog(): void { this.showDialog = false; }

  manage(row: TargetSystemDTO): void {
    this.selectedSystem = row;
    this.manageForm = this.fb.group({
      name: [row.name || '', Validators.required],
      apiUrl: [row.apiUrl || '', [Validators.required, Validators.pattern('https?://.+')]],
      description: [row.description || '']
    });
    this.showDetailDialog = true;
  }

  // Confirmation Dialog state & handlers
  showConfirmationDialog = false;
  confirmationData: ConfirmationDialogData = {
    title: 'Confirm Delete',
    message: 'Are you sure you want to delete this target system?',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
    severity: 'danger'
  };
  systemToDelete: TargetSystemDTO | null = null;

  onConfirmationConfirmed() {
    if (this.systemToDelete) {
      this.remove(this.systemToDelete);
      this.systemToDelete = null;
    }
    this.showConfirmationDialog = false;
  }

  onConfirmationCancelled() {
    this.systemToDelete = null;
    this.showConfirmationDialog = false;
  }

  onManageFinished(): void {
    this.showDetailDialog = false;
    this.selectedSystem = null;
    this.load();
  }

  saveManagedMetadata(): void {
    if (!this.selectedSystem || this.manageForm.invalid) return;
    const payload: TargetSystemDTO = { ...this.selectedSystem, ...this.manageForm.value } as TargetSystemDTO;
    this.api.update(this.selectedSystem.id!, payload).subscribe({ next: () => { this.load(); } });
  }
  // AAS-related methods
  isAasSelected(): boolean {
    const t = (this.selectedSystem?.apiType || '').trim().toUpperCase();
    return t === 'AAS';
  }

  onAasRefreshRequested(): void {
    this.load();
  }

  // Empty state functionality
  isSearchActive: boolean = false;

  getEmptyMessage(): string {
    if (this.isSearchActive) {
      return 'No matching target systems found';
    }
    return 'No target systems available';
  }

  onHeaderCreated(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Created',
      detail: 'Header has been successfully created.',
      life: 3000
    });
  }

  onHeaderDeleted(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Deleted',
      detail: 'Header has been successfully deleted.',
      life: 3000
    });
  }
}


