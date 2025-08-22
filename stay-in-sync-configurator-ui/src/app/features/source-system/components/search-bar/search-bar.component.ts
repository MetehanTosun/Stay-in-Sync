import { Component, EventEmitter, Input, Output, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

export interface SearchBarConfig {
  placeholder?: string;
  debounceTime?: number;
  showClearButton?: boolean;
  showSearchButton?: boolean;
  showLoadingIndicator?: boolean;
  caseSensitive?: boolean;
  enableRegex?: boolean;
}

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    ProgressSpinnerModule
  ],
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent implements OnInit, OnDestroy {
  /**
   * Configuration for the search bar component.
   */
  @Input() config: SearchBarConfig = {
    placeholder: 'Search source systems, endpoints, headers...',
    debounceTime: 300,
    showClearButton: true,
    showSearchButton: false,
    showLoadingIndicator: false,
    caseSensitive: false,
    enableRegex: false
  };

  /**
   * CSS class for the search icon.
   */
  @Input() searchIconClass = 'pi pi-search';

  /**
   * Custom placeholder text for the search input.
   */
  @Input() customPlaceholder?: string;

  /**
   * Indicates whether the loading spinner is active.
   */
  @Input() loading = false;

  /**
   * Number of search results found.
   */
  @Input() resultCount?: number;

  /**
   * Text displayed while loading.
   */
  @Input() loadingText = 'Searching...';

  /**
   * Determines whether to show loading text.
   */
  @Input() showLoadingText = true;

  /**
   * Emits the search term when a search is performed.
   */
  @Output() search = new EventEmitter<string>();

  /**
   * Emits an event when the search is cleared.
   */
  @Output() clear = new EventEmitter<void>();

  /**
   * Emits the search term whenever it changes.
   */
  @Output() searchChange = new EventEmitter<string>();

  /**
   * Form group for managing the search input.
   */
  searchForm!: FormGroup;

  /**
   * Subject used for managing component destruction.
   */
  private destroy$ = new Subject<void>();

  /**
   * Subject used for debouncing search input changes.
   */
  private searchSubject = new Subject<string>();

  /**
   * Unique ID for the component, used for accessibility.
   */
  componentId!: string;

  constructor(private fb: FormBuilder) {}

  /**
   * Initializes the component and sets up the form and debouncing logic.
   */
  ngOnInit(): void {
    this.initializeForm();
    this.setupSearchDebouncing();
    this.componentId = this.generateComponentId();
  }

  /**
   * Cleans up resources when the component is destroyed.
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubject.complete();
  }

  /**
   * Initializes the search form.
   */
  private initializeForm(): void {
    this.searchForm = this.fb.group({
      searchTerm: ['']
    });
  }

  /**
   * Sets up debouncing for the search input changes.
   */
  private setupSearchDebouncing(): void {
    this.searchSubject
      .pipe(
        debounceTime(this.config.debounceTime || 300),
        distinctUntilChanged((prev, curr) => {
          const prevNormalized = prev?.trim().toLowerCase();
          const currNormalized = curr?.trim().toLowerCase();
          return prevNormalized === currNormalized;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(searchTerm => {
        const normalizedTerm = searchTerm?.trim();
        this.searchChange.emit(normalizedTerm);
        this.search.emit(normalizedTerm);
      });
  }

  /**
   * Handles input events for the search field.
   * @param event The input event.
   */
  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  /**
   * Handles change events for the search field.
   * @param event The change event.
   */
  onSearchInputChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  /**
   * Handles paste events for the search field.
   * @param event The clipboard event.
   */
  onSearchPaste(event: ClipboardEvent): void {
    const pastedText = event.clipboardData?.getData('text') || '';
    if (pastedText.trim()) {
      setTimeout(() => {
        this.searchSubject.next(this.searchTerm);
      }, 100);
    }
  }

  /**
   * Submits the current search term.
   */
  onSearchSubmit(): void {
    const searchTerm = this.searchForm.get('searchTerm')?.value;
    if (searchTerm) {
      this.search.emit(searchTerm);
    }
  }

  /**
   * Clears the search input and emits the clear event.
   */
  onClearSearch(): void {
    this.searchForm.patchValue({ searchTerm: '' });
    this.searchSubject.next('');
    this.clear.emit();
    setTimeout(() => {
      const searchInput = document.querySelector('.search-input') as HTMLInputElement;
      if (searchInput) {
        searchInput.focus();
      }
    }, 0);
  }

  /**
   * Clears the search input.
   */
  clearSearch(): void {
    this.onClearSearch();
  }

  /**
   * Checks whether a search term is active.
   * @returns True if a search term is active, false otherwise.
   */
  isSearchActive(): boolean {
    return this.hasSearchTerm;
  }

  /**
   * Handles keyboard events for the search field.
   * @param event The keyboard event.
   */
  onKeyDown(event: KeyboardEvent): void {
    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        this.onClearSearch();
        break;

      case 'Enter':
        event.preventDefault();
        this.onSearchSubmit();
        break;

      case 'Tab':
        break;

      case 'ArrowUp':
      case 'ArrowDown':
        if (this.config.enableRegex) {
          break;
        }
        event.preventDefault();
        break;

      default:
        break;
    }
  }

  /**
   * Handles global keyboard events for accessibility shortcuts.
   * @param event The keyboard event.
   */
  @HostListener('document:keydown', ['$event'])
  onGlobalKeyDown(event: KeyboardEvent): void {
    if (event.ctrlKey && event.key === 'f' && !this.isSearchInputFocused()) {
      event.preventDefault();
      this.focusSearchInput();
    }
  }

  /**
   * Checks whether the search input is currently focused.
   * @returns True if the search input is focused, false otherwise.
   */
  private isSearchInputFocused(): boolean {
    const searchInput = document.querySelector('.search-input') as HTMLInputElement;
    return searchInput === document.activeElement;
  }

  /**
   * Focuses and selects the search input field.
   */
  focusSearchInput(): void {
    const searchInput = document.querySelector('.search-input') as HTMLInputElement;
    if (searchInput) {
      searchInput.focus();
      searchInput.select();
    }
  }

  /**
   * Returns a string describing keyboard shortcuts for the component.
   * @returns A string containing keyboard shortcuts.
   */
  getKeyboardShortcutsHelp(): string {
    const shortcuts = [
      'Enter: Search',
      'Escape: Clear search',
      'Ctrl+F: Focus search (when not already focused)'
    ];

    if (this.config.enableRegex) {
      shortcuts.push('Arrow keys: Navigate regex pattern');
    }

    return shortcuts.join(', ');
  }

  /**
   * Sets the loading state of the component.
   * @param isLoading True to enable loading, false to disable.
   */
  setLoading(isLoading: boolean): void {
    this.loading = isLoading;
  }

  /**
   * Checks whether the component is currently loading.
   * @returns True if loading, false otherwise.
   */
  isLoading(): boolean {
    return this.loading;
  }

  /**
   * Returns the text to display while loading.
   * @returns The loading text or an empty string if not loading.
   */
  getLoadingDisplayText(): string {
    if (!this.loading) return '';
    return this.showLoadingText ? this.loadingText : '';
  }

  /**
   * Checks whether a search is currently in progress.
   * @returns True if a search is in progress, false otherwise.
   */
  isSearchInProgress(): boolean {
    return this.loading && this.hasSearchTerm;
  }

  /**
   * Gets the current search term.
   * @returns The search term as a string.
   */
  get searchTerm(): string {
    return this.searchForm.get('searchTerm')?.value || '';
  }

  /**
   * Checks whether a search term is present.
   * @returns True if a search term exists, false otherwise.
   */
  get hasSearchTerm(): boolean {
    return this.searchTerm.trim().length > 0;
  }

  /**
   * Returns a string describing the number of search results.
   * @returns A string describing the result count.
   */
  get resultCountText(): string {
    if (this.resultCount === undefined) return '';
    return this.resultCount === 0 ? 'No results found' : `${this.resultCount} result${this.resultCount === 1 ? '' : 's'} found`;
  }

  /**
   * Returns the placeholder text for the search input.
   * @returns The placeholder text.
   */
  get placeholderText(): string {
    return this.customPlaceholder || this.config.placeholder || 'Search...';
  }

  /**
   * Returns the CSS classes for the search icon.
   * @returns The CSS classes as a string.
   */
  get searchIconClasses(): string {
    return this.searchIconClass;
  }

  /**
   * Generates a unique ID for the component.
   * @returns A unique ID string.
   */
  private generateComponentId(): string {
    return 'search-bar-' + Math.random().toString(36).substr(2, 9);
  }
}
