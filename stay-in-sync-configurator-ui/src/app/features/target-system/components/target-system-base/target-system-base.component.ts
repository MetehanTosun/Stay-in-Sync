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
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { CreateTargetSystemComponent } from '../create-target-system/create-target-system.component';
import { ManageTargetEndpointsComponent } from '../manage-target-endpoints/manage-target-endpoints.component';
import { ManageApiHeadersComponent } from '../../../source-system/components/manage-api-headers/manage-api-headers.component';
import { SearchBarComponent } from '../../../source-system/components/search-bar/search-bar.component';

@Component({
  standalone: true,
  selector: 'app-target-system-base',
  template: `
    <p-toolbar>
      <div class="p-toolbar-group-left">
        <h3>Target Systems</h3>
      </div>
      <div class="p-toolbar-group-right">
        <button pButton type="button" label="Create Target System" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>
    </p-toolbar>

    <app-search-bar
      [customPlaceholder]="'Search target systems by name, description, API URL...'"
      (search)="onSearchChange($event)"
      (clear)="onSearchClear()">
    </app-search-bar>

    <p-table [value]="displaySystems" [loading]="loading">
      <ng-template pTemplate="header">
        <tr>
          <th>Name</th>
          <th>API URL</th>
          <th>Description</th>
          <th>Actions</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-row>
        <tr>
          <td>{{ row.name }}</td>
            <td>
              <div class="api-url-cell">
                <div class="api-url">{{ row.apiUrl }}</div>
                <div class="api-type" *ngIf="row.apiType">
                  <span class="api-type-badge">{{ row.apiType }}</span>
                </div>
              </div>
            </td>
            <td>{{ row.description }}</td>
          <td>
            <button pButton label="Manage" class="p-button-text p-button-sm" (click)="manage(row)"></button>
            <button pButton icon="pi pi-trash" class="p-button-text p-button-danger" (click)="confirmDelete(row)"></button>
          </td>
        </tr>
      </ng-template>
    </p-table>

    <p-dialog [(visible)]="showDialog" [modal]="true" [style]="{width: '900px'}" [header]="dialogTitle">
      <form [formGroup]="form" class="p-fluid">
        <div class="p-field">
          <label for="name">Name</label>
          <input id="name" pInputText formControlName="name">
        </div>
        <div class="p-field">
          <label for="apiUrl">API URL</label>
          <input id="apiUrl" pInputText formControlName="apiUrl">
        </div>
        <div class="p-field">
          <label for="apiType">API Type</label>
          <input id="apiType" pInputText formControlName="apiType">
        </div>
        <div class="p-field">
          <label for="description">Description</label>
          <input id="description" pInputText formControlName="description">
        </div>
      </form>
      <ng-template pTemplate="footer">
        <button pButton label="Cancel" class="p-button-text" (click)="closeDialog()"></button>
        <button pButton label="Open Wizard" (click)="openWizard()" [disabled]="form.invalid"></button>
      </ng-template>
    </p-dialog>
    
    <app-create-target-system [(visible)]="wizardVisible"></app-create-target-system>

    <p-dialog [(visible)]="showDetailDialog" [modal]="true" [style]="{ width: '80vw', height: '80vh' }" header="Manage Target System">
      <div class="p-grid p-dir-col">
        <div class="p-col">
          <h3 class="p-mb-2">Metadata</h3>
          <form *ngIf="manageForm" [formGroup]="manageForm" class="p-fluid p-formgrid p-grid" (ngSubmit)="saveManagedMetadata()">
            <div class="p-field p-col-12 p-md-6">
              <label for="m_name">Name</label>
              <input id="m_name" pInputText formControlName="name" />
            </div>
            <div class="p-field p-col-12 p-md-6">
              <label for="m_apiUrl">API URL</label>
              <input id="m_apiUrl" pInputText formControlName="apiUrl" />
            </div>
            <!-- spacer row between metadata rows -->
            <div class="p-col-12" style="height: 1rem;"></div>
            <div class="p-field p-col-12 p-md-6">
              <label for="m_apiType">API Type</label>
              <input id="m_apiType" pInputText formControlName="apiType" />
            </div>
            <div class="p-field p-col-12">
              <label for="m_description">Description</label>
              <textarea id="m_description" pInputTextarea rows="3" formControlName="description"></textarea>
            </div>
            <div class="p-col-12">
              <button pButton type="submit" label="Save Metadata" [disabled]="manageForm.invalid"></button>
            </div>
          </form>
        </div>
        <div class="p-col p-mt-4">
          <h3 class="p-mb-2">API Headers</h3>
          <app-manage-api-headers *ngIf="selectedSystem" [syncSystemId]="selectedSystem.id!"></app-manage-api-headers>
        </div>
        <div class="p-col p-mt-4">
          <h3 class="p-mb-2">Endpoints</h3>
          <app-manage-target-endpoints *ngIf="selectedSystem" [targetSystemId]="selectedSystem.id!" (finish)="onManageFinished()"></app-manage-target-endpoints>
        </div>
      </div>
    </p-dialog>
    <app-confirmation-dialog
      [(visible)]="showConfirmationDialog"
      [data]="confirmationData"
      (confirmed)="onConfirmationConfirmed()"
      (cancelled)="onConfirmationCancelled()">
    </app-confirmation-dialog>
  `,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    ToolbarModule,
    ConfirmDialogModule,
    CreateTargetSystemComponent,
    ManageTargetEndpointsComponent,
    ManageApiHeadersComponent,
    SearchBarComponent,
    TextareaModule,
    ConfirmationDialogComponent
  ],
  providers: [ConfirmationService, MessageService],
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
      apiType: ['', Validators.required],
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
      message: `Are you sure you want to delete "${row.name}"?`,
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
      apiType: [row.apiType || '', Validators.required],
      description: [row.description || '']
    });
    this.showDetailDialog = true;
  }

  // Confirmation Dialog state & handlers
  showConfirmationDialog = false;
  confirmationData: ConfirmationDialogData = {
    title: 'Confirm Delete',
    message: 'Are you sure?',
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
}


