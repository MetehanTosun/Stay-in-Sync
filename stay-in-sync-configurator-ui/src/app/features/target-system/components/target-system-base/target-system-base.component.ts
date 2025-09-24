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
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../../../source-system/components/confirmation-dialog/confirmation-dialog.component';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { CreateTargetSystemComponent } from '../create-target-system/create-target-system.component';
import { ManageTargetEndpointsComponent } from '../manage-target-endpoints/manage-target-endpoints.component';
import { ManageApiHeadersComponent } from '../../../source-system/components/manage-api-headers/manage-api-headers.component';
import { SearchBarComponent } from '../../../source-system/components/search-bar/search-bar.component';

interface AasOperationVarView { idShort: string; modelType?: string; valueType?: string }
interface AasAnnotationView { idShort: string; modelType?: string; valueType?: string; value?: any }
interface AasElementLivePanel {
  label: string;
  type: string;
  value?: any;
  valueType?: string;
  min?: any;
  max?: any;
  inputVariables?: AasOperationVarView[];
  outputVariables?: AasOperationVarView[];
  inoutputVariables?: AasOperationVarView[];
  firstRef?: string;
  secondRef?: string;
  annotations?: AasAnnotationView[];
}

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
    
    <app-create-target-system [(visible)]="wizardVisible" (created)="onCreated($event)"></app-create-target-system>

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
        <p-tabView>
          <p-tabPanel *ngIf="!isAasSelected()" header="Headers & Endpoints">
            <div class="p-grid p-m-2">
              <div class="p-col-12 p-md-6">
                <p-card>
                  <h4>API Headers</h4>
          <app-manage-api-headers *ngIf="selectedSystem" [syncSystemId]="selectedSystem.id!"></app-manage-api-headers>
                </p-card>
        </div>
              <div class="p-col-12 p-md-6">
                <p-card>
                  <h4>Endpoints</h4>
          <app-manage-target-endpoints *ngIf="selectedSystem" [targetSystemId]="selectedSystem.id!" (finish)="onManageFinished()"></app-manage-target-endpoints>
                </p-card>
              </div>
            </div>
          </p-tabPanel>
          <p-tabPanel *ngIf="isAasSelected()" header="AAS Management">
            <div class="p-d-flex p-ai-center p-jc-between" style="margin-bottom: .5rem;">
              <div>
                <strong>Base URL:</strong> {{ selectedSystem?.apiUrl }}
                <span class="ml-3"><strong>AAS ID:</strong> {{ selectedSystem?.aasId || '-' }}</span>
              </div>
              <div>
                <button pButton type="button" label="Refresh Snapshot" class="p-button-text" (click)="discoverAasSnapshot()" [disabled]="aasTreeLoading"></button>
                <button pButton type="button" label="+ Submodel" class="p-button-text" (click)="openAasCreateSubmodel()"></button>
              </div>
            </div>
            <button pButton type="button" label="Load Snapshot" (click)="discoverAasSnapshot()" [disabled]="aasTreeLoading"></button>
            <span *ngIf="aasTreeLoading" class="ml-2">Loading...</span>
            <div style="display:flex; gap:1rem; align-items:flex-start;">
              <div style="flex: 1 1 65%; min-width: 0;">
                <p-tree [value]="aasTreeNodes" (onNodeExpand)="onAasNodeExpand($event)" (onNodeSelect)="onAasNodeSelect($event)" selectionMode="single">
                  <ng-template let-node pTemplate="default">
                    <div style="display:flex;align-items:center;gap:.5rem;">
                      <span>{{ node.label }}</span>
                      <span style="font-size:.75rem;padding:.1rem .4rem;border-radius:999px;border:1px solid var(--surface-border);color:var(--text-color-secondary);">
                        {{ node.data?.type==='submodel' ? (node.data?.modelType || (node.data?.raw?.kind?.toLowerCase?.().includes('template') ? 'Submodel Template' : 'Submodel')) : (node.data?.modelType || node.data?.raw?.modelType || (node.data?.raw?.valueType ? 'Property' : 'Element')) }}
                      </span>
                      <button *ngIf="node.data?.type==='submodel'" pButton type="button" class="p-button-text" label="Create element" (click)="openAasCreateElement(node.data.id)"></button>
                      <button *ngIf="node.data?.type==='element' && (node.data?.modelType==='SubmodelElementCollection' || node.data?.modelType==='SubmodelElementList' || node.data?.modelType==='Entity')" pButton type="button" class="p-button-text" label="Add child" (click)="openAasCreateElement(node.data.submodelId, node.data.idShortPath)"></button>
                      <button *ngIf="node.data?.type==='submodel'" pButton type="button" class="p-button-text p-button-danger" label="Delete submodel" (click)="deleteAasSubmodel(node.data.id)"></button>
                      <button *ngIf="node.data?.type==='element'" pButton type="button" class="p-button-text p-button-danger" label="Delete" (click)="deleteAasElement(node.data.submodelId, node.data.idShortPath)"></button>
                      <button *ngIf="node.data?.type==='element' && (node.data?.modelType==='Property' || node.data?.raw?.valueType)" pButton type="button" class="p-button-text" label="Set value" (click)="openAasSetValue(node.data.submodelId, node.data)"></button>
                    </div>
                  </ng-template>
                </p-tree>
              </div>
              <div id="aas-element-details" class="p-card" *ngIf="selectedAasNode && selectedAasNode.data?.type==='element'" style="flex: 0 0 35%; padding:1rem;border:1px solid var(--surface-border);border-radius:4px; position: sticky; top: .5rem; align-self: flex-start; max-height: calc(100vh - 1rem); overflow: auto;">
                <div class="p-d-flex p-ai-center p-jc-between">
                  <h4 style="margin:0;">Details</h4>
                  <span *ngIf="aasSelectedLiveLoading">Loading...</span>
                </div>
                <div *ngIf="aasSelectedLivePanel">
                  <div><strong>Label:</strong> {{ aasSelectedLivePanel.label }}</div>
                  <div><strong>Type:</strong> {{ aasSelectedLivePanel.type }}</div>
                  <div *ngIf="aasSelectedLivePanel.valueType"><strong>valueType:</strong> {{ aasSelectedLivePanel.valueType }}</div>
                  <div *ngIf="aasSelectedLivePanel.type==='MultiLanguageProperty' && (aasSelectedLivePanel.value?.length || 0) > 0">
                    <strong>values:</strong>
                    <div style="margin:.25rem 0 .5rem 0;">
                      <div *ngFor="let v of aasSelectedLivePanel.value" style="display:flex;gap:.5rem;align-items:baseline;">
                        <span style="font-size:.75rem;padding:.1rem .4rem;border:1px solid var(--surface-border);border-radius:999px;color:var(--text-color-secondary);min-width:2.5rem;text-align:center;">{{ v?.language || '-' }}</span>
                        <span>{{ v?.text || '' }}</span>
                      </div>
                    </div>
                  </div>
                  <div *ngIf="aasSelectedLivePanel.value !== undefined && aasSelectedLivePanel.type!=='SubmodelElementCollection' && aasSelectedLivePanel.type!=='SubmodelElementList' && aasSelectedLivePanel.type!=='MultiLanguageProperty'">
                    <strong>value:</strong> {{ (aasSelectedLivePanel.value | json) }}
                  </div>
                  <div *ngIf="aasSelectedLivePanel.type==='SubmodelElementCollection'">
                    <div><strong>Items:</strong> {{ aasSelectedLivePanel.value?.length || 0 }}</div>
                  </div>
                  <div *ngIf="aasSelectedLivePanel.type==='SubmodelElementList'">
                    <div><strong>Count:</strong> {{ aasSelectedLivePanel.value?.length || 0 }}</div>
                  </div>
                  <div *ngIf="aasSelectedLivePanel.type==='Range' || aasSelectedLivePanel.min !== undefined || aasSelectedLivePanel.max !== undefined">
                    <div><strong>min:</strong> {{ aasSelectedLivePanel.min }}</div>
                    <div><strong>max:</strong> {{ aasSelectedLivePanel.max }}</div>
                  </div>
                  <div *ngIf="aasSelectedLivePanel.type==='Operation'">
                    <div *ngIf="aasSelectedLivePanel.inputVariables?.length">
                      <strong>Inputs:</strong>
                      <ul style="margin:.25rem 0 .5rem 1rem;">
                        <li *ngFor="let v of aasSelectedLivePanel.inputVariables">{{ v.idShort }} <span *ngIf="v.valueType">({{ v.valueType }})</span></li>
                      </ul>
                    </div>
                    <div *ngIf="aasSelectedLivePanel.inoutputVariables?.length">
                      <strong>In/Out:</strong>
                      <ul style="margin:.25rem 0 .5rem 1rem;">
                        <li *ngFor="let v of aasSelectedLivePanel.inoutputVariables">{{ v.idShort }} <span *ngIf="v.valueType">({{ v.valueType }})</span></li>
                      </ul>
                    </div>
                    <div *ngIf="aasSelectedLivePanel.outputVariables?.length">
                      <strong>Outputs:</strong>
                      <ul style="margin:.25rem 0 .5rem 1rem;">
                        <li *ngFor="let v of aasSelectedLivePanel.outputVariables">{{ v.idShort }} <span *ngIf="v.valueType">({{ v.valueType }})</span></li>
                      </ul>
                    </div>
                  </div>
                  <div *ngIf="aasSelectedLivePanel.annotations?.length">
                    <strong>Annotations:</strong>
                    <ul style="margin:.25rem 0 .5rem 1rem;">
                      <li *ngFor="let anno of aasSelectedLivePanel.annotations">
                        <strong>{{ anno.idShort }}</strong>
                        <span *ngIf="anno.modelType"> ({{ anno.modelType }})</span>:
                        {{ anno.value || '-' }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
        </div>
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

  // AAS-related properties
  aasTreeNodes: TreeNode[] = [];
  aasTreeLoading = false;
  selectedAasNode: TreeNode | null = null;
  aasSelectedLivePanel: AasElementLivePanel | null = null;
  aasSelectedLiveLoading = false;

  constructor(
    private api: TargetSystemResourceService,
    private fb: FormBuilder,
    private confirm: ConfirmationService,
    private http: HttpClient,
    private messageService: MessageService
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

  onCreated(_: TargetSystemDTO): void {
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

  // AAS-related methods
  isAasSelected(): boolean {
    return this.selectedSystem?.apiType === 'AAS';
  }

  discoverAasSnapshot(): void {
    if (!this.selectedSystem?.id) return;
    this.aasTreeLoading = true;
    this.aasTreeNodes = [];
    
    this.http.get<any>(`/api/config/target-system/${this.selectedSystem.id}/aas/submodels`).subscribe({
      next: (response) => {
        // Handle both array and wrapped object responses
        let submodels: any[] = [];
        if (Array.isArray(response)) {
          submodels = response;
        } else if (response && response.result && Array.isArray(response.result)) {
          submodels = response.result;
        } else {
          console.warn('Unexpected submodels response format:', response);
          submodels = [];
        }
        
        const nodes: TreeNode[] = submodels.map(sm => ({
          label: sm.idShort || sm.id || 'Submodel',
          data: {
            type: 'submodel',
            id: sm.id,
            raw: sm,
            modelType: sm.modelType
          },
          leaf: false,
          children: [],
          expandedIcon: 'pi pi-folder-open',
          collapsedIcon: 'pi pi-folder'
        }));
        this.aasTreeNodes = nodes;
        this.aasTreeLoading = false;
      },
      error: () => {
        this.aasTreeLoading = false;
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to load AAS snapshot'});
      }
    });
  }

  onAasNodeExpand(event: any): void {
    const node = event.node;
    if (node.data?.type === 'submodel' && (!node.children || node.children.length === 0)) {
      this.loadSubmodelElements(node);
    } else if (node.data?.type === 'element' && (!node.children || node.children.length === 0)) {
      this.loadElementChildren(node);
    }
  }

  onAasNodeSelect(event: any): void {
    this.selectedAasNode = event.node;
    if (event.node?.data?.type === 'element') {
      this.loadElementDetails(event.node);
    }
  }

  private loadSubmodelElements(node: TreeNode): void {
    if (!this.selectedSystem?.id || !node.data?.id) return;
    
    this.http.get<any>(`/api/config/target-system/${this.selectedSystem.id}/aas/submodels/${btoa(node.data.id)}/elements`).subscribe({
      next: (response) => {
        // Handle both array and wrapped object responses
        let elements: any[] = [];
        if (Array.isArray(response)) {
          elements = response;
        } else if (response && response.result && Array.isArray(response.result)) {
          elements = response.result;
        } else {
          console.warn('Unexpected elements response format:', response);
          elements = [];
        }
        
        node.children = elements.map(el => ({
          label: el.idShort || 'Element',
          data: {
            type: 'element',
            submodelId: node.data.id,
            idShortPath: el.idShortPath || el.idShort,
            raw: el,
            modelType: el.modelType
          },
          leaf: this.isLeafElement(el),
          children: [],
          expandedIcon: 'pi pi-folder-open',
          collapsedIcon: 'pi pi-folder'
        }));
        this.aasTreeNodes = [...this.aasTreeNodes];
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to load submodel elements'});
      }
    });
  }

  private loadElementChildren(node: TreeNode): void {
    if (!this.selectedSystem?.id || !node.data?.submodelId || !node.data?.idShortPath) return;
    
    const smId = btoa(node.data.submodelId);
    const parentPath = node.data.idShortPath;
    
    this.http.get<any>(`/api/config/target-system/${this.selectedSystem.id}/aas/submodels/${smId}/elements?parentPath=${encodeURIComponent(parentPath)}`).subscribe({
      next: (response) => {
        // Handle both array and wrapped object responses
        let children: any[] = [];
        if (Array.isArray(response)) {
          children = response;
        } else if (response && response.result && Array.isArray(response.result)) {
          children = response.result;
        } else {
          console.warn('Unexpected children response format:', response);
          children = [];
        }
        
        node.children = children.map(el => ({
          label: el.idShort || 'Element',
          data: {
            type: 'element',
            submodelId: node.data.submodelId,
            idShortPath: el.idShortPath || el.idShort,
            raw: el,
            modelType: el.modelType
          },
          leaf: this.isLeafElement(el),
          children: [],
          expandedIcon: 'pi pi-folder-open',
          collapsedIcon: 'pi pi-folder'
        }));
        this.aasTreeNodes = [...this.aasTreeNodes];
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to load element children'});
      }
    });
  }

  private loadElementDetails(node: TreeNode): void {
    if (!this.selectedSystem?.id || !node.data?.submodelId || !node.data?.idShortPath) return;
    
    this.aasSelectedLiveLoading = true;
    const smId = btoa(node.data.submodelId);
    const path = node.data.idShortPath;
    
    this.http.get<any>(`/api/config/target-system/${this.selectedSystem.id}/aas/submodels/${smId}/elements/${encodeURIComponent(path)}`).subscribe({
      next: (element) => {
        this.aasSelectedLivePanel = {
          label: element.idShort || 'Element',
          type: element.modelType || 'Unknown',
          value: element.value,
          valueType: element.valueType,
          min: element.min,
          max: element.max,
          inputVariables: element.inputVariables?.map((v: any) => ({
            idShort: v.idShort,
            modelType: v.modelType,
            valueType: v.valueType
          })),
          outputVariables: element.outputVariables?.map((v: any) => ({
            idShort: v.idShort,
            modelType: v.modelType,
            valueType: v.valueType
          })),
          inoutputVariables: element.inoutputVariables?.map((v: any) => ({
            idShort: v.idShort,
            modelType: v.modelType,
            valueType: v.valueType
          })),
          annotations: element.annotations?.map((a: any) => ({
            idShort: a.idShort,
            modelType: a.modelType,
            valueType: a.valueType,
            value: a.value
          }))
        };
        this.aasSelectedLiveLoading = false;
      },
      error: () => {
        this.aasSelectedLiveLoading = false;
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to load element details'});
      }
    });
  }

  private isLeafElement(element: any): boolean {
    const type = element.modelType;
    return type !== 'SubmodelElementCollection' && type !== 'SubmodelElementList' && type !== 'Entity';
  }

  // AAS Management methods (stubs for now)
  openAasCreateSubmodel(): void {
    this.messageService.add({severity: 'info', summary: 'Info', detail: 'Create submodel functionality coming soon'});
  }

  openAasCreateElement(submodelId: string, parentPath?: string): void {
    this.messageService.add({severity: 'info', summary: 'Info', detail: 'Create element functionality coming soon'});
  }

  deleteAasSubmodel(submodelId: string): void {
    this.messageService.add({severity: 'info', summary: 'Info', detail: 'Delete submodel functionality coming soon'});
  }

  deleteAasElement(submodelId: string, elementPath: string): void {
    this.messageService.add({severity: 'info', summary: 'Info', detail: 'Delete element functionality coming soon'});
  }

  openAasSetValue(submodelId: string, elementData: any): void {
    this.messageService.add({severity: 'info', summary: 'Info', detail: 'Set value functionality coming soon'});
  }
}


