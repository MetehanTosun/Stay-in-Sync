import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { SearchBarComponent, SearchBarConfig } from './search-bar.component';

describe('SearchBarComponent', () => {
  let component: SearchBarComponent;
  let fixture: ComponentFixture<SearchBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchBarComponent, ReactiveFormsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize with default config', () => {
      expect(component.config.placeholder).toBe('Search source systems, endpoints, headers...');
      expect(component.config.debounceTime).toBe(300);
      expect(component.config.showClearButton).toBe(true);
      expect(component.config.showSearchButton).toBe(false);
    });

    it('should initialize form with empty search term', () => {
      expect(component.searchForm.get('searchTerm')?.value).toBe('');
    });

    it('should have empty search term initially', () => {
      expect(component.searchTerm).toBe('');
      expect(component.hasSearchTerm).toBe(false);
    });
  });

  describe('Search Input Functionality', () => {
    it('should update search term when input changes', () => {
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      searchInput.nativeElement.value = 'test search';
      searchInput.nativeElement.dispatchEvent(new Event('input'));
      
      expect(component.searchTerm).toBe('test search');
      expect(component.hasSearchTerm).toBe(true);
    });

    it('should emit search event with debouncing', fakeAsync(() => {
      spyOn(component.search, 'emit');
      spyOn(component.searchChange, 'emit');
      
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      searchInput.nativeElement.value = 'test';
      searchInput.nativeElement.dispatchEvent(new Event('input'));
      
      // Should not emit immediately
      expect(component.search.emit).not.toHaveBeenCalled();
      
      // Should emit after debounce time
      tick(300);
      expect(component.search.emit).toHaveBeenCalledWith('test');
      expect(component.searchChange.emit).toHaveBeenCalledWith('test');
    }));

    it('should normalize whitespace in search terms', fakeAsync(() => {
      spyOn(component.search, 'emit');
      
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      searchInput.nativeElement.value = '  test search  ';
      searchInput.nativeElement.dispatchEvent(new Event('input'));
      
      tick(300);
      expect(component.search.emit).toHaveBeenCalledWith('test search');
    }));

    it('should handle paste events', () => {
      spyOn(component.search, 'emit');
      
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      const pasteEvent = new ClipboardEvent('paste', {
        clipboardData: new DataTransfer()
      });
      Object.defineProperty(pasteEvent.clipboardData, 'getData', {
        value: () => 'pasted text'
      });
      
      searchInput.nativeElement.dispatchEvent(pasteEvent);
      
      // Wait for the setTimeout in onSearchPaste
      setTimeout(() => {
        expect(component.search.emit).toHaveBeenCalled();
      }, 150);
    });
  });

  describe('Clear Search Functionality', () => {
    it('should clear search when clear button is clicked', () => {
      spyOn(component.clear, 'emit');
      
      // Set a search term first
      component.searchForm.patchValue({ searchTerm: 'test' });
      fixture.detectChanges();
      
      const clearButton = fixture.debugElement.query(By.css('.clear-button'));
      clearButton.nativeElement.click();
      
      expect(component.searchTerm).toBe('');
      expect(component.hasSearchTerm).toBe(false);
      expect(component.clear.emit).toHaveBeenCalled();
    });

    it('should clear search when Escape key is pressed', () => {
      spyOn(component, 'onClearSearch');
      
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
      
      searchInput.nativeElement.dispatchEvent(escapeEvent);
      
      expect(component.onClearSearch).toHaveBeenCalled();
    });

    it('should programmatically clear search', () => {
      spyOn(component.clear, 'emit');
      
      component.searchForm.patchValue({ searchTerm: 'test' });
      component.clearSearch();
      
      expect(component.searchTerm).toBe('');
      expect(component.clear.emit).toHaveBeenCalled();
    });
  });

  describe('Keyboard Shortcuts', () => {
    it('should submit search when Enter key is pressed', () => {
      spyOn(component, 'onSearchSubmit');
      
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
      
      searchInput.nativeElement.dispatchEvent(enterEvent);
      
      expect(component.onSearchSubmit).toHaveBeenCalled();
    });

    it('should handle Ctrl+F to focus search input', () => {
      spyOn(component, 'focusSearchInput');
      
      const ctrlFEvent = new KeyboardEvent('keydown', { 
        key: 'f', 
        ctrlKey: true 
      });
      
      component.onGlobalKeyDown(ctrlFEvent);
      
      expect(component.focusSearchInput).toHaveBeenCalled();
    });

    it('should prevent default behavior for handled keys', () => {
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
      spyOn(escapeEvent, 'preventDefault');
      
      searchInput.nativeElement.dispatchEvent(escapeEvent);
      
      expect(escapeEvent.preventDefault).toHaveBeenCalled();
    });
  });

  describe('Loading States', () => {
    it('should show loading indicator when loading is true', () => {
      component.loading = true;
      component.config.showLoadingIndicator = true;
      fixture.detectChanges();
      
      const loadingContainer = fixture.debugElement.query(By.css('.loading-container'));
      expect(loadingContainer).toBeTruthy();
    });

    it('should hide loading indicator when loading is false', () => {
      component.loading = false;
      component.config.showLoadingIndicator = true;
      fixture.detectChanges();
      
      const loadingContainer = fixture.debugElement.query(By.css('.loading-container'));
      expect(loadingContainer).toBeFalsy();
    });

    it('should show loading text when configured', () => {
      component.loading = true;
      component.config.showLoadingIndicator = true;
      component.showLoadingText = true;
      component.loadingText = 'Custom loading...';
      fixture.detectChanges();
      
      const loadingText = fixture.debugElement.query(By.css('.loading-text'));
      expect(loadingText.nativeElement.textContent).toContain('Custom loading...');
    });

    it('should set loading state programmatically', () => {
      component.setLoading(true);
      expect(component.isLoading()).toBe(true);
      
      component.setLoading(false);
      expect(component.isLoading()).toBe(false);
    });
  });

  describe('Result Count Display', () => {
    it('should show result count when provided', () => {
      component.resultCount = 5;
      component.searchForm.patchValue({ searchTerm: 'test' });
      fixture.detectChanges();
      
      const resultCount = fixture.debugElement.query(By.css('.result-count-text'));
      expect(resultCount.nativeElement.textContent).toContain('5 results found');
    });

    it('should show "No results found" when count is 0', () => {
      component.resultCount = 0;
      component.searchForm.patchValue({ searchTerm: 'test' });
      fixture.detectChanges();
      
      const resultCount = fixture.debugElement.query(By.css('.result-count-text'));
      expect(resultCount.nativeElement.textContent).toContain('No results found');
    });

    it('should handle singular result count', () => {
      component.resultCount = 1;
      component.searchForm.patchValue({ searchTerm: 'test' });
      fixture.detectChanges();
      
      const resultCount = fixture.debugElement.query(By.css('.result-count-text'));
      expect(resultCount.nativeElement.textContent).toContain('1 result found');
    });
  });

  describe('Configuration Options', () => {
    it('should use custom placeholder text', () => {
      component.customPlaceholder = 'Custom placeholder';
      fixture.detectChanges();
      
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      expect(searchInput.nativeElement.placeholder).toBe('Custom placeholder');
    });

    it('should show clear button when configured', () => {
      component.config.showClearButton = true;
      component.searchForm.patchValue({ searchTerm: 'test' });
      fixture.detectChanges();
      
      const clearButton = fixture.debugElement.query(By.css('.clear-button'));
      expect(clearButton).toBeTruthy();
    });

    it('should hide clear button when not configured', () => {
      component.config.showClearButton = false;
      component.searchForm.patchValue({ searchTerm: 'test' });
      fixture.detectChanges();
      
      const clearButton = fixture.debugElement.query(By.css('.clear-button'));
      expect(clearButton).toBeFalsy();
    });

    it('should show search button when configured', () => {
      component.config.showSearchButton = true;
      fixture.detectChanges();
      
      const searchButton = fixture.debugElement.query(By.css('.search-button'));
      expect(searchButton).toBeTruthy();
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA labels', () => {
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      expect(searchInput.nativeElement.getAttribute('aria-label')).toContain('Search');
    });

    it('should have proper ARIA describedby', () => {
      const searchInput = fixture.debugElement.query(By.css('.search-input'));
      expect(searchInput.nativeElement.getAttribute('aria-describedby')).toBe('search-results');
    });

    it('should have live region for loading text', () => {
      component.loading = true;
      component.config.showLoadingIndicator = true;
      fixture.detectChanges();
      
      const loadingText = fixture.debugElement.query(By.css('.loading-text'));
      expect(loadingText.nativeElement.getAttribute('aria-live')).toBe('polite');
    });

    it('should have live region for result count', () => {
      component.resultCount = 5;
      component.searchForm.patchValue({ searchTerm: 'test' });
      fixture.detectChanges();
      
      const resultCount = fixture.debugElement.query(By.css('.search-results-count'));
      expect(resultCount.nativeElement.getAttribute('aria-live')).toBe('polite');
    });
  });

  describe('Utility Methods', () => {
    it('should check if search is active', () => {
      expect(component.isSearchActive()).toBe(false);
      
      component.searchForm.patchValue({ searchTerm: 'test' });
      expect(component.isSearchActive()).toBe(true);
    });

    it('should check if search is in progress', () => {
      expect(component.isSearchInProgress()).toBe(false);
      
      component.loading = true;
      component.searchForm.patchValue({ searchTerm: 'test' });
      expect(component.isSearchInProgress()).toBe(true);
    });

    it('should get keyboard shortcuts help text', () => {
      const helpText = component.getKeyboardShortcutsHelp();
      expect(helpText).toContain('Enter: Search');
      expect(helpText).toContain('Escape: Clear search');
      expect(helpText).toContain('Ctrl+F: Focus search');
    });

    it('should get loading display text', () => {
      component.loading = true;
      component.showLoadingText = true;
      component.loadingText = 'Custom loading...';
      
      expect(component.getLoadingDisplayText()).toBe('Custom loading...');
    });
  });

  describe('Component Lifecycle', () => {
    it('should clean up subscriptions on destroy', () => {
      // Test that ngOnDestroy can be called without errors
      expect(() => {
        component.ngOnDestroy();
      }).not.toThrow();
    });
  });
}); 