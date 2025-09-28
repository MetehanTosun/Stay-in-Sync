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

import {CreateSourceSystemComponent} from '../create-source-system/create-source-system.component';
import {ManageApiHeadersComponent} from '../manage-api-headers/manage-api-headers.component';
import {ManageEndpointsComponent} from '../manage-endpoints/manage-endpoints.component';
import {ConfirmationDialogComponent, ConfirmationDialogData} from '../confirmation-dialog/confirmation-dialog.component';
import {SourceSystemAasManagementComponent} from '../source-system-aas-management/source-system-aas-management.component';

import {SearchBarComponent} from '../search-bar/search-bar.component';
import {SourceSystemSearchPipe, SearchOptions, SearchResultCount} from '../../pipes/source-system-search.pipe';

import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';
import {SourceSystem} from '../../models/sourceSystem';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import { SourceSystemEndpointDTO } from '../../models/sourceSystemEndpointDTO';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

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
    ManageApiHeadersComponent,
    ManageEndpointsComponent,
    ConfirmationDialogComponent,
    SourceSystemAasManagementComponent,
    SearchBarComponent,
    FormsModule,
    ToastModule
  ]
})
export class SourceSystemBaseComponent implements OnInit, OnDestroy {
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

  /**
   * Controls visibility of the detail/manage dialog
   */
  showDetailDialog = false;

  /**
   * Currently selected system for viewing or editing
   */
  selectedSystem: SourceSystemDTO | null = null;
  
  /**
   * Reactive form for editing system metadata
   */
  metadataForm!: FormGroup;

  /**
   * Selected endpoint for parameter management
   */
  selectedEndpointForParams: SourceSystemEndpointDTO | null = null;




  
  /**
   * List of endpoints for the currently selected system
   */
  endpointsForSelectedSystem: SourceSystemEndpointDTO[] = [];

  /**
   * Currently selected file for upload
   */
  selectedFile: File | null = null;

  /**
   * Current search term entered by user
   */
  searchTerm: string = '';
  
  /**
   * Search configuration options
   */
  searchOptions: SearchOptions = {};
  
  /**
   * Filtered systems based on search criteria
   */
  filteredSystems: SourceSystemDTO[] = [];
  
  /**
   * Search result count information
   */
  searchResultCount: SearchResultCount | null = null;
  
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
    private searchPipe: SourceSystemSearchPipe,
    private messageService: MessageService
  ) {
    this.initializeForm();
  }

  /**
   * Initializes the component on startup
   * Loads source systems and sets up search state recovery
   */
  ngOnInit(): void {
    this.loadSystems();
    this.loadSearchState();
    
    window.addEventListener('beforeunload', this.onBeforeUnload.bind(this));
    window.addEventListener('storage', this.onStorageChange.bind(this));
  }

  /**
   * Cleanup on component destruction
   * Saves search state and removes event listeners
   */
  ngOnDestroy(): void {
    this.saveSearchState();
    window.removeEventListener('beforeunload', this.onBeforeUnload.bind(this));
    window.removeEventListener('storage', this.onStorageChange.bind(this));
  }

  /**
   * Handles page unload event to save search state
   */
  onBeforeUnload(): void {
    this.saveSearchState();
  }

  /**
   * Handles storage events from other tabs/windows
   * Synchronizes search state across browser tabs
   * @param event - Storage event containing state changes
   */
  onStorageChange(event: StorageEvent): void {
    if (event.key === 'source-system-search-state' && event.newValue) {
      try {
        const newState = JSON.parse(event.newValue);
        if (this.isValidSearchState(newState) && newState.timestamp > (this.getSearchState().timestamp || 0)) {
          this.searchTerm = newState.term || '';
          this.searchOptions = newState.options || {};
          this.updateFilteredSystems();
        }
      } catch (error) {
        console.warn('Failed to parse search state from storage:', error);
      }
    }
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
        this.updateFilteredSystems();
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
    this.selectedSystem = null;
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
   * Opens the edit dialog for a source system
   * @param system - The source system to edit
   */
  editSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    this.showDetailDialog = true;
  }

  /**
   * Opens the manage dialog for a source system
   * @param system - The source system to manage
   */
  manageSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    this.showDetailDialog = true;
  }

  /**
   * Opens the view dialog for a source system
   * Loads endpoints and shows the detail dialog
   * @param system - The source system to view
   */
  viewSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    // Initialize form with current system values
    this.metadataForm = this.fb.group({
      name: [system.name || '', [Validators.required, Validators.minLength(2)]],
      description: [system.description || ''],
      apiUrl: [system.apiUrl || '', [Validators.required, Validators.pattern('https?://.+')]],
      apiType: [system.apiType || 'REST', Validators.required],
      openApiSpec: [system.openApiSpec || '']
    });
    this.loadEndpointsForSelectedSystem();
    this.showDetailDialog = true;
  }

  /**
   * Closes the detail dialog
   * Resets form and clears selection
   */
  closeDetailDialog(): void {
    this.showDetailDialog = false;
    this.selectedSystem = null;
    this.metadataForm.reset();
  }

  /**
   * Saves metadata changes for the selected system
   * Updates the system via API and refreshes the list
   */
  saveMetadata(): void {
    if (this.metadataForm.valid && this.selectedSystem) {
      const updatedSystem: SourceSystemDTO = {
        ...this.selectedSystem,
        ...this.metadataForm.value
      };

      this.api.apiConfigSourceSystemIdPut(this.selectedSystem.id!, updatedSystem).subscribe({
        next: () => {
          this.loadSystems();
          this.closeDetailDialog();
        },
        error: (error) => {
          this.erorrService.handleError(error);
          this.errorMsg = 'Failed to save metadata';
        }
      });
    }
  }

  /**
   * Handles file selection for OpenAPI specification upload
   * @param event - File upload event
   */
  onFileSelected(event: any): void {
    if (event.files && event.files.length > 0) {
      this.selectedFile = event.files[0];
      this.metadataForm.patchValue({
        openApiSpec: this.selectedFile?.name || ''
      });
    }
  }

  /**
   * Loads endpoints for the currently selected system
   * Fetches endpoint data from the API
   */
  loadEndpointsForSelectedSystem() {
    if (this.selectedSystem) {
      this.apiEndpointSvc.apiConfigSourceSystemSourceSystemIdEndpointGet(this.selectedSystem.id!).subscribe({
        next: (endpoints) => {
          this.endpointsForSelectedSystem = endpoints;
        },
        error: (error) => {
          console.error('Error loading endpoints:', error);
        }
      });
    }
  }

  // AAS: Manage Page helpers
  isAasSelected(): boolean {
    return (this.selectedSystem?.apiType || '').toUpperCase().includes('AAS');
  }

  onAasRefreshRequested(): void {
    // Handle refresh request from AAS management component
    this.loadSystems();
  }





  /**
   * Selects an endpoint for parameter management
   * @param endpoint - The endpoint to select
   */
  selectEndpointForParams(endpoint: SourceSystemEndpointDTO) {
    this.selectedEndpointForParams = endpoint;
  }

  /**
   * Handles search term changes
   * Updates filtered systems and search state
   * @param searchTerm - The new search term
   */
  onSearchChange(searchTerm: string): void {
    this.searchTerm = searchTerm;
    this.isSearchActive = searchTerm.trim().length > 0;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  /**
   * Handles search clear action
   * Resets search state and shows all systems
   */
  onSearchClear(): void {
    this.searchTerm = '';
    this.isSearchActive = false;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  /**
   * Updates the filtered systems based on current search criteria
   * Uses the search pipe to filter and sort results
   */
  private updateFilteredSystems(): void {
    this.startSearchTimer();
    this.filteredSystems = this.searchPipe.transform(this.systems, this.searchTerm, this.searchOptions);
    this.updateSearchResultCount();
    this.searchDurationMs = this.endSearchTimer();
  }

  /**
   * Updates the search result count information
   * Calculates statistics for display
   */
  private updateSearchResultCount(): void {
    this.searchResultCount = this.searchPipe.getSearchResultCount(this.systems, this.searchTerm, this.searchOptions);
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
   * Updates search options and refreshes results
   * @param options - Partial search options to update
   */
  updateSearchOptions(options: Partial<SearchOptions>): void {
    this.searchOptions = { ...this.searchOptions, ...options };
    this.updateFilteredSystems();
  }

  /**
   * Sets the search scope for filtering
   * @param scope - The search scope to use
   */
  setSearchScope(scope: 'all' | 'names' | 'descriptions' | 'urls' | 'endpoints' | 'headers'): void {
    this.updateSearchOptions({ searchScope: scope });
  }

  /**
   * Toggles case sensitivity in search
   */
  toggleCaseSensitive(): void {
    this.updateSearchOptions({ 
      caseSensitive: !this.searchOptions.caseSensitive 
    });
  }

  /**
   * Toggles regex search functionality
   */
  toggleRegexSearch(): void {
    this.updateSearchOptions({ 
      enableRegex: !this.searchOptions.enableRegex 
    });
  }

  /**
   * Toggles search result highlighting
   */
  toggleHighlightMatches(): void {
    this.updateSearchOptions({ 
      highlightMatches: !this.searchOptions.highlightMatches 
    });
  }

  /**
   * Gets the current search state for persistence
   * @returns Object containing current search state
   */
  getSearchState(): {
    isActive: boolean;
    term: string;
    options: SearchOptions;
    resultCount: SearchResultCount | null;
    hasResults: boolean;
    timestamp: number;
  } {
    return {
      isActive: this.isSearchActive,
      term: this.searchTerm,
      options: this.searchOptions,
      resultCount: this.searchResultCount,
      hasResults: this.searchResultCount?.hasResults || false,
      timestamp: Date.now()
    };
  }

  /**
   * Resets search state to default values
   * Clears search term and options
   */
  resetSearch(): void {
    this.searchTerm = '';
    this.searchOptions = this.searchPipe.getDefaultSearchOptions();
    this.isSearchActive = false;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  /**
   * Saves current search state to localStorage
   * Includes timestamp for synchronization across tabs
   */
  saveSearchState(): void {
    const state = this.getSearchState();
    const stateData = {
      ...state,
      version: '1.0',
      savedAt: new Date().toISOString()
    };
    
    try {
      localStorage.setItem('source-system-search-state', JSON.stringify(stateData));
      this.syncSearchStateAcrossTabs();
    } catch (error) {
      console.warn('Failed to save search state:', error);
    }
  }

  /**
   * Loads search state from localStorage
   * Restores previous search term and options
   */
  loadSearchState(): void {
    try {
      const savedState = localStorage.getItem('source-system-search-state');
      if (savedState) {
        const state = JSON.parse(savedState);
        if (this.isValidSearchState(state)) {
          this.searchTerm = state.term || '';
          this.searchOptions = state.options || this.searchPipe.getDefaultSearchOptions();
          this.isSearchActive = state.isActive || false;
          this.updateFilteredSystems();
        }
      }
    } catch (error) {
      console.warn('Failed to load search state:', error);
    }
  }

  /**
   * Clears saved search state from localStorage
   */
  clearSearchState(): void {
    try {
      localStorage.removeItem('source-system-search-state');
      localStorage.removeItem('source-system-search-backup');
    } catch (error) {
      console.warn('Failed to clear search state:', error);
    }
  }

  /**
   * Validates if a saved search state is valid
   * @param state - The state object to validate
   * @returns True if state is valid
   */
  private isValidSearchState(state: any): boolean {
    return state && 
           typeof state === 'object' && 
           typeof state.term === 'string' &&
           typeof state.isActive === 'boolean';
  }

  /**
   * Clears old saved searches to prevent storage bloat
   * Keeps only the most recent searches
   */
  private clearOldSavedSearches(): void {
    try {
      const savedSearches = this.loadSavedSearches();
      if (savedSearches.length > 10) {
        const recentSearches = savedSearches.slice(-10);
        localStorage.setItem('saved-searches', JSON.stringify(recentSearches));
      }
    } catch (error) {
      console.warn('Failed to clear old searches:', error);
    }
  }

  /**
   * Gets default search options from the search pipe
   * @returns Default search options object
   */
  private getDefaultSearchOptions(): SearchOptions {
    return this.searchPipe.getDefaultSearchOptions();
  }

  /**
   * Synchronizes search state across browser tabs
   * Broadcasts current state to other tabs via storage events
   */
  syncSearchStateAcrossTabs(): void {
    const state = this.getSearchState();
    const stateData = {
      ...state,
      timestamp: Date.now(),
      tabId: Math.random().toString(36).substr(2, 9)
    };
    
    try {
      localStorage.setItem('source-system-search-state', JSON.stringify(stateData));
      window.dispatchEvent(new StorageEvent('storage', {
        key: 'source-system-search-state',
        newValue: JSON.stringify(stateData),
        oldValue: null,
        storageArea: localStorage
      }));
    } catch (error) {
      console.warn('Failed to sync search state:', error);
    }
  }

  /**
   * Attempts to recover search state from other tabs
   * @returns True if state was recovered successfully
   */
  recoverSearchState(): boolean {
    try {
      const savedState = localStorage.getItem('source-system-search-state');
      if (savedState) {
        const state = JSON.parse(savedState);
        if (this.isValidSearchState(state) && this.isRecentState(state)) {
          this.searchTerm = state.term || '';
          this.searchOptions = state.options || this.getDefaultSearchOptions();
          this.isSearchActive = state.isActive || false;
          this.updateFilteredSystems();
          return true;
        }
      }
    } catch (error) {
      console.warn('Failed to recover search state:', error);
    }
    return false;
  }

  /**
   * Checks if a saved state is recent (within last 24 hours)
   * @param state - The state object to check
   * @returns True if state is recent
   */
  private isRecentState(state: any): boolean {
    if (!state.timestamp) return false;
    const stateTime = new Date(state.timestamp).getTime();
    const currentTime = Date.now();
    const oneDay = 24 * 60 * 60 * 1000;
    return (currentTime - stateTime) < oneDay;
  }

  /**
   * Creates a backup of current search state
   */
  backupSearchState(): void {
    try {
      const state = this.getSearchState();
      localStorage.setItem('source-system-search-backup', JSON.stringify(state));
    } catch (error) {
      console.warn('Failed to backup search state:', error);
    }
  }

  /**
   * Restores search state from backup
   * @returns True if restoration was successful
   */
  restoreSearchStateFromBackup(): boolean {
    try {
      const backupState = localStorage.getItem('source-system-search-backup');
      if (backupState) {
        const state = JSON.parse(backupState);
        if (this.isValidSearchState(state)) {
          this.searchTerm = state.term || '';
          this.searchOptions = state.options || this.getDefaultSearchOptions();
          this.isSearchActive = state.isActive || false;
          this.updateFilteredSystems();
          return true;
        }
      }
    } catch (error) {
      console.warn('Failed to restore search state from backup:', error);
    }
    return false;
  }

  /**
   * Gets information about saved search state
   * @returns Object containing state information
   */
  getSearchStateInfo(): {
    hasSavedState: boolean;
    hasBackup: boolean;
    lastSaved: string | null;
    stateSize: number;
  } {
    try {
      const savedState = localStorage.getItem('source-system-search-state');
      const backupState = localStorage.getItem('source-system-search-backup');
      
      return {
        hasSavedState: !!savedState,
        hasBackup: !!backupState,
        lastSaved: savedState ? new Date(JSON.parse(savedState).timestamp || Date.now()).toLocaleString() : null,
        stateSize: (savedState?.length || 0) + (backupState?.length || 0)
      };
    } catch (error) {
      return {
        hasSavedState: false,
        hasBackup: false,
        lastSaved: null,
        stateSize: 0
      };
    }
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
  getResponsiveSearchOptions(): SearchOptions {
    const baseOptions = this.searchOptions;
    
    if (this.isSmallScreen()) {
      return {
        ...baseOptions,
        highlightMatches: false,
        searchScope: 'all'
      };
    }
    
    return baseOptions;
  }

  /**
   * Performs advanced search with custom options
   * @param searchTerm - Search term to use
   * @param options - Custom search options
   */
  performAdvancedSearch(searchTerm: string, options: SearchOptions = {}): void {
    this.searchTerm = searchTerm;
    this.searchOptions = { ...this.searchOptions, ...options };
    this.isSearchActive = true;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  /**
   * Searches by system name
   * @param name - Name to search for
   */
  searchByName(name: string): void {
    this.performAdvancedSearch(name, { searchScope: 'names' });
  }

  /**
   * Searches by system description
   * @param description - Description to search for
   */
  searchByDescription(description: string): void {
    this.performAdvancedSearch(description, { searchScope: 'descriptions' });
  }

  /**
   * Searches by API URL
   * @param url - URL to search for
   */
  searchByUrl(url: string): void {
    this.performAdvancedSearch(url, { searchScope: 'urls' });
  }

  /**
   * Searches by endpoints
   * @param endpoint - Endpoint to search for
   */
  searchByEndpoints(endpoint: string): void {
    this.performAdvancedSearch(endpoint, { searchScope: 'endpoints' });
  }

  /**
   * Searches by headers
   * @param header - Header to search for
   */
  searchByHeaders(header: string): void {
    this.performAdvancedSearch(header, { searchScope: 'headers' });
  }

  /**
   * Searches using regex pattern
   * @param pattern - Regex pattern to search for
   */
  searchWithRegex(pattern: string): void {
    this.performAdvancedSearch(pattern, { enableRegex: true });
  }

  /**
   * Searches with case sensitivity
   * @param term - Search term
   */
  searchCaseSensitive(term: string): void {
    this.performAdvancedSearch(term, { caseSensitive: true });
  }

  /**
   * Gets search suggestions based on current systems
   * @returns Array of search suggestions
   */
  getSearchSuggestions(): string[] {
    const suggestions: string[] = [];
    
    this.systems.forEach(system => {
      if (system.name && !suggestions.includes(system.name)) {
        suggestions.push(system.name);
      }
      if (system.apiType && !suggestions.includes(system.apiType)) {
        suggestions.push(system.apiType);
      }
    });
    
    return suggestions.slice(0, 10);
  }

  /**
   * Gets alternative search terms based on current search
   * @returns Array of alternative search terms
   */
  getAlternativeSearchTerms(): string[] {
    if (!this.searchTerm) return [];
    
    const alternatives: string[] = [];
    const term = this.searchTerm.toLowerCase();
    
    this.systems.forEach(system => {
      if (system.name?.toLowerCase().includes(term) && !alternatives.includes(system.name)) {
        alternatives.push(system.name);
      }
      if (system.description?.toLowerCase().includes(term)) {
        const words = system.description.split(' ').filter(word => 
          word.toLowerCase().includes(term) && word.length > 3
        );
        alternatives.push(...words.slice(0, 3));
      }
    });
    
    return [...new Set(alternatives)].slice(0, 5);
  }

  /**
   * Gets search breakdown information
   * @returns Search breakdown object or null
   */
  getSearchBreakdown(): SearchResultCount['breakdown'] | null {
    return this.searchResultCount?.breakdown || null;
  }

  /**
   * Checks if there are name matches in search results
   * @returns True if name matches exist
   */
  hasNameMatches(): boolean {
    return !!this.searchResultCount?.breakdown?.byName;
  }

  /**
   * Checks if there are description matches in search results
   * @returns True if description matches exist
   */
  hasDescriptionMatches(): boolean {
    return !!this.searchResultCount?.breakdown?.byDescription;
  }

  /**
   * Checks if there are URL matches in search results
   * @returns True if URL matches exist
   */
  hasUrlMatches(): boolean {
    return !!this.searchResultCount?.breakdown?.byUrl;
  }

  /**
   * Checks if there are endpoint matches in search results
   * @returns True if endpoint matches exist
   */
  hasEndpointMatches(): boolean {
    return !!this.searchResultCount?.breakdown?.byEndpoints;
  }

  /**
   * Checks if there are header matches in search results
   * @returns True if header matches exist
   */
  hasHeaderMatches(): boolean {
    return !!this.searchResultCount?.breakdown?.byHeaders;
  }

  /**
   * Start time for search performance measurement
   */
  private searchStartTime: number = 0;
  
  /**
   * End time for search performance measurement
   */
  private searchEndTime: number = 0;
  /** Cached search duration to avoid recomputation during checks */
  searchDurationMs: number = 0;

  /**
   * Starts the search performance timer
   */
  private startSearchTimer(): void {
    this.searchStartTime = performance.now();
  }

  /**
   * Ends the search performance timer
   * @returns Duration in milliseconds
   */
  private endSearchTimer(): number {
    this.searchEndTime = performance.now();
    return this.searchEndTime - this.searchStartTime;
  }

  /**
   * Gets search performance metrics
   * @returns Object containing performance data
   */
  getSearchPerformance(): { duration: number; resultCount: number; searchTerm: string } {
    return {
      duration: this.searchDurationMs,
      resultCount: this.searchResultCount?.filtered || 0,
      searchTerm: this.searchTerm
    };
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
   * Checks if a row should be highlighted based on search
   * @param system - The system to check
   * @returns True if row should be highlighted
   */
  isHighlightedRow(system: SourceSystemDTO): boolean {
    if (!this.isSearchActive || !this.searchOptions.highlightMatches) {
      return false;
    }
    
    const searchableText = this.getSystemSearchableText(system);
    return searchableText.toLowerCase().includes(this.searchTerm.toLowerCase());
  }

  /**
   * Gets searchable text from a system for highlighting
   * @param system - The system to get text from
   * @returns Combined searchable text
   */
  private getSystemSearchableText(system: SourceSystemDTO): string {
    const searchableParts: string[] = [];
    const searchScope = this.searchOptions.searchScope || 'all';

    if (searchScope === 'all' || searchScope === 'names') {
      searchableParts.push(system.name || '');
      searchableParts.push(system.description || '');
    }

    if (searchScope === 'all' || searchScope === 'urls') {
      searchableParts.push(system.apiUrl || '');
      searchableParts.push(system.apiType || '');
    }

    return searchableParts.join(' ').toLowerCase();
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
    
    return this.searchPipe.highlightMatches(text, this.searchTerm, this.searchOptions);
  }

  /**
   * Calculates relevance score for a system
   * @param system - The system to score
   * @returns Relevance score as percentage
   */
  getRelevanceScore(system: SourceSystemDTO): number {
    if (!this.isSearchActive) return 0;
    
    const searchableText = this.getSystemSearchableText(system);
    const normalizedSearchTerm = this.searchTerm.toLowerCase();
    let score = 0;

    if (system.name) {
      const normalizedName = system.name.toLowerCase();
      if (normalizedName === normalizedSearchTerm) {
        score += 100;
      } else if (normalizedName.includes(normalizedSearchTerm)) {
        score += 50;
      }
    }

    if (system.description) {
      const normalizedDesc = system.description.toLowerCase();
      if (normalizedDesc.includes(normalizedSearchTerm)) {
        score += 30;
      }
    }

    if (system.apiUrl) {
      const normalizedUrl = system.apiUrl.toLowerCase();
      if (normalizedUrl.includes(normalizedSearchTerm)) {
        score += 25;
      }
    }

    if (system.apiType) {
      const normalizedType = system.apiType.toLowerCase();
      if (normalizedType === normalizedSearchTerm) {
        score += 20;
      } else if (normalizedType.includes(normalizedSearchTerm)) {
        score += 15;
      }
    }

    return Math.min(Math.round((score / 100) * 100), 100);
  }

  /**
   * Gets endpoint match count for a system
   * @param system - The system to check
   * @returns Number of matching endpoints
   */
  getEndpointMatchCount(system: SourceSystemDTO): number {
    if (!this.isSearchActive || !this.hasEndpointMatches()) return 0;
    
    return this.endpointsForSelectedSystem.filter(endpoint => 
      endpoint.endpointPath?.toLowerCase().includes(this.searchTerm.toLowerCase())
    ).length;
  }

  /**
   * Gets formatted search breakdown text
   * @returns Formatted breakdown string
   */
  getSearchBreakdownText(): string {
    const breakdown = this.getSearchBreakdown();
    if (!breakdown) return '';
    
    const parts: string[] = [];
    if (breakdown.byName > 0) parts.push(`${breakdown.byName} names`);
    if (breakdown.byDescription > 0) parts.push(`${breakdown.byDescription} descriptions`);
    if (breakdown.byUrl > 0) parts.push(`${breakdown.byUrl} URLs`);
    if (breakdown.byEndpoints > 0) parts.push(`${breakdown.byEndpoints} endpoints`);
    if (breakdown.byHeaders > 0) parts.push(`${breakdown.byHeaders} headers`);
    
    return parts.join(', ');
  }

  /**
   * Checks if search has results
   * @returns True if search has results
   */
  hasSearchResults(): boolean {
    return this.searchResultCount?.hasResults || false;
  }

  /**
   * Shows detailed search information for a system
   * @param system - The system to show details for
   */
  showSearchDetails(system: SourceSystemDTO): void {
    console.log('Search details for:', system.name, {
      relevanceScore: this.getRelevanceScore(system),
      endpointMatches: this.getEndpointMatchCount(system),
      searchTerm: this.searchTerm,
      searchOptions: this.searchOptions
    });
  }

  /**
   * Shows search suggestions to the user
   */
  showSearchSuggestions(): void {
    const suggestions = this.getSearchSuggestions();
    const alternatives = this.getAlternativeSearchTerms();
    
    console.log('Search suggestions:', { suggestions, alternatives });
  }

  /**
   * Exports search results to various formats
   */
  exportSearchResults(): void {
    if (!this.hasSearchResults()) return;
    
    const exportData = {
      searchTerm: this.searchTerm,
      searchOptions: this.searchOptions,
      results: this.filteredSystems,
      exportDate: new Date().toISOString(),
      totalResults: this.searchResultCount?.filtered || 0
    };
    
    const dataStr = JSON.stringify(exportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    
    const link = document.createElement('a');
    link.href = url;
    link.download = `source-systems-search-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    
    URL.revokeObjectURL(url);
  }

  /**
   * Saves current search query for later use
   */
  saveSearchQuery(): void {
    if (!this.isSearchActive) return;
    
    try {
      const savedSearches = this.loadSavedSearches();
      const newSearch = {
        term: this.searchTerm,
        options: this.searchOptions,
        timestamp: Date.now(),
        resultCount: this.searchResultCount?.filtered || 0
      };
      
      savedSearches.push(newSearch);
      
      if (savedSearches.length > 20) {
        savedSearches.splice(0, savedSearches.length - 20);
      }
      
      localStorage.setItem('saved-searches', JSON.stringify(savedSearches));
    } catch (error) {
      console.warn('Failed to save search query:', error);
    }
  }

  /**
   * Loads previously saved searches
   * @returns Array of saved searches
   */
  loadSavedSearches(): any[] {
    try {
      const saved = localStorage.getItem('saved-searches');
      return saved ? JSON.parse(saved) : [];
    } catch (error) {
      return [];
    }
  }

  /**
   * Applies a saved search
   * @param savedSearch - The saved search to apply
   */
  applySavedSearch(savedSearch: any): void {
    if (savedSearch.term) {
      this.searchTerm = savedSearch.term;
      this.searchOptions = savedSearch.options || this.getDefaultSearchOptions();
      this.isSearchActive = true;
      this.updateFilteredSystems();
      this.saveSearchState();
    }
  }

  /**
   * Clears all saved searches
   */
  clearSavedSearches(): void {
    try {
      localStorage.removeItem('saved-searches');
    } catch (error) {
      console.warn('Failed to clear saved searches:', error);
    }
  }

  /**
   * Focuses the search input field
   */
  focusSearchInput(): void {
    const searchInput = document.querySelector('input[formControlName="searchTerm"]') as HTMLInputElement;
    if (searchInput) {
      searchInput.focus();
      searchInput.select();
    }
  }

  /**
   * Updates filtered systems with performance measurement
   */
  private updateFilteredSystemsWithPerformance(): void {
    this.startSearchTimer();
    this.updateFilteredSystems();
    this.endSearchTimer();
  }

  /**
   * Initializes the reactive form for metadata editing
   */
  private initializeForm(): void {
    this.metadataForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      apiType: ['REST', Validators.required],
      openApiSpec: ['']
    });
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

  /**
   * Handle endpoint creation event
   */
  onEndpointCreated(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Endpoint Created',
      detail: 'Endpoint has been successfully created.',
      life: 3000
    });
  }

  /**
   * Handle endpoint deletion event
   */
  onEndpointDeleted(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Endpoint Deleted',
      detail: 'Endpoint has been successfully deleted.',
      life: 3000
    });
  }

  /**
   * Handle endpoint update event
   */
  onEndpointUpdated(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Endpoint Updated',
      detail: 'Endpoint has been successfully updated.',
      life: 3000
    });
  }

}
