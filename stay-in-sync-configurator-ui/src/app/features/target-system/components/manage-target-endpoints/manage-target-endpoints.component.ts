import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';
import { TargetSystemEndpointDTO } from '../../models/targetSystemEndpointDTO';
import { CreateTargetSystemEndpointDTO } from '../../models/createTargetSystemEndpointDTO';

@Component({
  standalone: true,
  selector: 'app-manage-target-endpoints',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    DropdownModule,
    CardModule,
    ProgressSpinnerModule
  ],
  template: `
    <p-card header="Target Endpoints">
      <button pButton label="Neu" icon="pi pi-plus" (click)="openCreate()"></button>
      <p-table [value]="endpoints" [loading]="loading" class="mt-3">
        <ng-template pTemplate="header">
          <tr>
            <th>Path</th>
            <th>Method</th>
            <th>Aktionen</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-row>
          <tr>
            <td>{{ row.endpointPath }}</td>
            <td>{{ row.httpRequestType }}</td>
            <td>
              <button pButton icon="pi pi-pencil" class="p-button-text" (click)="openEdit(row)"></button>
              <button pButton icon="pi pi-trash" class="p-button-text p-button-danger" (click)="delete(row)"></button>
            </td>
          </tr>
        </ng-template>
      </p-table>
    </p-card>

    <p-dialog [(visible)]="showDialog" [modal]="true" [style]="{width: '480px'}" [header]="dialogTitle">
      <form [formGroup]="form" class="p-fluid">
        <div class="p-field">
          <label for="endpointPath">Endpoint Path</label>
          <input id="endpointPath" pInputText formControlName="endpointPath">
        </div>
        <div class="p-field">
          <label for="httpRequestType">HTTP Methode</label>
          <select id="httpRequestType" formControlName="httpRequestType">
            <option *ngFor="let m of httpRequestTypes" [value]="m">{{ m }}</option>
          </select>
        </div>
      </form>
      <ng-template pTemplate="footer">
        <button pButton label="Abbrechen" class="p-button-text" (click)="showDialog=false"></button>
        <button pButton label="Speichern" (click)="save()" [disabled]="form.invalid"></button>
      </ng-template>
    </p-dialog>
  `,
  styles: [``]
})
export class ManageTargetEndpointsComponent implements OnInit {
  @Input() targetSystemId!: number;
  @Output() finish = new EventEmitter<void>();

  endpoints: TargetSystemEndpointDTO[] = [];
  loading = false;
  showDialog = false;
  dialogTitle = 'Neuer Endpoint';
  form!: FormGroup;
  editing: TargetSystemEndpointDTO | null = null;
  httpRequestTypes: Array<'GET'|'POST'|'PUT'|'DELETE'|'PATCH'> = ['GET','POST','PUT','DELETE','PATCH'];

  constructor(private api: TargetSystemEndpointResourceService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required]
    });
    this.load();
  }

  load(): void {
    if (!this.targetSystemId) return;
    this.loading = true;
    this.api.list(this.targetSystemId).subscribe({
      next: list => { this.endpoints = list; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openCreate(): void {
    this.editing = null;
    this.dialogTitle = 'Neuer Endpoint';
    this.form.reset({ endpointPath: '', httpRequestType: 'GET' });
    this.showDialog = true;
  }

  openEdit(row: TargetSystemEndpointDTO): void {
    this.editing = row;
    this.dialogTitle = 'Endpoint bearbeiten';
    this.form.reset({ endpointPath: row.endpointPath, httpRequestType: row.httpRequestType });
    this.showDialog = true;
  }

  save(): void {
    if (this.editing?.id) {
      const dto: TargetSystemEndpointDTO = {
        id: this.editing.id,
        targetSystemId: this.targetSystemId,
        endpointPath: this.form.value.endpointPath,
        httpRequestType: this.form.value.httpRequestType
      };
      this.api.replace(this.editing.id, dto).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    } else {
      const payload: CreateTargetSystemEndpointDTO = {
        endpointPath: this.form.value.endpointPath,
        httpRequestType: this.form.value.httpRequestType
      };
      this.api.create(this.targetSystemId, [payload]).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    }
  }

  delete(row: TargetSystemEndpointDTO): void {
    if (!row.id) return;
    this.api.delete(row.id).subscribe({ next: () => this.load() });
  }
}


