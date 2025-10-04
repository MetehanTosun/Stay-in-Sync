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
import { TabViewModule } from 'primeng/tabview';
import { TreeModule } from 'primeng/tree';
import { TreeNode } from 'primeng/api';
import { ConfirmationService, MessageService } from 'primeng/api';
import { HttpClient } from '@angular/common/http';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../../../source-system/components/confirmation-dialog/confirmation-dialog.component';
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
  template: `
    <p-card>
      <app-search-bar
        [customPlaceholder]="'Search target systems by name, description, API URL...'"
        (search)="onSearchChange($event)"
        (clear)="onSearchClear()">
      </app-search-bar>

    <p-toolbar>
      <div class="p-toolbar-group-left">
        <h3>Target Systems</h3>
      </div>
      <div class="p-toolbar-group-right">
        <button pButton type="button" label="Create Target System" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>
    </p-toolbar>

    <p-table [value]="displaySystems" [loading]="loading" [paginator]="true" [rows]="10" [responsiveLayout]="'scroll'">
      <ng-template pTemplate="header">
        <tr>
          <th style="min-width: 200px;">Name</th>
          <th style="min-width: 250px;">API URL</th>
          <th style="min-width: 300px;">Description</th>
          <th style="min-width: 150px;">Actions</th>
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

      <!-- Empty state message when no data is available -->
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="4">
            <div class="empty-state">
              <div class="empty-icon">
                <i class="pi pi-search" *ngIf="isSearchActive"></i>
                <i class="pi pi-database" *ngIf="!isSearchActive"></i>
              </div>
              <div class="empty-message">
                <h4>{{ getEmptyMessage() }}</h4>
                <p *ngIf="isSearchActive">
                  No target systems found matching "{{ searchTerm }}"
                </p>
                <p *ngIf="!isSearchActive">
                  No target systems available. Create your first target system to get started.
                </p>
              </div>
              <div class="empty-actions" *ngIf="isSearchActive">
                <button pButton
                        type="button"
                        label="Clear Search"
                        class="p-button-text"
                        (click)="onSearchClear()">
                </button>
                <button pButton
                        type="button"
                        label="Show All"
                        class="p-button-text"
                        (click)="onSearchClear()">
                </button>
              </div>
              <div class="empty-actions" *ngIf="!isSearchActive">
                <button pButton
                        type="button"
                        label="Create Target System"
                        icon="pi pi-plus"
                        (click)="openCreate()">
                </button>
              </div>
            </div>
          </td>
        </tr>
      </ng-template>
    </p-table>

    <p-dialog [(visible)]="showDialog" [modal]="true" [style]="{width: '900px'}" [header]="dialogTitle">
      <form [formGroup]="form" class="p-fluid">
        <div class="p-field">
          <label for="name">Name*</label>
          <input id="name" pInputText formControlName="name">
        </div>
        <div class="p-field">
          <label for="apiUrl">API URL*</label>
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
    
    <app-create-target-system [(visible)]="wizardVisible" (created)="onCreated($event)"></app-create-target-system>

    <p-dialog [(visible)]="showDetailDialog" [modal]="true" [style]="{ width: '80vw', height: '80vh' }" header="Manage Target System">
      <div class="p-grid p-dir-col">
        <div class="p-col" style="margin-bottom: 2rem;">
          <h3 style="margin-bottom: 1.5rem; font-size: 1.25rem; font-weight: 600;">Metadata</h3>
          <form *ngIf="manageForm" [formGroup]="manageForm" class="p-fluid" (ngSubmit)="saveManagedMetadata()">
            <div class="p-field" style="margin-bottom: 1rem;">
              <label for="m_name" style="font-weight: 500; margin-bottom: 0.5rem; display: block;">Name*</label>
              <input id="m_name" pInputText formControlName="name" style="width: 100%;" />
            </div>
            <div class="p-field" style="margin-bottom: 1rem;">
              <label for="m_apiUrl" style="font-weight: 500; margin-bottom: 0.5rem; display: block;">API URL*</label>
              <input id="m_apiUrl" pInputText formControlName="apiUrl" style="width: 100%;" />
            </div>
            <div class="p-field" style="margin-bottom: 1rem;">
              <label for="m_description" style="font-weight: 500; margin-bottom: 0.5rem; display: block;">Description</label>
              <textarea id="m_description" pInputTextarea rows="3" formControlName="description" style="width: 100%;"></textarea>
            </div>
            <div class="p-field" style="margin-bottom: 1rem;">
              <button pButton type="submit" label="Save Metadata" [disabled]="manageForm.invalid"></button>
            </div>
          </form>
        </div>
        <p-tabView>
          <p-tabPanel *ngIf="!isAasSelected()" header="Headers & Endpoints">
            <div class="p-grid" style="margin: 1rem 0;">
              <div class="p-col-12 p-md-6" style="padding: 0.5rem;">
                <p-card [style]="{'height': '100%'}">
                  <h4 style="margin-bottom: 1rem; font-size: 1.125rem; font-weight: 600;">API Headers</h4>
                  <app-manage-api-headers *ngIf="selectedSystem" [syncSystemId]="selectedSystem.id!" (onCreated)="onHeaderCreated()" (onDeleted)="onHeaderDeleted()"></app-manage-api-headers>
                </p-card>
              </div>
              <div class="p-col-12 p-md-6" style="padding: 0.5rem;">
                <p-card [style]="{'height': '100%'}">
                  <h4 style="margin-bottom: 1rem; font-size: 1.125rem; font-weight: 600;">Target Endpoints</h4>
                  <app-manage-target-endpoints *ngIf="selectedSystem" [targetSystemId]="selectedSystem.id!" (finish)="onManageFinished()"></app-manage-target-endpoints>
                </p-card>
              </div>
            </div>
          </p-tabPanel>
          <p-tabPanel *ngIf="isAasSelected()" header="AAS Management">
            <app-aas-management 
              [system]="selectedSystem" 
              (refreshRequested)="onAasRefreshRequested()">
            </app-aas-management>
          </p-tabPanel>
        </p-tabView>
      </div>
    </p-dialog>
    <app-confirmation-dialog
      [(visible)]="showConfirmationDialog"
      [data]="confirmationData"
      (confirmed)="onConfirmationConfirmed()"
      (cancelled)="onConfirmationCancelled()">
    </app-confirmation-dialog>
    </p-card>
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
    TabViewModule,
    TreeModule,
    CreateTargetSystemComponent,
    ManageTargetEndpointsComponent,
    ManageApiHeadersComponent,
    SearchBarComponent,
    TextareaModule,
    ConfirmationDialogComponent,
    AasManagementComponent
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

  // AAS-related properties

  constructor(
    private api: TargetSystemResourceService,
    private fb: FormBuilder,
    private confirm: ConfirmationService,
    private http: HttpClient,
    private messageService: MessageService,
    private errorService: HttpErrorService
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
      next: list => { 
        this.systems = list; 
        this.applyFilter(); 
        this.loading = false; 
      },
      error: (err) => { 
        console.error('[TargetSystemBase] load: Error loading systems', err);
        this.loading = false; 
      }
    });
  }

  onSearchChange(term: string): void {
    this.searchTerm = term || '';
    this.isSearchActive = this.searchTerm.trim().length > 0;
    this.applyFilter();
  }

  onSearchClear(): void {
    this.searchTerm = '';
    this.isSearchActive = false;
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

  onCreated(system: TargetSystemDTO): void {
    // refresh list immediately so the newly created system appears without manual reload
    this.load();
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

  // AAS-related methods
  isAasSelected(): boolean {
    return this.selectedSystem?.apiType === 'AAS';
  }

  onAasRefreshRequested(): void {
    this.load();
  }

  // Empty state functionality
  isSearchActive: boolean = false;

  /**
   * Get appropriate empty state message
   * @returns Message to display when no results are found
   */
  getEmptyMessage(): string {
    if (this.isSearchActive) {
      return 'No matching target systems found';
    }
    return 'No target systems available';
  }

  /**
   * Handle header creation event
   */
  onHeaderCreated(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Created',
      detail: 'Header has been successfully created.',
      life: 3000
    });
  }

  /**
   * Handle header deletion event
   */
  onHeaderDeleted(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Deleted',
      detail: 'Header has been successfully deleted.',
      life: 3000
    });
  }
}
