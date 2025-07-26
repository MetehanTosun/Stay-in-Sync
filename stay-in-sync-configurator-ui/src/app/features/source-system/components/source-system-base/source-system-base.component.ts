import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { FormsModule } from '@angular/forms';

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
import { ManageEndpointParamsComponent } from '../manage-endpoint-params/manage-endpoint-params.component';

// Service und DTOs aus dem `generated`-Ordner
import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';
import {SourceSystem} from '../../models/sourceSystem';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import { SourceSystemEndpointDTO } from '../../models/sourceSystemEndpointDTO';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';

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
    ManageEndpointsComponent,
    ManageEndpointParamsComponent, // hinzugefügt
    FormsModule, // für ngModel
    // ggf. weitere Komponenten
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

  selectedEndpointForParams: SourceSystemEndpointDTO | null = null;
  endpointsForSelectedSystem: SourceSystemEndpointDTO[] = [];

  showHeadersSection = true;
  showEndpointsSection = true;
  showMetadataSection = true;

  toggleHeadersSection() {
    
    this.showHeadersSection = !this.showHeadersSection;
  }

  toggleEndpointsSection() {
    this.showEndpointsSection = !this.showEndpointsSection;
  }

  toggleMetadataSection() {
    this.showMetadataSection = !this.showMetadataSection;
  }

  /**
   * Injects the source system service and form builder.
   */
  constructor(
    private api: SourceSystemResourceService,
    private fb: FormBuilder,
    protected erorrService: HttpErrorService,
    private apiEndpointSvc: SourceSystemEndpointResourceService // hinzugefügt
  ) {
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

  /**
   * Open the create new system dialog.
   */
  openCreate(): void {
    // Reset selection when creating new
    this.selectedSystem = null;
    this.showCreateDialog = true;
  }

  /**
   * Handler for when the create/edit dialog is closed; reloads systems if necessary.
   * @param visible true if the dialog remains visible after closure.
   */
  onCreateDialogClose(visible: boolean): void {
    this.showCreateDialog = visible;
    if (!visible) {
      this.loadSystems();
      this.selectedSystem = null;
    }
  }

  /**
   * Delete a source system and refresh the list.
   * @param system the system to delete.
   */
  deleteSourceSystem(system: SourceSystemDTO): void {
    if (!system.id) {
      console.warn('Keine ID vorhanden, Löschen übersprungen');
      return;
    }
    this.api.apiConfigSourceSystemIdDelete(system.id).subscribe({
      next: () => this.loadSystems(),
      error: err => {
        console.error('Löschen des Source System fehlgeschlagen', err)
        this.erorrService.handleError(err);
      }
    });
  }

  /**
   * Open the create dialog pre-filled to edit an existing system.
   * @param system the system to edit.
   */
  editSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    this.showCreateDialog = true;
  }

  /**
   * Open the create/edit wizard in manage mode for the selected system.
   * @param system the system to manage.
   */
  manageSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    this.showCreateDialog = true;
    this.loadEndpointsForSelectedSystem(); // Endpoints beim Öffnen laden
  }

  /**
   * Open the detail dialog to manage headers and endpoints for a system.
   * @param system the system to view details of.
   */
  viewSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    this.showDetailDialog = true;

    this.metadataForm.patchValue({
      name: system.name,
      apiUrl: system.apiUrl,
      description: system.description
    });
    this.loadEndpointsForSelectedSystem();
  }

  /**
   * Close the detail dialog and optionally reload systems.
   */
  closeDetailDialog(): void {
    this.showDetailDialog = false;
    this.selectedSystem = null;
    this.loadSystems();
  }

  /**
   * Save metadata changes of the selected system to the backend.
   */
  saveMetadata(): void {
    if (!this.selectedSystem || this.metadataForm.invalid) {
      return;
    }
    const updated: SourceSystemDTO = {
      ...this.selectedSystem,
      ...this.metadataForm.value
    };
    this.api
      .apiConfigSourceSystemIdPut(this.selectedSystem.id!, updated)
      .subscribe({
        next: () => {
          this.selectedSystem = updated;
          this.loadSystems();
        },
        error: err => {
          console.error('Failed to save metadata', err)
          this.erorrService.handleError(err);
        }
      });
  }

  // Neue Methode, um Endpunkte für das aktuell ausgewählte System zu laden
  loadEndpointsForSelectedSystem() {
    if (!this.selectedSystem?.id) {
      this.endpointsForSelectedSystem = [];
      return;
    }
    this.apiEndpointSvc.apiConfigSourceSystemSourceSystemIdEndpointGet(this.selectedSystem.id).subscribe({
      next: (eps: SourceSystemEndpointDTO[]) => {
        this.endpointsForSelectedSystem = eps;
      },
      error: (err) => {
        this.endpointsForSelectedSystem = [];
        console.error('Fehler beim Laden der Endpunkte:', err);
      }
    });
  }

  // Methode, um einen Endpoint für die Param-Verwaltung auszuwählen
  selectEndpointForParams(endpoint: SourceSystemEndpointDTO) {
    this.selectedEndpointForParams = endpoint;
  }
}
