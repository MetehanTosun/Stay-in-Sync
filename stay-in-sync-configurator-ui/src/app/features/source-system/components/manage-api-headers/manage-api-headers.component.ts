import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {CardModule} from 'primeng/card';

import {ApiHeaderResourceService} from '../../service/apiHeaderResource.service';
import {ApiRequestHeaderType} from '../../models/apiRequestHeaderType';
import {ApiHeaderDTO} from '../../models/apiHeaderDTO';
import {CreateApiHeaderDTO} from '../../models/createApiHeaderDTO';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {ConfirmationDialogComponent, ConfirmationDialogData} from '../confirmation-dialog/confirmation-dialog.component';
import {FloatLabel} from 'primeng/floatlabel';
import {Select, SelectModule} from 'primeng/select';

/**
 * Component for managing API header templates for a given system.
 * Allows viewing, creating, and deleting header definitions.
 */
@Component({
  standalone: true,
  selector: 'app-manage-api-headers',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    CardModule,
    ConfirmationDialogComponent,
    FloatLabel,
    SelectModule
  ],
  templateUrl: './manage-api-headers.component.html'
})
export class ManageApiHeadersComponent implements OnInit {
  /** ID of the sync system (e.g., sourceSystemId) */
  @Input() syncSystemId!: number;
  /** When true, hide Accept/Content-Type and show AAS-specific hint */
  @Input() isAas: boolean = false;
  /** When true, allow entering and updating concrete header values */
  @Input() allowValues: boolean = false;
  @Output() onCreated = new EventEmitter<void>();
  @Output() onDeleted = new EventEmitter<void>();
  @Output() onRetest = new EventEmitter<void>();

  headers: ApiHeaderDTO[] = [];
  form!: FormGroup;
  loading = false;
  editing: ApiHeaderDTO | null = null;

  /** Confirmation dialog properties */
  showConfirmationDialog = false;
  confirmationData: ConfirmationDialogData = {
    title: 'Delete API Header',
    message: 'Are you sure you want to delete this API header? This action cannot be undone.',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
    severity: 'warning'
  };
  headerToDelete: ApiHeaderDTO | null = null;

  headerTypes = [
    {label: 'Accept', value: ApiRequestHeaderType.Accept},
    {label: 'Content-Type', value: ApiRequestHeaderType.ContentType},
    {label: 'Authorization', value: ApiRequestHeaderType.Authorization},
    {label: 'User-Agent', value: ApiRequestHeaderType.UserAgent},
    {label: 'Cache-Control', value: ApiRequestHeaderType.CacheControl},
    {label: 'Accept-Charset', value: ApiRequestHeaderType.AcceptCharset},
    {label: 'Custom', value: ApiRequestHeaderType.Custom}
  ];

  get allowedHeaderTypes() {
    if (!this.isAas) return this.headerTypes;
    return this.headerTypes.filter(h => h.value !== ApiRequestHeaderType.Accept && h.value !== ApiRequestHeaderType.ContentType);
  }

  constructor(
    private fb: FormBuilder,
    private hdrSvc: ApiHeaderResourceService,
    protected errorSerice: HttpErrorService
  ) {}


  /**
   * Initializes the header management form and loads existing headers.
   */
  ngOnInit() {
    this.form = this.fb.group({
      headerName: ['', Validators.required],
      headerType: [ApiRequestHeaderType.Custom, Validators.required],
      headerValue: ['']
    });
    this.loadHeaders();
  }

  /**
   * Fetches the list of headers for the current system and updates UI state.
   */
  loadHeaders() {
    this.loading = true;
    this.hdrSvc
      .apiConfigSyncSystemSyncSystemIdRequestHeaderGet(this.syncSystemId)
      .subscribe({
        next: (list) => {
          this.headers = list;
          this.loading = false;
        },
        error: (err) => {
          this.errorSerice.handleError(err);
          this.loading = false;
        }
      });
  }

  /**
   * Creates a new API header using form values and reloads the header list.
   */
  addHeader() {
    if (this.form.invalid) return;
    if (this.editing) {
      const existingValues = (this.editing.values as any) ? Array.from(this.editing.values as any) as string[] : [];
      const nextValues = this.allowValues && this.form.value.headerValue
        ? [this.form.value.headerValue]
        : existingValues;
      const updated: ApiHeaderDTO = {
        id: this.editing.id,
        headerName: this.form.value.headerName,
        headerType: this.form.value.headerType,
        values: nextValues as any
      } as any;
      this.hdrSvc.apiConfigSyncSystemRequestHeaderIdPut(this.editing.id!, updated).subscribe({
        next: () => {
          this.editing = null;
          this.form.reset({headerType: ApiRequestHeaderType.Custom});
          this.loadHeaders();
        },
        error: (err) => this.errorSerice.handleError(err)
      });
    } else {
      const dto: CreateApiHeaderDTO = {
        headerName: this.form.value.headerName,
        headerType: this.form.value.headerType,
        // @ts-ignore
        values: this.allowValues && this.form.value.headerValue ? [this.form.value.headerValue] : undefined
      } as any;
      this.hdrSvc
        .apiConfigSyncSystemSyncSystemIdRequestHeaderPost(this.syncSystemId, dto)
        .subscribe({
          next: () => {
            this.form.reset({headerType: ApiRequestHeaderType.Custom});
            this.loadHeaders();
            this.onCreated.emit();
          },
          error: (err) => {
            this.errorSerice.handleError(err);
          }
        });
    }
  }

  editHeader(h: ApiHeaderDTO) {
    this.editing = h;
    this.form.patchValue({
      headerName: h.headerName,
      headerType: h.headerType,
      headerValue: h.values && (h.values as any)[0] ? (h.values as any)[0] : ''
    });
  }

  cancelEdit() {
    this.editing = null;
    this.form.reset({ headerType: ApiRequestHeaderType.Custom });
  }

  /**
   * Show confirmation dialog for deleting an API header.
   *
   * @param header The header to delete.
   */
  deleteHeader(header: ApiHeaderDTO) {
    this.headerToDelete = header;
    this.confirmationData = {
      title: 'Delete API Header',
      message: `Are you sure you want to delete the API header "${header.headerName}" (${header.headerType})? This action cannot be undone.`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'warning'
    };
    this.showConfirmationDialog = true;
  }

  /**
   * Handle confirmation dialog events.
   */
  onConfirmationConfirmed(): void {
    if (this.headerToDelete && this.headerToDelete.id) {
      this.hdrSvc
        .apiConfigSyncSystemRequestHeaderIdDelete(this.headerToDelete.id)
        .subscribe({
          next: () => {
            this.headers = this.headers.filter(h => h.id !== this.headerToDelete!.id);
            this.headerToDelete = null;
            this.onDeleted.emit();
          },
          error: (err) => {
            this.errorSerice.handleError(err);
            this.headerToDelete = null;
          }
        });
    }
  }

  onConfirmationCancelled(): void {
    this.headerToDelete = null;
  }
}
