import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';

// PrimeNG
import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {DialogModule} from 'primeng/dialog';
import {ToolbarModule} from 'primeng/toolbar';
import {MessageModule} from 'primeng/message';
import {CardModule} from 'primeng/card';
import {TabViewModule} from 'primeng/tabview';
import {DropdownModule} from 'primeng/dropdown';
import {InputTextModule} from 'primeng/inputtext';
import {TextareaModule} from 'primeng/textarea';

// Create-Dialog-Komponente
import {CreateSourceSystemComponent} from '../create-source-system/create-source-system.component';
import {ManageApiHeadersComponent} from '../manage-api-headers/manage-api-headers.component';
import {ManageEndpointsComponent} from '../manage-endpoints/manage-endpoints.component';

// Service und DTOs aus dem `generated`-Ordner
import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';
import {SourceSystem} from '../../models/sourceSystem';
import {HttpErrorService} from '../../../../core/services/http-error.service';

/**
 * Base component for displaying, creating, and managing source systems.
 */
@Component({
  standalone: true,
  selector: 'app-source-system-base',
  templateUrl: './source-system-base.component.html',
  styleUrls: ['./source-system-base.component.css'],
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    DialogModule,
    ToolbarModule,
    MessageModule,
    CardModule,
    TabViewModule,
    DropdownModule,
    InputTextModule,
    TextareaModule,
    ReactiveFormsModule,
    CreateSourceSystemComponent,
    ManageApiHeadersComponent,
    ManageEndpointsComponent
  ]
})
export class SourceSystemBaseComponent implements OnInit {
  /**
   * List of source systems to display in the table.
   */
  systems: SourceSystemDTO[] = [];
  /**
   * Flag indicating whether data is currently loading.
   */
  loading = false;
  /**
   * Holds any error message encountered during operations.
   */
  errorMsg?: string;

  /**
   * Controls visibility of the create/edit dialog.
   */
  showCreateDialog = false;

  /**
   * Controls visibility of the detail/manage dialog.
   */
  showDetailDialog = false;

  /**
   * Currently selected system for viewing or editing.
   */
  selectedSystem: SourceSystemDTO | null = null;
  /**
   * Reactive form for editing system metadata.
   */
  metadataForm!: FormGroup;

  /**
   * Injects the source system service and form builder.
   */
  constructor(private api: SourceSystemResourceService, private fb: FormBuilder, protected erorrService: HttpErrorService) {
  }

  /**
   * Component initialization lifecycle hook.
   */
  ngOnInit(): void {
    this.loadSystems();

    this.metadataForm = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: ['']
    });
  }

  /**
   * Load all source systems from the backend and populate the table.
   */
  loadSystems(): void {
    this.loading = true;
    this.api.apiConfigSourceSystemGet().subscribe({
      next: (list: SourceSystem[]) => {

        this.systems = list.map(system => ({
          id: system.id,
          name: system.name || '',
          apiUrl: system.apiUrl || '',
          description: system.description || '',
          apiType: system.apiType || '',
          openApiSpec: undefined
        } as SourceSystemDTO));
        this.loading = false;
      },
      error: err => {
        console.error('Failed to load source systems', err);
        this.erorrService.handleError(err);
        this.errorMsg = 'Failed to load source systems';
        this.loading = false;
      }
    });
  }
}
