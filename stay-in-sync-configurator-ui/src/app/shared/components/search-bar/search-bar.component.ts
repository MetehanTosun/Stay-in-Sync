import { Component, EventEmitter, Input, Output, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

// PrimeNG components
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
  @Input() config: SearchBarConfig = {
    placeholder: 'Search source systems, endpoints, headers...',
    debounceTime: 300,
    showClearButton: true,
    showSearchButton: false,
    showLoadingIndicator: false,
    caseSensitive: false,
    enableRegex: false
  };

  @Input() searchIconClass = 'pi pi-search';
  @Input() customPlaceholder?: string;

  @Input() loading = false;
  @Input() resultCount?: number;
  @Input() loadingText = 'Searching...';
  @Input() showLoadingText = true;

  @Output() search = new EventEmitter<string>();
  @Output() clear = new EventEmitter<void>();
  @Output() searchChange = new EventEmitter<string>();

  searchForm!: FormGroup;
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.initializeForm();
    this.setupSearchDebouncing();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchSubject.complete();
  }

  private initializeForm(): void {
    this.searchForm = this.fb.group({
      searchTerm: ['']
    });
  }

  private setupSearchDebouncing(): void {
    this.searchSubject
      .pipe(
        debounceTime(this.config.debounceTime || 300),
        distinctUntilChanged((prev, curr) => {
          // Normalize whitespace for comparison
          const prevNormalized = prev?.trim().toLowerCase();
          const currNormalized = curr?.trim().toLowerCase();
          return prevNormalized === currNormalized;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(searchTerm => {
        // Emit the normalized search term
        const normalizedTerm = searchTerm?.trim();
        this.searchChange.emit(normalizedTerm);
        this.search.emit(normalizedTerm);
      });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onSearchInputChange(event: Event): void {
    // Handle form control value changes
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onSearchPaste(event: ClipboardEvent): void {
    // Handle paste events with immediate search
    const pastedText = event.clipboardData?.getData('text') || '';
    if (pastedText.trim()) {
      // For paste events, we might want to search immediately
      setTimeout(() => {
        this.searchSubject.next(this.searchTerm);
      }, 100);
    }
  }

  onSearchSubmit(): void {
    const searchTerm = this.searchForm.get('searchTerm')?.value;
    if (searchTerm) {
      this.search.emit(searchTerm);
    }
  }

  onClearSearch(): void {
    // Clear the form control
    this.searchForm.patchValue({ searchTerm: '' });
    
    // Emit empty search term to trigger filtering
    this.searchSubject.next('');
    
    // Emit clear event for parent components
    this.clear.emit();
    
    // Focus back to the search input for better UX
    setTimeout(() => {
      const searchInput = document.querySelector('.search-input') as HTMLInputElement;
      if (searchInput) {
        searchInput.focus();
      }
    }, 0);
  }

  // Method to programmatically clear search (for external use)
  clearSearch(): void {
    this.onClearSearch();
  }

  // Method to check if search is active
  isSearchActive(): boolean {
    return this.hasSearchTerm;
  }

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
        // Allow normal tab navigation
        break;
        
      case 'ArrowUp':
      case 'ArrowDown':
        // Prevent default arrow key behavior in search input
        // This allows for future autocomplete functionality
        if (this.config.enableRegex) {
          // Allow arrow keys for regex editing
          break;
        }
        event.preventDefault();
        break;
        
      default:
        // Allow all other keys
        break;
    }
  }

  // Method to handle Ctrl+F (browser find) integration
  @HostListener('document:keydown', ['$event'])
  onGlobalKeyDown(event: KeyboardEvent): void {
    // If Ctrl+F is pressed and we're not already in the search input
    if (event.ctrlKey && event.key === 'f' && !this.isSearchInputFocused()) {
      event.preventDefault();
      this.focusSearchInput();
    }
  }

  // Method to check if search input is focused
  private isSearchInputFocused(): boolean {
    const searchInput = document.querySelector('.search-input') as HTMLInputElement;
    return searchInput === document.activeElement;
  }

  // Method to focus the search input
  focusSearchInput(): void {
    const searchInput = document.querySelector('.search-input') as HTMLInputElement;
    if (searchInput) {
      searchInput.focus();
      searchInput.select(); // Select all text for easy replacement
    }
  }

  // Method to get keyboard shortcuts help text
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

  // Method to set loading state
  setLoading(isLoading: boolean): void {
    this.loading = isLoading;
  }

  // Method to get loading state
  isLoading(): boolean {
    return this.loading;
  }

  // Method to get loading display text
  getLoadingDisplayText(): string {
    if (!this.loading) return '';
    return this.showLoadingText ? this.loadingText : '';
  }

  // Method to check if search is in progress
  isSearchInProgress(): boolean {
    return this.loading && this.hasSearchTerm;
  }

  get searchTerm(): string {
    return this.searchForm.get('searchTerm')?.value || '';
  }

  get hasSearchTerm(): boolean {
    return this.searchTerm.trim().length > 0;
  }

  get resultCountText(): string {
    if (this.resultCount === undefined) return '';
    return this.resultCount === 0 ? 'No results found' : `${this.resultCount} result${this.resultCount === 1 ? '' : 's'} found`;
  }

  get placeholderText(): string {
    return this.customPlaceholder || this.config.placeholder || 'Search...';
  }

  get searchIconClasses(): string {
    return this.searchIconClass;
  }
} 