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
    CardModule
  ],
  templateUrl: './manage-api-headers.component.html'
})
export class ManageApiHeadersComponent implements OnInit {
  @Input() syncSystemId!: number;              // hier übergibst du die ID (bspw. sourceSystemId)
  @Output() onCreated = new EventEmitter<void>();

  headers: ApiHeaderDTO[] = [];
  form!: FormGroup;
  loading = false;

  headerTypes = [
    {label: 'Accept', value: ApiRequestHeaderType.Accept},
    {label: 'Content-Type', value: ApiRequestHeaderType.ContentType},
    {label: 'Authorization', value: ApiRequestHeaderType.Authorization},
    {label: 'User-Agent', value: ApiRequestHeaderType.UserAgent},
    {label: 'Cache-Control', value: ApiRequestHeaderType.CacheControl},
    {label: 'Accept-Charset', value: ApiRequestHeaderType.AcceptCharset},
    {label: 'Custom', value: ApiRequestHeaderType.Custom}
  ];

  constructor(
    private fb: FormBuilder,
    private hdrSvc: ApiHeaderResourceService,
    protected errorSerice: HttpErrorService
  ) {
  }

  /**
   * Initializes the header management form and loads existing headers.
   */
  ngOnInit() {
    this.form = this.fb.group({
      headerName: ['', Validators.required],
      headerType: [ApiRequestHeaderType.Custom, Validators.required]
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
          console.error(err);
          this.loading = false;
        }
      });
  }

  /**
   * Creates a new API header using form values and reloads the header list.
   */
  addHeader() {
    if (this.form.invalid) return;
    const dto: CreateApiHeaderDTO = this.form.value;
    this.hdrSvc
      .apiConfigSyncSystemSyncSystemIdRequestHeaderPost(this.syncSystemId, dto)
      .subscribe({
        next: () => {
          this.form.reset({headerType: ApiRequestHeaderType.Custom});
          this.loadHeaders();
          this.onCreated.emit();
        },
        error: (err) => {
          console.log(err);
          this.errorSerice.handleError(err);
        }
      });
  }

  /**
   * Deletes an existing API header by its identifier and updates the list.
   *
   * @param id The identifier of the header to delete.
   */
  deleteHeader(id: number) {
    this.hdrSvc
      .apiConfigSyncSystemRequestHeaderIdDelete(id)
      .subscribe({
        next: () => this.headers = this.headers.filter(h => h.id !== id),
        error: console.error
      });
  }
}
