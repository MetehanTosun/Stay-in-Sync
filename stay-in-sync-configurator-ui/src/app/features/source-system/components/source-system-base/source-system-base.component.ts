import {CommonModule} from '@angular/common';
import {Component, OnInit, OnDestroy, HostListener} from '@angular/core';
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
import {FileUploadModule} from 'primeng/fileupload';

// Create-Dialog-Komponente
import {CreateSourceSystemComponent} from '../create-source-system/create-source-system.component';
import {ManageApiHeadersComponent} from '../manage-api-headers/manage-api-headers.component';
import {ManageEndpointsComponent} from '../manage-endpoints/manage-endpoints.component';
import {ConfirmationDialogComponent, ConfirmationDialogData} from '../confirmation-dialog/confirmation-dialog.component';

// Search components and pipes
import {SearchBarComponent} from '../search-bar/search-bar.component';
import {SourceSystemSearchPipe, SearchOptions, SearchResultCount} from '../../pipes/source-system-search.pipe';

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
    FileUploadModule,
    ReactiveFormsModule,
    CreateSourceSystemComponent,
    ManageApiHeadersComponent,
    ManageEndpointsComponent,
    ConfirmationDialogComponent,
    SearchBarComponent,
    FormsModule, // für ngModel
    // ggf. weitere Komponenten
  ]
})
export class SourceSystemBaseComponent implements OnInit, OnDestroy {
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

  selectedFile: File | null = null;

  // Search functionality
  searchTerm: string = '';
  searchOptions: SearchOptions = {};
  filteredSystems: SourceSystemDTO[] = [];
  searchResultCount: SearchResultCount | null = null;
  isSearchActive: boolean = false;

  showHeadersSection = true;
  showEndpointsSection = true;
  showMetadataSection = true;

  // Confirmation dialog properties
  showConfirmationDialog = false;
  confirmationData: ConfirmationDialogData = {
    title: 'Delete Source System',
    message: 'Are you sure you want to delete this source system? This action cannot be undone.',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
    severity: 'danger'
  };
  systemToDelete: SourceSystemDTO | null = null;

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
    private apiEndpointSvc: SourceSystemEndpointResourceService, // hinzugefügt
    private searchPipe: SourceSystemSearchPipe
  ) {
  }

  /**
   * Component initialization lifecycle hook.
   */
  ngOnInit(): void {
    this.metadataForm = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', Validators.required],
      description: [''],
      openApiSpec: ['']
    });
    
    // Load saved search state
    this.loadSearchState();
    
    this.loadSystems();
  }

  ngOnDestroy(): void {
    // Save search state before component is destroyed
    this.saveSearchState();
    console.debug('Component destroyed, search state saved');
  }

  @HostListener('window:beforeunload')
  onBeforeUnload(): void {
    // Save search state when page is about to unload
    this.saveSearchState();
  }

  @HostListener('window:storage', ['$event'])
  onStorageChange(event: StorageEvent): void {
    // Handle storage events from other tabs/windows
    if (event.key === 'sourceSystemSearchState' && event.newValue) {
      try {
        const newState = JSON.parse(event.newValue);
        if (this.isValidSearchState(newState)) {
          // Only update if the state is different from current
          if (newState.searchTerm !== this.searchTerm || 
              JSON.stringify(newState.searchOptions) !== JSON.stringify(this.searchOptions)) {
            this.loadSearchState();
          }
        }
      } catch (error) {
        console.warn('Failed to handle storage change:', error);
      }
    }
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
        
        // Initialize filtered systems
        this.filteredSystems = [...this.systems];
        this.updateSearchResultCount();
        
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
   * Show confirmation dialog for deleting a source system.
   * @param system the system to delete.
   */
  deleteSourceSystem(system: SourceSystemDTO): void {
    this.systemToDelete = system;
    this.confirmationData = {
      title: 'Delete Source System',
      message: `Are you sure you want to delete the source system "${system.name}"? This action cannot be undone and will also delete all associated endpoints, headers, and configurations.`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'danger'
    };
    this.showConfirmationDialog = true;
  }

  /**
   * Handle confirmation dialog events.
   */
  onConfirmationConfirmed(): void {
    if (this.systemToDelete && this.systemToDelete.id) {
      this.api.apiConfigSourceSystemIdDelete(this.systemToDelete.id).subscribe({
        next: () => {
          this.loadSystems();
          this.systemToDelete = null;
        },
        error: err => {
          console.error('Löschen des Source System fehlgeschlagen', err)
          this.erorrService.handleError(err);
          this.systemToDelete = null;
        }
      });
    }
  }

  onConfirmationCancelled(): void {
    this.systemToDelete = null;
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
      description: system.description,
      openApiSpec: system.openApiSpec || ''
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

  /**
   * Handles file selection for OpenAPI spec upload.
   */
  onFileSelected(event: any): void {
    const file = event.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => {
        const fileContent = reader.result as string;
        this.metadataForm.patchValue({ openApiSpec: fileContent });
      };
      reader.readAsText(file);
    }
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

  // Search methods
  onSearchChange(searchTerm: string): void {
    this.searchTerm = searchTerm;
    this.isSearchActive = searchTerm.trim().length > 0;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  onSearchClear(): void {
    this.searchTerm = '';
    this.isSearchActive = false;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  private updateFilteredSystems(): void {
    this.updateFilteredSystemsWithPerformance();
  }

  private updateSearchResultCount(): void {
    // Use the search pipe for comprehensive result count
    this.searchResultCount = this.searchPipe.getSearchResultCount(this.systems, this.searchTerm, this.searchOptions);
  }

  getDisplaySystems(): SourceSystemDTO[] {
    return this.isSearchActive ? this.filteredSystems : this.systems;
  }

  // Advanced search methods
  updateSearchOptions(options: Partial<SearchOptions>): void {
    this.searchOptions = { ...this.searchOptions, ...options };
    if (this.isSearchActive) {
      this.updateFilteredSystems();
    }
  }

  setSearchScope(scope: 'all' | 'names' | 'descriptions' | 'urls' | 'endpoints' | 'headers'): void {
    this.searchOptions.searchScope = scope;
    if (this.isSearchActive) {
      this.updateFilteredSystems();
    }
  }

  toggleCaseSensitive(): void {
    this.searchOptions.caseSensitive = !this.searchOptions.caseSensitive;
    if (this.isSearchActive) {
      this.updateFilteredSystems();
    }
  }

  toggleRegexSearch(): void {
    this.searchOptions.enableRegex = !this.searchOptions.enableRegex;
    if (this.isSearchActive) {
      this.updateFilteredSystems();
    }
  }

  toggleHighlightMatches(): void {
    this.searchOptions.highlightMatches = !this.searchOptions.highlightMatches;
    if (this.isSearchActive) {
      this.updateFilteredSystems();
    }
  }

  // Search state management
  getSearchState(): {
    isActive: boolean;
    term: string;
    options: SearchOptions;
    resultCount: SearchResultCount | null;
    hasResults: boolean;
  } {
    return {
      isActive: this.isSearchActive,
      term: this.searchTerm,
      options: this.searchOptions,
      resultCount: this.searchResultCount,
      hasResults: this.searchResultCount?.hasResults || false
    };
  }

  resetSearch(): void {
    this.searchTerm = '';
    this.isSearchActive = false;
    this.searchOptions = {};
    this.filteredSystems = [...this.systems];
    this.searchResultCount = null;
  }

  // Search persistence
  saveSearchState(): void {
    const searchState = {
      searchTerm: this.searchTerm,
      searchOptions: this.searchOptions,
      isSearchActive: this.isSearchActive,
      timestamp: new Date().toISOString(),
      version: '1.0' // For future compatibility
    };
    
    try {
      localStorage.setItem('sourceSystemSearchState', JSON.stringify(searchState));
      console.debug('Search state saved:', searchState);
    } catch (error) {
      console.warn('Failed to save search state:', error);
      // Try to clear some space by removing old saved searches
      this.clearOldSavedSearches();
    }
  }

  loadSearchState(): void {
    const savedState = localStorage.getItem('sourceSystemSearchState');
    if (savedState) {
      try {
        const state = JSON.parse(savedState);
        
        // Validate the saved state
        if (this.isValidSearchState(state)) {
          this.searchTerm = state.searchTerm || '';
          this.searchOptions = { ...this.getDefaultSearchOptions(), ...state.searchOptions };
          this.isSearchActive = state.isSearchActive || false;
          
          // Update filtered systems if search was active
          if (this.isSearchActive && this.searchTerm) {
            this.updateFilteredSystems();
          }
          
          console.debug('Search state loaded:', state);
        } else {
          console.warn('Invalid search state format, clearing...');
          this.clearSearchState();
        }
      } catch (error) {
        console.warn('Failed to load search state:', error);
        this.clearSearchState();
      }
    }
  }

  clearSearchState(): void {
    localStorage.removeItem('sourceSystemSearchState');
    console.debug('Search state cleared');
  }

  private isValidSearchState(state: any): boolean {
    return state && 
           typeof state === 'object' && 
           (state.searchTerm === undefined || typeof state.searchTerm === 'string') &&
           (state.searchOptions === undefined || typeof state.searchOptions === 'object') &&
           (state.isSearchActive === undefined || typeof state.isSearchActive === 'boolean');
  }

  private clearOldSavedSearches(): void {
    try {
      const savedSearches = JSON.parse(localStorage.getItem('savedSourceSystemSearches') || '[]');
      // Keep only the 5 most recent searches
      const trimmedSearches = savedSearches.slice(0, 5);
      localStorage.setItem('savedSourceSystemSearches', JSON.stringify(trimmedSearches));
      console.debug('Cleared old saved searches');
    } catch (error) {
      console.warn('Failed to clear old saved searches:', error);
    }
  }

  private getDefaultSearchOptions(): SearchOptions {
    return {
      caseSensitive: false,
      enableRegex: false,
      searchScope: 'all',
      highlightMatches: true
    };
  }

  // Search state synchronization and recovery methods
  syncSearchStateAcrossTabs(): void {
    // Broadcast current search state to other tabs
    const searchState = {
      searchTerm: this.searchTerm,
      searchOptions: this.searchOptions,
      isSearchActive: this.isSearchActive,
      timestamp: new Date().toISOString(),
      version: '1.0'
    };
    
    try {
      localStorage.setItem('sourceSystemSearchState', JSON.stringify(searchState));
      // Trigger storage event for other tabs
      window.dispatchEvent(new StorageEvent('storage', {
        key: 'sourceSystemSearchState',
        newValue: JSON.stringify(searchState),
        oldValue: null,
        storageArea: localStorage
      }));
    } catch (error) {
      console.warn('Failed to sync search state across tabs:', error);
    }
  }

  recoverSearchState(): boolean {
    const savedState = localStorage.getItem('sourceSystemSearchState');
    if (savedState) {
      try {
        const state = JSON.parse(savedState);
        if (this.isValidSearchState(state)) {
          // Check if the saved state is recent (within last 24 hours)
          const savedTime = new Date(state.timestamp);
          const now = new Date();
          const hoursDiff = (now.getTime() - savedTime.getTime()) / (1000 * 60 * 60);
          
          if (hoursDiff < 24) {
            this.searchTerm = state.searchTerm || '';
            this.searchOptions = { ...this.getDefaultSearchOptions(), ...state.searchOptions };
            this.isSearchActive = state.isSearchActive || false;
            
            if (this.isSearchActive && this.searchTerm) {
              this.updateFilteredSystems();
            }
            
            console.debug('Search state recovered:', state);
            return true;
          } else {
            console.debug('Saved search state is too old, clearing...');
            this.clearSearchState();
          }
        }
      } catch (error) {
        console.warn('Failed to recover search state:', error);
        this.clearSearchState();
      }
    }
    return false;
  }

  backupSearchState(): void {
    const searchState = {
      searchTerm: this.searchTerm,
      searchOptions: this.searchOptions,
      isSearchActive: this.isSearchActive,
      timestamp: new Date().toISOString(),
      version: '1.0'
    };
    
    try {
      localStorage.setItem('sourceSystemSearchStateBackup', JSON.stringify(searchState));
      console.debug('Search state backed up');
    } catch (error) {
      console.warn('Failed to backup search state:', error);
    }
  }

  restoreSearchStateFromBackup(): boolean {
    const backupState = localStorage.getItem('sourceSystemSearchStateBackup');
    if (backupState) {
      try {
        const state = JSON.parse(backupState);
        if (this.isValidSearchState(state)) {
          this.searchTerm = state.searchTerm || '';
          this.searchOptions = { ...this.getDefaultSearchOptions(), ...state.searchOptions };
          this.isSearchActive = state.isSearchActive || false;
          
          if (this.isSearchActive && this.searchTerm) {
            this.updateFilteredSystems();
          }
          
          console.debug('Search state restored from backup:', state);
          return true;
        }
      } catch (error) {
        console.warn('Failed to restore search state from backup:', error);
      }
    }
    return false;
  }

  getSearchStateInfo(): {
    hasSavedState: boolean;
    hasBackup: boolean;
    lastSaved: string | null;
    stateSize: number;
  } {
    const savedState = localStorage.getItem('sourceSystemSearchState');
    const backupState = localStorage.getItem('sourceSystemSearchStateBackup');
    
    let lastSaved: string | null = null;
    let stateSize = 0;
    
    if (savedState) {
      try {
        const state = JSON.parse(savedState);
        lastSaved = state.timestamp || null;
        stateSize = savedState.length;
      } catch (error) {
        console.warn('Failed to parse saved state for info:', error);
      }
    }
    
    return {
      hasSavedState: !!savedState,
      hasBackup: !!backupState,
      lastSaved,
      stateSize
    };
  }

  // Responsive design methods
  isMobileDevice(): boolean {
    return window.innerWidth <= 768;
  }

  isTabletDevice(): boolean {
    return window.innerWidth > 768 && window.innerWidth <= 1024;
  }

  isDesktopDevice(): boolean {
    return window.innerWidth > 1024;
  }

  isSmallScreen(): boolean {
    return window.innerWidth <= 480;
  }

  isLandscapeMode(): boolean {
    return window.innerHeight < 500 && window.innerWidth > window.innerHeight;
  }

  isTouchDevice(): boolean {
    return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
  }

  getResponsivePlaceholder(): string {
    if (this.isSmallScreen()) {
      return 'Search...';
    } else if (this.isMobileDevice()) {
      return 'Search source systems...';
    } else {
      return 'Search source systems by name, description, or API URL...';
    }
  }

  getResponsiveTableRows(): number {
    if (this.isSmallScreen()) {
      return 5;
    } else if (this.isMobileDevice()) {
      return 8;
    } else {
      return 10;
    }
  }

  shouldShowAdvancedSearch(): boolean {
    return !this.isSmallScreen() && !this.isMobileDevice();
  }

  shouldShowSearchBreakdown(): boolean {
    return !this.isSmallScreen();
  }

  shouldShowSearchActions(): boolean {
    return !this.isSmallScreen();
  }

  getResponsiveSearchOptions(): SearchOptions {
    const baseOptions = this.searchOptions;
    
    // On small screens, disable some features for better performance
    if (this.isSmallScreen()) {
      return {
        ...baseOptions,
        highlightMatches: false, // Disable highlighting on small screens
        searchScope: 'all' // Use simple search scope
      };
    }
    
    return baseOptions;
  }

  // Enhanced search methods
  performAdvancedSearch(searchTerm: string, options: SearchOptions = {}): void {
    this.searchTerm = searchTerm;
    this.searchOptions = { ...this.searchOptions, ...options };
    this.isSearchActive = searchTerm.trim().length > 0;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  searchByName(name: string): void {
    this.performAdvancedSearch(name, { searchScope: 'names' });
  }

  searchByDescription(description: string): void {
    this.performAdvancedSearch(description, { searchScope: 'descriptions' });
  }

  searchByUrl(url: string): void {
    this.performAdvancedSearch(url, { searchScope: 'urls' });
  }

  searchByEndpoints(endpoint: string): void {
    this.performAdvancedSearch(endpoint, { searchScope: 'endpoints' });
  }

  searchByHeaders(header: string): void {
    this.performAdvancedSearch(header, { searchScope: 'headers' });
  }

  // Search with regex
  searchWithRegex(pattern: string): void {
    this.performAdvancedSearch(pattern, { enableRegex: true });
  }

  // Search with case sensitivity
  searchCaseSensitive(term: string): void {
    this.performAdvancedSearch(term, { caseSensitive: true });
  }

  // Get search suggestions
  getSearchSuggestions(): string[] {
    if (!this.searchTerm || this.searchTerm.trim().length < 2) {
      return [];
    }
    
    const noResultsInfo = this.searchPipe.getNoResultsInfo(this.systems, this.searchTerm, this.searchOptions);
    return noResultsInfo.suggestions;
  }

  // Get alternative search terms
  getAlternativeSearchTerms(): string[] {
    if (!this.searchTerm || this.searchTerm.trim().length < 2) {
      return [];
    }
    
    const noResultsInfo = this.searchPipe.getNoResultsInfo(this.systems, this.searchTerm, this.searchOptions);
    return noResultsInfo.alternativeSearchTerms;
  }

  // Search result analysis
  getSearchBreakdown(): SearchResultCount['breakdown'] | null {
    if (!this.searchResultCount) {
      return null;
    }
    return this.searchResultCount.breakdown;
  }

  // Check if search has specific type of results
  hasNameMatches(): boolean {
    const breakdown = this.getSearchBreakdown();
    return breakdown ? breakdown.byName > 0 : false;
  }

  hasDescriptionMatches(): boolean {
    const breakdown = this.getSearchBreakdown();
    return breakdown ? breakdown.byDescription > 0 : false;
  }

  hasUrlMatches(): boolean {
    const breakdown = this.getSearchBreakdown();
    return breakdown ? breakdown.byUrl > 0 : false;
  }

  hasEndpointMatches(): boolean {
    const breakdown = this.getSearchBreakdown();
    return breakdown ? breakdown.byEndpoints > 0 : false;
  }

  hasHeaderMatches(): boolean {
    const breakdown = this.getSearchBreakdown();
    return breakdown ? breakdown.byHeaders > 0 : false;
  }

  // Search performance monitoring
  private searchStartTime: number = 0;
  private searchEndTime: number = 0;

  private startSearchTimer(): void {
    this.searchStartTime = performance.now();
  }

  private endSearchTimer(): number {
    this.searchEndTime = performance.now();
    return this.searchEndTime - this.searchStartTime;
  }

  getSearchPerformance(): { duration: number; resultCount: number; searchTerm: string } {
    return {
      duration: this.searchEndTime - this.searchStartTime,
      resultCount: this.filteredSystems.length,
      searchTerm: this.searchTerm
    };
  }

  // Table integration methods
  getEmptyMessage(): string {
    if (this.isSearchActive) {
      return 'No matching source systems found';
    }
    return 'No source systems available';
  }

  isHighlightedRow(system: SourceSystemDTO): boolean {
    if (!this.isSearchActive || !this.searchOptions?.highlightMatches) {
      return false;
    }
    // Use the search pipe's transform method to check if the system matches
    const matchingSystems = this.searchPipe.transform([system], this.searchTerm, this.searchOptions);
    return matchingSystems.length > 0;
  }

  getHighlightedText(text: string, fieldType: 'name' | 'url' | 'description'): string {
    if (!this.isSearchActive || !this.searchOptions?.highlightMatches || !text) {
      return text || '';
    }
    
    return this.searchPipe.highlightMatches(text, this.searchTerm, this.searchOptions);
  }

  getRelevanceScore(system: SourceSystemDTO): number {
    if (!this.isSearchActive) {
      return 0;
    }
    
    // Use a simple approach: if the system matches the search, give it a score
    const matchingSystems = this.searchPipe.transform([system], this.searchTerm, this.searchOptions);
    return matchingSystems.length > 0 ? 100 : 0;
  }

  getEndpointMatchCount(system: SourceSystemDTO): number {
    if (!this.isSearchActive || !this.searchTerm) {
      return 0;
    }
    
    // For now, return 0 since SourceSystemDTO doesn't have endpoints property
    // This can be enhanced later when we have the full data structure
    return 0;
  }

  getSearchBreakdownText(): string {
    const breakdown = this.getSearchBreakdown();
    if (!breakdown) {
      return '';
    }
    
    const parts: string[] = [];
    if (breakdown.byName > 0) parts.push(`${breakdown.byName} by name`);
    if (breakdown.byDescription > 0) parts.push(`${breakdown.byDescription} by description`);
    if (breakdown.byUrl > 0) parts.push(`${breakdown.byUrl} by URL`);
    if (breakdown.byEndpoints > 0) parts.push(`${breakdown.byEndpoints} by endpoints`);
    if (breakdown.byHeaders > 0) parts.push(`${breakdown.byHeaders} by headers`);
    
    return parts.join(', ');
  }

  hasSearchResults(): boolean {
    return this.searchResultCount?.hasResults || false;
  }

  // Search action methods
  showSearchDetails(system: SourceSystemDTO): void {
    // This would open a dialog showing detailed search information
    console.log('Show search details for:', system.name);
    // TODO: Implement search details dialog
  }

  showSearchSuggestions(): void {
    const suggestions = this.getSearchSuggestions();
    const alternatives = this.getAlternativeSearchTerms();
    
    console.log('Search suggestions:', suggestions);
    console.log('Alternative terms:', alternatives);
    // TODO: Implement suggestions dialog
  }

  exportSearchResults(): void {
    if (!this.hasSearchResults()) {
      return;
    }
    
    const exportData = {
      searchTerm: this.searchTerm,
      searchOptions: this.searchOptions,
      results: this.filteredSystems,
      exportDate: new Date().toISOString(),
      totalResults: this.filteredSystems.length
    };
    
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `source-systems-search-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  saveSearchQuery(): void {
    if (!this.isSearchActive) {
      return;
    }
    
    const savedSearches = JSON.parse(localStorage.getItem('savedSourceSystemSearches') || '[]');
    const searchQuery = {
      term: this.searchTerm,
      options: this.searchOptions,
      timestamp: new Date().toISOString(),
      resultCount: this.filteredSystems.length
    };
    
    // Check if this search already exists
    const existingIndex = savedSearches.findIndex((s: any) => 
      s.term === this.searchTerm && JSON.stringify(s.options) === JSON.stringify(this.searchOptions)
    );
    
    if (existingIndex >= 0) {
      savedSearches[existingIndex] = searchQuery;
    } else {
      savedSearches.unshift(searchQuery);
    }
    
    // Keep only the last 10 searches
    savedSearches.splice(10);
    
    localStorage.setItem('savedSourceSystemSearches', JSON.stringify(savedSearches));
    console.log('Search query saved');
  }

  loadSavedSearches(): any[] {
    return JSON.parse(localStorage.getItem('savedSourceSystemSearches') || '[]');
  }

  applySavedSearch(savedSearch: any): void {
    this.searchTerm = savedSearch.term;
    this.searchOptions = savedSearch.options;
    this.isSearchActive = true;
    this.updateFilteredSystems();
    this.saveSearchState();
  }

  clearSavedSearches(): void {
    localStorage.removeItem('savedSourceSystemSearches');
  }

  focusSearchInput(): void {
    // Focus the search input in the search bar component
    const searchInput = document.querySelector('.search-input') as HTMLInputElement;
    if (searchInput) {
      searchInput.focus();
    }
  }

  // Enhanced updateFilteredSystems with performance monitoring
  private updateFilteredSystemsWithPerformance(): void {
    this.startSearchTimer();
    
    if (this.isSearchActive) {
      // Use the search pipe for comprehensive filtering
      this.filteredSystems = this.searchPipe.transform(this.systems, this.searchTerm, this.searchOptions);
      
      // Apply highlighting if enabled
      if (this.searchOptions.highlightMatches) {
        this.filteredSystems = this.searchPipe.highlightSourceSystemsMatches(
          this.filteredSystems, 
          this.searchTerm, 
          this.searchOptions
        );
      }
    } else {
      this.filteredSystems = [...this.systems];
    }
    
    const searchDuration = this.endSearchTimer();
    
    // Log search performance for debugging
    if (this.isSearchActive && searchDuration > 100) {
      console.warn(`Search took ${searchDuration.toFixed(2)}ms for term: "${this.searchTerm}"`);
    }
    
    // Update search result count
    this.updateSearchResultCount();
  }
}
