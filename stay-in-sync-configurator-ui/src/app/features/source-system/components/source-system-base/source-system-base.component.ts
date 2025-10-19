import {CommonModule} from '@angular/common';
import {Component, OnInit, OnDestroy, HostListener} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { FormsModule } from '@angular/forms';

import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {DialogModule} from 'primeng/dialog';
import {ToolbarModule} from 'primeng/toolbar';
import {MessageModule} from 'primeng/message';
import {CardModule} from 'primeng/card';
import {TabViewModule} from 'primeng/tabview';
import {TreeModule} from 'primeng/tree';
import {DropdownModule} from 'primeng/dropdown';
import {InputTextModule} from 'primeng/inputtext';
import {TextareaModule} from 'primeng/textarea';
import {FileUploadModule} from 'primeng/fileupload';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import {CreateSourceSystemComponent} from '../create-source-system/create-source-system.component';
import {ConfirmationDialogComponent, ConfirmationDialogData} from '../confirmation-dialog/confirmation-dialog.component';
import {SourceSystemAasManagementComponent} from '../source-system-aas-management/source-system-aas-management.component';
import { ManageApiHeadersComponent } from '../manage-api-headers/manage-api-headers.component';
import { ManageEndpointsComponent } from '../manage-endpoints/manage-endpoints.component';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';
import {SourceSystem} from '../../models/sourceSystem';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import { SourceSystemEndpointDTO } from '../../models/sourceSystemEndpointDTO';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { AasService } from '../../services/aas.service';
import { TreeNode } from 'primeng/api';
import {Router} from '@angular/router';
import {JobStatusTagComponent} from '../../../../shared/components/job-status-tag/job-status-tag.component';
import {Select} from 'primeng/select';

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

/**
 * Base component for displaying, creating, and managing source systems.
 * Provides comprehensive functionality for listing, searching, creating, editing,
 * and deleting source systems with advanced search capabilities and responsive design.
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
    TreeModule,
    DropdownModule,
    InputTextModule,
    TextareaModule,
    FileUploadModule,
    ReactiveFormsModule,
    CreateSourceSystemComponent,
    ConfirmationDialogComponent,
    FormsModule,
    JobStatusTagComponent,
    Select,
    ToastModule,
    ManageApiHeadersComponent,
    ManageEndpointsComponent,
    SourceSystemAasManagementComponent,
  ],
  providers: [MessageService]
})
export class SourceSystemBaseComponent implements OnInit {
  /**
   * List of source systems to display in the table
   */
  systems: SourceSystemDTO[] = [];

  /**
   * Flag indicating whether data is currently loading
   */
  loading = false;

  /**
   * Holds any error message encountered during operations
   */
  errorMsg?: string;

  /**
   * Controls visibility of the create/edit dialog
   */
  showCreateDialog = false;

  // design reverted; routes handle manage flow

  /**
   * Currently selected file for upload
   */
  selectedFile: File | null = null;

  /**
   * Current search term entered by user
   */
  searchTerm: string = '';

  // simplified search options placeholder if needed
  searchOptions: any = {};

  /**
   * Filtered systems based on search criteria
   */
  filteredSystems: SourceSystemDTO[] = [];

  /**
   * Search result count information
   */
  searchResultCount: any | null = null;

  /**
   * Flag indicating if search is currently active
   */
  isSearchActive: boolean = false;

  /**
   * Controls visibility of headers section in detail dialog
   */
  showHeadersSection = true;

  /**
   * Controls visibility of endpoints section in detail dialog
   */
  showEndpointsSection = true;

  /**
   * Controls visibility of metadata section in detail dialog
   */
  showMetadataSection = true;

  /**
   * Controls visibility of confirmation dialog
   */
  showConfirmationDialog = false;

  /**
   * Configuration data for confirmation dialog
   */
  confirmationData: ConfirmationDialogData = {
    title: 'Confirm Delete',
    message: 'Are you sure you want to delete this source system?',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
    severity: 'danger'
  };

  /**
   * System to be deleted (stored for confirmation)
   */
  systemToDelete: SourceSystemDTO | null = null;

  /**
   * Toggles the visibility of the headers section
   */
  toggleHeadersSection() {
    this.showHeadersSection = !this.showHeadersSection;
  }

  /**
   * Toggles the visibility of the endpoints section
   */
  toggleEndpointsSection() {
    this.showEndpointsSection = !this.showEndpointsSection;
  }

  /**
   * Toggles the visibility of the metadata section
   */
  toggleMetadataSection() {
    this.showMetadataSection = !this.showMetadataSection;
  }

  /**
   * Constructor for the SourceSystemBaseComponent
   * @param api - Service for source system API operations
   * @param fb - Form builder for reactive forms
   * @param erorrService - Service for handling HTTP errors
   * @param apiEndpointSvc - Service for endpoint operations
   * @param searchPipe - Pipe for search functionality
   */
  constructor(
    private api: SourceSystemResourceService,
    private fb: FormBuilder,
    protected erorrService: HttpErrorService,
    private apiEndpointSvc: SourceSystemEndpointResourceService,
    private aasService: AasService,
    private router: Router,
    private messageService: MessageService
  ) {
  }

  /**
   * Initializes the component on startup
   * Loads source systems and sets up search state recovery
   */
  ngOnInit(): void {
    this.loadSystems();

  }

  /**
   * Loads all source systems from the API
   * Handles loading states and error conditions
   */
  loadSystems(): void {
    this.loading = true;
    this.errorMsg = undefined;

    this.api.apiConfigSourceSystemGet().subscribe({
      next: (systems: SourceSystem[]) => {
        this.systems = systems.map(system => ({
          id: system.id,
          name: system.name || '',
          apiUrl: system.apiUrl || '',
          description: system.description || '',
          apiType: system.apiType || '',
          openApiSpec: undefined
        } as SourceSystemDTO));
        this.loading = false;
      },
      error: (error) => {
        this.erorrService.handleError(error);
        this.errorMsg = 'Failed to load source systems';
        this.loading = false;
      }
    });
  }

  /**
   * Opens the create source system dialog
   * Resets selection and shows the dialog
   */
  openCreate(): void {
    this.showCreateDialog = true;
  }

  /**
   * Handles the close event of the create dialog
   * Reloads systems if a new one was created
   * @param visible - Whether the dialog is visible
   */
  onCreateDialogClose(visible: boolean): void {
    this.showCreateDialog = visible;
    if (!visible) {
      this.loadSystems();
    }
  }

  /**
   * Initiates deletion of a source system
   * Shows confirmation dialog before proceeding
   * @param system - The source system to delete
   */
  deleteSourceSystem(system: SourceSystemDTO): void {
    this.systemToDelete = system;
    this.confirmationData = {
      title: 'Confirm Delete',
      message: `Are you sure you want to delete "${system.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'danger'
    };
    this.showConfirmationDialog = true;
  }

  /**
   * Handles confirmation of system deletion
   * Performs the actual deletion and updates the list
   */
  onConfirmationConfirmed(): void {
    if (this.systemToDelete) {
      this.api.apiConfigSourceSystemIdDelete(this.systemToDelete.id!).subscribe({
        next: () => {
          this.loadSystems();
          this.showConfirmationDialog = false;
          this.systemToDelete = null;
          this.messageService.add({ key: 'sourceBase', severity: 'success', summary: 'Source System Deleted', detail: 'Source system has been removed.', life: 3000 });
        },
        error: (error) => {
          this.erorrService.handleError(error);
          this.errorMsg = 'Failed to delete source system';
          this.showConfirmationDialog = false;
        }
      });
    }
  }

  /**
   * Handles cancellation of system deletion
   * Closes the confirmation dialog without action
   */
  onConfirmationCancelled(): void {
    this.showConfirmationDialog = false;
    this.systemToDelete = null;
  }

  /**
   * Opens the route to manage a source system
   */
  manageSourceSystem(system: SourceSystemDTO): void {
    this.router.navigate(['/source-system/', system.id]);
  }

  // AAS: Manage Page helpers
  onAasRefreshRequested(): void {
    // Handle refresh request from AAS management component
    this.loadSystems();
  }

  /**
   * Gets the systems to display in the table
   * Returns filtered systems when search is active, otherwise all systems
   * @returns Array of systems to display
   */
  getDisplaySystems(): SourceSystemDTO[] {
    return this.isSearchActive ? this.filteredSystems : this.systems;
  }

  /**
   * Checks if current device is a mobile device
   * @returns True if mobile device
   */
  isMobileDevice(): boolean {
    return window.innerWidth <= 768;
  }

  /**
   * Checks if current device is a tablet
   * @returns True if tablet device
   */
  isTabletDevice(): boolean {
    return window.innerWidth > 768 && window.innerWidth <= 1024;
  }

  /**
   * Checks if current device is a desktop
   * @returns True if desktop device
   */
  isDesktopDevice(): boolean {
    return window.innerWidth > 1024;
  }

  /**
   * Checks if current screen is small
   * @returns True if small screen
   */
  isSmallScreen(): boolean {
    return window.innerWidth <= 640;
  }

  /**
   * Checks if device is in landscape mode
   * @returns True if landscape orientation
   */
  isLandscapeMode(): boolean {
    return window.innerWidth > window.innerHeight;
  }

  /**
   * Checks if device supports touch input
   * @returns True if touch device
   */
  isTouchDevice(): boolean {
    return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
  }

  /**
   * Gets responsive placeholder text for search input
   * @returns Placeholder text based on screen size
   */
  getResponsivePlaceholder(): string {
    if (this.isSmallScreen()) {
      return 'Search systems...';
    } else if (this.isMobileDevice()) {
      return 'Search by name, URL, or description...';
    } else {
      return 'Search source systems by name, description, API URL, endpoints, or headers...';
    }
  }

  /**
   * Gets responsive table row count
   * @returns Number of rows to display based on screen size
   */
  getResponsiveTableRows(): number {
    if (this.isSmallScreen()) {
      return 5;
    } else if (this.isMobileDevice()) {
      return 10;
    } else {
      return 25;
    }
  }

  /**
   * Checks if advanced search features should be shown
   * @returns True if advanced search should be visible
   */
  shouldShowAdvancedSearch(): boolean {
    return !this.isSmallScreen() && this.isSearchActive;
  }

  /**
   * Checks if search breakdown should be shown
   * @returns True if breakdown should be visible
   */
  shouldShowSearchBreakdown(): boolean {
    return !this.isSmallScreen() && !!this.searchResultCount?.breakdown;
  }

  /**
   * Checks if search actions should be shown
   * @returns True if actions should be visible
   */
  shouldShowSearchActions(): boolean {
    return !this.isSmallScreen() && this.isSearchActive;
  }

  /**
   * Gets responsive search options based on screen size
   * @returns Search options optimized for current screen
   */
  getResponsiveSearchOptions(): any {
    return {};
  }

  /**
   * Gets appropriate empty state message
   * @returns Message to display when no results are found
   */
  getEmptyMessage(): string {
    if (this.isSearchActive) {
      return 'No matching source systems found';
    }
    return 'No source systems available';
  }

  /**
   * Gets highlighted text for display
   * @param text - Original text
   * @param fieldType - Type of field for highlighting
   * @returns Text with HTML highlighting
   */
  getHighlightedText(text: string, fieldType: 'name' | 'url' | 'description'): string {
    if (!this.isSearchActive || !this.searchOptions.highlightMatches || !text) {
      return text || '';
    }

    // simple fallback: highlight by wrapping matches in <mark>
    try {
      const term = (this.searchTerm || '').trim();
      if (!term) return text || '';
      const esc = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const re = new RegExp(esc, 'gi');
      return String(text || '').replace(re, (m) => `<mark>${m}</mark>`);
    } catch {
      return text || '';
    }
  }
}
