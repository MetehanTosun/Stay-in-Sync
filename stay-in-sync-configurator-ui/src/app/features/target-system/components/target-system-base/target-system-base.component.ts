import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { CreateTargetSystemComponent } from '../create-target-system/create-target-system.component';

@Component({
  standalone: true,
  selector: 'app-target-system-base',
  template: `
    <p-toolbar>
      <div class="p-toolbar-group-start">
        <button pButton label="New" icon="pi pi-plus" (click)="openCreate()"></button>
      </div>
    </p-toolbar>

    <p-table [value]="systems" [loading]="loading">
      <ng-template pTemplate="header">
        <tr>
          <th>Name</th>
          <th>API URL</th>
          <th>Type</th>
          <th>Actions</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-row>
        <tr>
          <td>{{ row.name }}</td>
          <td>{{ row.apiUrl }}</td>
          <td>{{ row.apiType }}</td>
          <td>
            <button pButton icon="pi pi-pencil" class="p-button-text" (click)="edit(row)"></button>
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
    <p-confirmDialog></p-confirmDialog>
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
    CreateTargetSystemComponent
  ],
  providers: [ConfirmationService, MessageService]
})
export class TargetSystemBaseComponent implements OnInit {
  systems: TargetSystemDTO[] = [];
  loading = false;
  showDialog = false;
  dialogTitle = 'New Target System';
  form!: FormGroup;
  editing: TargetSystemDTO | null = null;
  wizardVisible = false;

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
  }

  load(): void {
    this.loading = true;
    this.api.getAll().subscribe({
      next: list => { this.systems = list; this.loading = false; },
      error: () => { this.loading = false; }
    });
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
    this.confirm.confirm({
      message: `Soll "${row.name}" gelöscht werden?`,
      accept: () => this.remove(row)
    });
  }

  remove(row: TargetSystemDTO): void {
    if (!row.id) return;
    this.api.delete(row.id).subscribe({ next: () => this.load() });
  }

  closeDialog(): void { this.showDialog = false; }
}


