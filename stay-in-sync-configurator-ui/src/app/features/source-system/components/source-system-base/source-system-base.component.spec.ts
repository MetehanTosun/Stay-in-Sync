// biome-ignore lint/style/useImportType: <explanation>
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { SourceSystemBaseComponent } from './source-system-base.component';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { SourceSystemSearchPipe, SearchOptions, SearchResultCount } from '../../pipes/source-system-search.pipe';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { SourceSystem } from '../../models/sourceSystem';

describe('SourceSystemBaseComponent', () => {
  let component: SourceSystemBaseComponent;
  let fixture: ComponentFixture<SourceSystemBaseComponent>;
  let mockSourceSystemService: jasmine.SpyObj<SourceSystemResourceService>;
  let mockEndpointService: jasmine.SpyObj<SourceSystemEndpointResourceService>;
  let mockErrorService: jasmine.SpyObj<HttpErrorService>;
  let mockSearchPipe: jasmine.SpyObj<SourceSystemSearchPipe>;

  const mockSourceSystems: SourceSystem[] = [
    {
      id: 1,
      name: 'Test System 1',
      apiUrl: 'https://api.test1.com',
      description: 'Test system for unit testing',
      apiType: 'REST'
    },
    {
      id: 2,
      name: 'Test System 2',
      apiUrl: 'https://api.test2.com',
      description: 'Another test system',
      apiType: 'SOAP'
    }
  ];

  const mockSourceSystemDTOs: SourceSystemDTO[] = [
    {
      id: 1,
      name: 'Test System 1',
      apiUrl: 'https://api.test1.com',
      description: 'Test system for unit testing',
      apiType: 'REST'
    },
    {
      id: 2,
      name: 'Test System 2',
      apiUrl: 'https://api.test2.com',
      description: 'Another test system',
      apiType: 'SOAP'
    }
  ];

  beforeEach(async () => {
    const sourceSystemSpy = jasmine.createSpyObj('SourceSystemResourceService', ['apiConfigSourceSystemGet', 'apiConfigSourceSystemIdDelete']);
    const endpointSpy = jasmine.createSpyObj('SourceSystemEndpointResourceService', ['getEndpoints']);
    const errorSpy = jasmine.createSpyObj('HttpErrorService', ['handleError']);
    const searchPipeSpy = jasmine.createSpyObj('SourceSystemSearchPipe', [
      'transform', 'getSearchResultCount', 'highlightMatches', 'searchEndpoints', 
      'getNoResultsInfo', 'calculateRelevanceScore', 'highlightSourceSystemsMatches'
    ]);

    await TestBed.configureTestingModule({
      imports: [SourceSystemBaseComponent, ReactiveFormsModule],
      providers: [
        { provide: SourceSystemResourceService, useValue: sourceSystemSpy },
        { provide: SourceSystemEndpointResourceService, useValue: endpointSpy },
        { provide: HttpErrorService, useValue: errorSpy },
        { provide: SourceSystemSearchPipe, useValue: searchPipeSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceSystemBaseComponent);
    component = fixture.componentInstance;
    
    mockSourceSystemService = TestBed.inject(SourceSystemResourceService) as jasmine.SpyObj<SourceSystemResourceService>;
    mockEndpointService = TestBed.inject(SourceSystemEndpointResourceService) as jasmine.SpyObj<SourceSystemEndpointResourceService>;
    mockErrorService = TestBed.inject(HttpErrorService) as jasmine.SpyObj<HttpErrorService>;
    mockSearchPipe = TestBed.inject(SourceSystemSearchPipe) as jasmine.SpyObj<SourceSystemSearchPipe>;

    // Setup default mock responses
    mockSourceSystemService.apiConfigSourceSystemGet.and.returnValue(of(mockSourceSystems));
    mockSearchPipe.transform.and.returnValue(mockSourceSystemDTOs);
    mockSearchPipe.getSearchResultCount.and.returnValue({
      total: 2,
      filtered: 2,
      searchTerm: '',
      searchScope: 'all',
      hasResults: true,
      displayText: 'Showing 2 results',
      percentage: 100,
      breakdown: {
        byName: 2,
        byDescription: 0,
        byUrl: 0,
        byEndpoints: 0,
        byHeaders: 0
      }
    });
    mockSearchPipe.highlightMatches.and.returnValue('highlighted text');
    mockSearchPipe.searchEndpoints.and.returnValue([]);
    mockSearchPipe.getNoResultsInfo.and.returnValue({
      hasResults: true,
      totalItems: 2,
      filteredItems: 2,
      searchTerm: '',
      searchScope: 'all',
      suggestions: [],
      alternativeSearchTerms: [],
      message: 'Found 2 results'
    });
    mockSearchPipe.calculateRelevanceScore.and.returnValue(0.8);
    mockSearchPipe.highlightSourceSystemsMatches.and.returnValue(mockSourceSystemDTOs);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Search Functionality', () => {
    beforeEach(() => {
      component.systems = mockSourceSystemDTOs;
      component.filteredSystems = [...mockSourceSystemDTOs];
    });

    it('should initialize search state correctly', () => {
      expect(component.searchTerm).toBe('');
      expect(component.isSearchActive).toBe(false);
      expect(component.filteredSystems).toEqual(mockSourceSystemDTOs);
    });

    it('should handle search term change', () => {
      const searchTerm = 'test';
      spyOn(component, 'updateFilteredSystems');
      spyOn(component, 'saveSearchState');

      component.onSearchChange(searchTerm);

      expect(component.searchTerm).toBe(searchTerm);
      expect(component.isSearchActive).toBe(true);
      expect(component.updateFilteredSystems).toHaveBeenCalled();
      expect(component.saveSearchState).toHaveBeenCalled();
    });

    it('should handle search clear', () => {
      component.searchTerm = 'test';
      component.isSearchActive = true;
      spyOn(component, 'updateFilteredSystems');
      spyOn(component, 'saveSearchState');

      component.onSearchClear();

      expect(component.searchTerm).toBe('');
      expect(component.isSearchActive).toBe(false);
      expect(component.updateFilteredSystems).toHaveBeenCalled();
      expect(component.saveSearchState).toHaveBeenCalled();
    });

    it('should update filtered systems correctly', () => {
      component.searchTerm = 'test';
      component.isSearchActive = true;
      mockSearchPipe.transform.and.returnValue([mockSourceSystemDTOs[0]]);

      component.updateFilteredSystems();

      expect(mockSearchPipe.transform).toHaveBeenCalledWith(mockSourceSystemDTOs, 'test', component.searchOptions);
      expect(component.filteredSystems).toEqual([mockSourceSystemDTOs[0]]);
    });

    it('should update search result count correctly', () => {
      const mockResultCount: SearchResultCount = {
        total: 2,
        filtered: 1,
        searchTerm: 'test',
        searchScope: 'all',
        hasResults: true,
        displayText: 'Showing 1 of 2 results',
        percentage: 50,
        breakdown: {
          byName: 1,
          byDescription: 0,
          byUrl: 0,
          byEndpoints: 0,
          byHeaders: 0
        }
      };

      mockSearchPipe.getSearchResultCount.and.returnValue(mockResultCount);

      component.updateSearchResultCount();

      expect(mockSearchPipe.getSearchResultCount).toHaveBeenCalledWith(mockSourceSystemDTOs, component.searchTerm, component.searchOptions);
      expect(component.searchResultCount).toEqual(mockResultCount);
    });

    it('should get display systems correctly', () => {
      component.filteredSystems = [mockSourceSystemDTOs[0]];

      const result = component.getDisplaySystems();

      expect(result).toEqual([mockSourceSystemDTOs[0]]);
    });

    it('should perform advanced search correctly', () => {
      const searchTerm = 'advanced';
      const options: SearchOptions = { caseSensitive: true };
      spyOn(component, 'updateFilteredSystems');
      spyOn(component, 'saveSearchState');

      component.performAdvancedSearch(searchTerm, options);

      expect(component.searchTerm).toBe(searchTerm);
      expect(component.searchOptions).toEqual(jasmine.objectContaining(options));
      expect(component.isSearchActive).toBe(true);
      expect(component.updateFilteredSystems).toHaveBeenCalled();
      expect(component.saveSearchState).toHaveBeenCalled();
    });

    it('should search by name correctly', () => {
      spyOn(component, 'performAdvancedSearch');

      component.searchByName('test');

      expect(component.performAdvancedSearch).toHaveBeenCalledWith('test', { searchScope: 'names' });
    });

    it('should search by description correctly', () => {
      spyOn(component, 'performAdvancedSearch');

      component.searchByDescription('test');

      expect(component.performAdvancedSearch).toHaveBeenCalledWith('test', { searchScope: 'descriptions' });
    });

    it('should search by URL correctly', () => {
      spyOn(component, 'performAdvancedSearch');

      component.searchByUrl('test');

      expect(component.performAdvancedSearch).toHaveBeenCalledWith('test', { searchScope: 'urls' });
    });

    it('should search by endpoints correctly', () => {
      spyOn(component, 'performAdvancedSearch');

      component.searchByEndpoints('test');

      expect(component.performAdvancedSearch).toHaveBeenCalledWith('test', { searchScope: 'endpoints' });
    });

    it('should search by headers correctly', () => {
      spyOn(component, 'performAdvancedSearch');

      component.searchByHeaders('test');

      expect(component.performAdvancedSearch).toHaveBeenCalledWith('test', { searchScope: 'headers' });
    });

    it('should search with regex correctly', () => {
      spyOn(component, 'performAdvancedSearch');

      component.searchWithRegex('test.*');

      expect(component.performAdvancedSearch).toHaveBeenCalledWith('test.*', { enableRegex: true });
    });

    it('should search case sensitive correctly', () => {
      spyOn(component, 'performAdvancedSearch');

      component.searchCaseSensitive('Test');

      expect(component.performAdvancedSearch).toHaveBeenCalledWith('Test', { caseSensitive: true });
    });

    it('should get search suggestions correctly', () => {
      component.searchTerm = 'test';
      const mockSuggestions = ['suggestion1', 'suggestion2'];
      mockSearchPipe.getNoResultsInfo.and.returnValue({
        hasResults: true,
        totalItems: 2,
        filteredItems: 2,
        searchTerm: 'test',
        searchScope: 'all',
        suggestions: mockSuggestions,
        alternativeSearchTerms: [],
        message: 'Found results'
      });

      const result = component.getSearchSuggestions();

      expect(mockSearchPipe.getNoResultsInfo).toHaveBeenCalledWith(mockSourceSystemDTOs, 'test', component.searchOptions);
      expect(result).toEqual(mockSuggestions);
    });

    it('should get alternative search terms correctly', () => {
      component.searchTerm = 'test';
      const mockAlternatives = ['alternative1', 'alternative2'];
      mockSearchPipe.getNoResultsInfo.and.returnValue({
        hasResults: true,
        totalItems: 2,
        filteredItems: 2,
        searchTerm: 'test',
        searchScope: 'all',
        suggestions: [],
        alternativeSearchTerms: mockAlternatives,
        message: 'Found results'
      });

      const result = component.getAlternativeSearchTerms();

      expect(mockSearchPipe.getNoResultsInfo).toHaveBeenCalledWith(mockSourceSystemDTOs, 'test', component.searchOptions);
      expect(result).toEqual(mockAlternatives);
    });

    it('should return empty arrays for short search terms', () => {
      component.searchTerm = 'a';

      const suggestions = component.getSearchSuggestions();
      const alternatives = component.getAlternativeSearchTerms();

      expect(suggestions).toEqual([]);
      expect(alternatives).toEqual([]);
    });

    it('should get search breakdown correctly', () => {
      component.searchResultCount = {
        total: 2,
        filtered: 1,
        searchTerm: 'test',
        searchScope: 'all',
        hasResults: true,
        displayText: 'Showing 1 of 2 results',
        percentage: 50,
        breakdown: {
          byName: 1,
          byDescription: 0,
          byUrl: 0,
          byEndpoints: 0,
          byHeaders: 0
        }
      };

      const result = component.getSearchBreakdown();

      expect(result).toEqual(component.searchResultCount.breakdown);
    });

    it('should return null for search breakdown when no result count', () => {
      component.searchResultCount = null;

      const result = component.getSearchBreakdown();

      expect(result).toBeNull();
    });

    it('should check for specific match types correctly', () => {
      component.searchResultCount = {
        total: 2,
        filtered: 1,
        searchTerm: 'test',
        searchScope: 'all',
        hasResults: true,
        displayText: 'Showing 1 of 2 results',
        percentage: 50,
        breakdown: {
          byName: 1,
          byDescription: 0,
          byUrl: 1,
          byEndpoints: 0,
          byHeaders: 0
        }
      };

      expect(component.hasNameMatches()).toBe(true);
      expect(component.hasDescriptionMatches()).toBe(false);
      expect(component.hasUrlMatches()).toBe(true);
      expect(component.hasEndpointMatches()).toBe(false);
      expect(component.hasHeaderMatches()).toBe(false);
    });

    it('should get search breakdown text correctly', () => {
      component.searchResultCount = {
        total: 2,
        filtered: 1,
        searchTerm: 'test',
        searchScope: 'all',
        hasResults: true,
        displayText: 'Showing 1 of 2 results',
        percentage: 50,
        breakdown: {
          byName: 1,
          byDescription: 1,
          byUrl: 0,
          byEndpoints: 0,
          byHeaders: 0
        }
      };

      const result = component.getSearchBreakdownText();

      expect(result).toBe('1 by name, 1 by description');
    });

    it('should return empty string for search breakdown text when no breakdown', () => {
      component.searchResultCount = null;

      const result = component.getSearchBreakdownText();

      expect(result).toBe('');
    });

    it('should check for search results correctly', () => {
      component.searchResultCount = {
        total: 2,
        filtered: 1,
        searchTerm: 'test',
        searchScope: 'all',
        hasResults: true,
        displayText: 'Showing 1 of 2 results',
        percentage: 50,
        breakdown: {
          byName: 1,
          byDescription: 0,
          byUrl: 0,
          byEndpoints: 0,
          byHeaders: 0
        }
      };

      expect(component.hasSearchResults()).toBe(true);
    });

    it('should return false for search results when no result count', () => {
      component.searchResultCount = null;

      expect(component.hasSearchResults()).toBe(false);
    });

    it('should get empty message correctly', () => {
      component.isSearchActive = true;
      expect(component.getEmptyMessage()).toBe('No matching source systems found');

      component.isSearchActive = false;
      expect(component.getEmptyMessage()).toBe('No source systems available');
    });

    it('should check if row is highlighted correctly', () => {
      component.isSearchActive = true;
      component.searchOptions = { highlightMatches: true };
      mockSearchPipe.transform.and.returnValue([mockSourceSystemDTOs[0]]);

      const result = component.isHighlightedRow(mockSourceSystemDTOs[0]);

      expect(mockSearchPipe.transform).toHaveBeenCalledWith([mockSourceSystemDTOs[0]], component.searchTerm, component.searchOptions);
      expect(result).toBe(true);
    });

    it('should return false for highlighted row when search is not active', () => {
      component.isSearchActive = false;

      const result = component.isHighlightedRow(mockSourceSystemDTOs[0]);

      expect(result).toBe(false);
    });

    it('should get highlighted text correctly', () => {
      component.isSearchActive = true;
      component.searchOptions = { highlightMatches: true };
      mockSearchPipe.highlightMatches.and.returnValue('highlighted text');

      const result = component.getHighlightedText('test text', 'name');

      expect(mockSearchPipe.highlightMatches).toHaveBeenCalledWith('test text', component.searchTerm, component.searchOptions);
      expect(result).toBe('highlighted text');
    });

    it('should return original text when highlighting is disabled', () => {
      component.isSearchActive = true;
      component.searchOptions = { highlightMatches: false };

      const result = component.getHighlightedText('test text', 'name');

      expect(result).toBe('test text');
    });

    it('should get relevance score correctly', () => {
      component.isSearchActive = true;
      mockSearchPipe.transform.and.returnValue([mockSourceSystemDTOs[0]]);

      const result = component.getRelevanceScore(mockSourceSystemDTOs[0]);

      expect(mockSearchPipe.transform).toHaveBeenCalledWith([mockSourceSystemDTOs[0]], component.searchTerm, component.searchOptions);
      expect(result).toBe(100);
    });

    it('should return 0 for relevance score when search is not active', () => {
      component.isSearchActive = false;

      const result = component.getRelevanceScore(mockSourceSystemDTOs[0]);

      expect(result).toBe(0);
    });

    it('should get endpoint match count correctly', () => {
      component.isSearchActive = true;
      mockSearchPipe.searchEndpoints.and.returnValue([
        { system: mockSourceSystemDTOs[0], matchingEndpoints: [{ id: 1, name: 'endpoint1' }] }
      ]);

      const result = component.getEndpointMatchCount(mockSourceSystemDTOs[0]);

      expect(mockSearchPipe.searchEndpoints).toHaveBeenCalledWith([mockSourceSystemDTOs[0]], component.searchTerm, component.searchOptions);
      expect(result).toBe(1);
    });

    it('should return 0 for endpoint match count when search is not active', () => {
      component.isSearchActive = false;

      const result = component.getEndpointMatchCount(mockSourceSystemDTOs[0]);

      expect(result).toBe(0);
    });
  });

  describe('Search State Management', () => {
    beforeEach(() => {
      spyOn(localStorage, 'getItem').and.returnValue(null);
      spyOn(localStorage, 'setItem');
      spyOn(localStorage, 'removeItem');
    });

    it('should save search state correctly', () => {
      component.searchTerm = 'test';
      component.searchOptions = { caseSensitive: true };
      component.isSearchActive = true;

      component.saveSearchState();

      expect(localStorage.setItem).toHaveBeenCalledWith(
        'sourceSystemSearchState',
        jasmine.stringContaining('"searchTerm":"test"')
      );
    });

    it('should load search state correctly', () => {
      const mockState = {
        searchTerm: 'test',
        searchOptions: { caseSensitive: true },
        isSearchActive: true,
        timestamp: new Date().toISOString(),
        version: '1.0'
      };
      spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(mockState));
      spyOn(component, 'updateFilteredSystems');

      component.loadSearchState();

      expect(component.searchTerm).toBe('test');
      expect(component.searchOptions).toEqual(jasmine.objectContaining({ caseSensitive: true }));
      expect(component.isSearchActive).toBe(true);
      expect(component.updateFilteredSystems).toHaveBeenCalled();
    });

    it('should handle invalid search state gracefully', () => {
      spyOn(localStorage, 'getItem').and.returnValue('invalid json');
      spyOn(console, 'warn');

      component.loadSearchState();

      expect(console.warn).toHaveBeenCalled();
      expect(localStorage.removeItem).toHaveBeenCalledWith('sourceSystemSearchState');
    });

    it('should clear search state correctly', () => {
      component.clearSearchState();

      expect(localStorage.removeItem).toHaveBeenCalledWith('sourceSystemSearchState');
    });

    it('should validate search state correctly', () => {
      const validState = {
        searchTerm: 'test',
        searchOptions: { caseSensitive: true },
        isSearchActive: true
      };

      const invalidState = {
        searchTerm: 123, // Should be string
        searchOptions: 'not an object',
        isSearchActive: 'not a boolean'
      };

      expect(component['isValidSearchState'](validState)).toBe(true);
      expect(component['isValidSearchState'](invalidState)).toBe(false);
      expect(component['isValidSearchState'](null)).toBe(false);
    });
  });

  describe('Responsive Design', () => {
    beforeEach(() => {
      spyOn(window, 'innerWidth').and.returnValue(1024);
      spyOn(window, 'innerHeight').and.returnValue(768);
    });

    it('should detect mobile device correctly', () => {
      Object.defineProperty(window, 'innerWidth', { value: 768, configurable: true });
      expect(component.isMobileDevice()).toBe(true);

      Object.defineProperty(window, 'innerWidth', { value: 1024, configurable: true });
      expect(component.isMobileDevice()).toBe(false);
    });

    it('should detect tablet device correctly', () => {
      Object.defineProperty(window, 'innerWidth', { value: 900, configurable: true });
      expect(component.isTabletDevice()).toBe(true);

      Object.defineProperty(window, 'innerWidth', { value: 1200, configurable: true });
      expect(component.isTabletDevice()).toBe(false);
    });

    it('should detect desktop device correctly', () => {
      Object.defineProperty(window, 'innerWidth', { value: 1200, configurable: true });
      expect(component.isDesktopDevice()).toBe(true);

      Object.defineProperty(window, 'innerWidth', { value: 900, configurable: true });
      expect(component.isDesktopDevice()).toBe(false);
    });

    it('should detect small screen correctly', () => {
      Object.defineProperty(window, 'innerWidth', { value: 480, configurable: true });
      expect(component.isSmallScreen()).toBe(true);

      Object.defineProperty(window, 'innerWidth', { value: 600, configurable: true });
      expect(component.isSmallScreen()).toBe(false);
    });

    it('should detect landscape mode correctly', () => {
      Object.defineProperty(window, 'innerHeight', { value: 400, configurable: true });
      Object.defineProperty(window, 'innerWidth', { value: 800, configurable: true });
      expect(component.isLandscapeMode()).toBe(true);

      Object.defineProperty(window, 'innerHeight', { value: 800, configurable: true });
      Object.defineProperty(window, 'innerWidth', { value: 400, configurable: true });
      expect(component.isLandscapeMode()).toBe(false);
    });

    it('should detect touch device correctly', () => {
      spyOnProperty(navigator, 'maxTouchPoints').and.returnValue(5);
      expect(component.isTouchDevice()).toBe(true);

      spyOnProperty(navigator, 'maxTouchPoints').and.returnValue(0);
      expect(component.isTouchDevice()).toBe(false);
    });

    it('should get responsive placeholder correctly', () => {
      Object.defineProperty(window, 'innerWidth', { value: 360, configurable: true });
      expect(component.getResponsivePlaceholder()).toBe('Search...');

      Object.defineProperty(window, 'innerWidth', { value: 600, configurable: true });
      expect(component.getResponsivePlaceholder()).toBe('Search source systems...');

      Object.defineProperty(window, 'innerWidth', { value: 1200, configurable: true });
      expect(component.getResponsivePlaceholder()).toBe('Search source systems by name, description, or API URL...');
    });

    it('should get responsive table rows correctly', () => {
      Object.defineProperty(window, 'innerWidth', { value: 360, configurable: true });
      expect(component.getResponsiveTableRows()).toBe(5);

      Object.defineProperty(window, 'innerWidth', { value: 600, configurable: true });
      expect(component.getResponsiveTableRows()).toBe(8);

      Object.defineProperty(window, 'innerWidth', { value: 1200, configurable: true });
      expect(component.getResponsiveTableRows()).toBe(10);
    });

    it('should determine if advanced search should be shown', () => {
      Object.defineProperty(window, 'innerWidth', { value: 360, configurable: true });
      expect(component.shouldShowAdvancedSearch()).toBe(false);

      Object.defineProperty(window, 'innerWidth', { value: 1200, configurable: true });
      expect(component.shouldShowAdvancedSearch()).toBe(true);
    });

    it('should determine if search breakdown should be shown', () => {
      Object.defineProperty(window, 'innerWidth', { value: 360, configurable: true });
      expect(component.shouldShowSearchBreakdown()).toBe(false);

      Object.defineProperty(window, 'innerWidth', { value: 600, configurable: true });
      expect(component.shouldShowSearchBreakdown()).toBe(true);
    });

    it('should determine if search actions should be shown', () => {
      Object.defineProperty(window, 'innerWidth', { value: 360, configurable: true });
      expect(component.shouldShowSearchActions()).toBe(false);

      Object.defineProperty(window, 'innerWidth', { value: 600, configurable: true });
      expect(component.shouldShowSearchActions()).toBe(true);
    });

    it('should get responsive search options correctly', () => {
      component.searchOptions = { highlightMatches: true, searchScope: 'names' };

      Object.defineProperty(window, 'innerWidth', { value: 360, configurable: true });
      const smallScreenOptions = component.getResponsiveSearchOptions();
      expect(smallScreenOptions.highlightMatches).toBe(false);
      expect(smallScreenOptions.searchScope).toBe('all');

      Object.defineProperty(window, 'innerWidth', { value: 1200, configurable: true });
      const largeScreenOptions = component.getResponsiveSearchOptions();
      expect(largeScreenOptions.highlightMatches).toBe(true);
      expect(largeScreenOptions.searchScope).toBe('names');
    });
  });

  describe('Search Performance Monitoring', () => {
    beforeEach(() => {
      component.systems = mockSourceSystemDTOs;
      component.filteredSystems = [...mockSourceSystemDTOs];
    });

    it('should get search performance correctly', () => {
      component.searchTerm = 'test';
      component.searchStartTime = 1000;
      component.searchEndTime = 1100;

      const result = component.getSearchPerformance();

      expect(result.duration).toBe(100);
      expect(result.resultCount).toBe(2);
      expect(result.searchTerm).toBe('test');
    });

    it('should update filtered systems with performance monitoring', () => {
      component.searchTerm = 'test';
      component.isSearchActive = true;
      spyOn(console, 'warn');
      spyOn(component, 'updateSearchResultCount');

      component['updateFilteredSystemsWithPerformance']();

      expect(mockSearchPipe.transform).toHaveBeenCalled();
      expect(component.updateSearchResultCount).toHaveBeenCalled();
    });
  });

  describe('Search Actions', () => {
    beforeEach(() => {
      component.systems = mockSourceSystemDTOs;
      component.filteredSystems = [mockSourceSystemDTOs[0]];
      component.searchTerm = 'test';
      component.isSearchActive = true;
    });

    it('should export search results correctly', () => {
      spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
      spyOn(window.URL, 'revokeObjectURL');
      const mockAnchor = {
        href: '',
        download: '',
        click: jasmine.createSpy('click')
      };
      spyOn(document, 'createElement').and.returnValue(mockAnchor as any);

      component.exportSearchResults();

      expect(window.URL.createObjectURL).toHaveBeenCalled();
      expect(mockAnchor.click).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalled();
    });

    it('should save search query correctly', () => {
      spyOn(localStorage, 'getItem').and.returnValue('[]');
      spyOn(localStorage, 'setItem');

      component.saveSearchQuery();

      expect(localStorage.setItem).toHaveBeenCalledWith(
        'savedSourceSystemSearches',
        jasmine.stringContaining('"term":"test"')
      );
    });

    it('should load saved searches correctly', () => {
      const mockSavedSearches = [
        { term: 'test1', options: {}, timestamp: new Date().toISOString() },
        { term: 'test2', options: {}, timestamp: new Date().toISOString() }
      ];
      spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(mockSavedSearches));

      const result = component.loadSavedSearches();

      expect(result).toEqual(mockSavedSearches);
    });

    it('should apply saved search correctly', () => {
      const savedSearch = {
        term: 'saved test',
        options: { caseSensitive: true },
        timestamp: new Date().toISOString()
      };
      spyOn(component, 'updateFilteredSystems');
      spyOn(component, 'saveSearchState');

      component.applySavedSearch(savedSearch);

      expect(component.searchTerm).toBe('saved test');
      expect(component.searchOptions).toEqual(jasmine.objectContaining({ caseSensitive: true }));
      expect(component.isSearchActive).toBe(true);
      expect(component.updateFilteredSystems).toHaveBeenCalled();
      expect(component.saveSearchState).toHaveBeenCalled();
    });

    it('should clear saved searches correctly', () => {
      spyOn(localStorage, 'removeItem');

      component.clearSavedSearches();

      expect(localStorage.removeItem).toHaveBeenCalledWith('savedSourceSystemSearches');
    });

    it('should get search state info correctly', () => {
      const mockState = {
        searchTerm: 'test',
        timestamp: new Date().toISOString()
      };
      spyOn(localStorage, 'getItem')
        .withArgs('sourceSystemSearchState').and.returnValue(JSON.stringify(mockState))
        .withArgs('savedSourceSystemSearches').and.returnValue('[]');

      const result = component.getSearchStateInfo();

      expect(result.hasSavedState).toBe(true);
      expect(result.hasBackup).toBe(false);
      expect(result.lastSaved).toBe(mockState.timestamp);
      expect(result.stateSize).toBeGreaterThan(0);
    });
  });

  describe('Component Lifecycle', () => {
    it('should save search state on destroy', () => {
      spyOn(component, 'saveSearchState');
      spyOn(console, 'debug');

      component.ngOnDestroy();

      expect(component.saveSearchState).toHaveBeenCalled();
      expect(console.debug).toHaveBeenCalledWith('Component destroyed, search state saved');
    });

    it('should save search state on beforeunload', () => {
      spyOn(component, 'saveSearchState');

      component.onBeforeUnload();

      expect(component.saveSearchState).toHaveBeenCalled();
    });

    it('should handle storage change events correctly', () => {
      const mockEvent = new StorageEvent('storage', {
        key: 'sourceSystemSearchState',
        newValue: JSON.stringify({
          searchTerm: 'new test',
          searchOptions: {},
          isSearchActive: true,
          timestamp: new Date().toISOString(),
          version: '1.0'
        }),
        oldValue: null,
        storageArea: localStorage
      });
      spyOn(component, 'loadSearchState');

      component.onStorageChange(mockEvent);

      expect(component.loadSearchState).toHaveBeenCalled();
    });

    it('should not handle storage change events for different keys', () => {
      const mockEvent = new StorageEvent('storage', {
        key: 'differentKey',
        newValue: 'test',
        oldValue: null,
        storageArea: localStorage
      });
      spyOn(component, 'loadSearchState');

      component.onStorageChange(mockEvent);

      expect(component.loadSearchState).not.toHaveBeenCalled();
    });
  });
});
