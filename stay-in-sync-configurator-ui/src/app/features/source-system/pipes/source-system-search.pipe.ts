import { Injectable } from '@angular/core';
import { SourceSystemDTO } from '../models/sourceSystemDTO';

/**
 * Extended interface for search functionality with additional properties
 * for endpoints, headers, and metadata that may be used in future implementations
 */
interface ExtendedSourceSystemDTO extends SourceSystemDTO {
  endpoints?: any[];
  headers?: any[];
  metadata?: { [key: string]: any };
}

/**
 * Type alias for backward compatibility and type safety
 */
type SearchableSourceSystem = SourceSystemDTO;

/**
 * Configuration options for search functionality
 */
export interface SearchOptions {
  /** Whether search should be case-sensitive (default: false) */
  caseSensitive?: boolean;
  /** Whether to treat search term as regex pattern (default: false) */
  enableRegex?: boolean;
  /** Scope of search across different fields (default: 'all') */
  searchScope?: 'all' | 'names' | 'descriptions' | 'urls' | 'endpoints' | 'headers';
  /** Whether to highlight matches in results (default: false) */
  highlightMatches?: boolean;
}

/**
 * Represents a search result with relevance scoring
 */
export interface SearchResult {
  item: SourceSystemDTO;
  matches: string[];
  score: number;
}

/**
 * Information about search results when no matches are found
 */
export interface NoResultsInfo {
  hasResults: boolean;
  totalItems: number;
  filteredItems: number;
  searchTerm: string;
  searchScope: string;
  suggestions: string[];
  alternativeSearchTerms: string[];
  message: string;
}

/**
 * Detailed count information for search results
 */
export interface SearchResultCount {
  total: number;
  filtered: number;
  searchTerm: string;
  searchScope: string;
  hasResults: boolean;
  displayText: string;
  percentage: number;
  breakdown: {
    byName: number;
    byDescription: number;
    byUrl: number;
    byEndpoints: number;
    byHeaders: number;
  };
}

/**
 * Injectable pipe for filtering and searching source systems
 * Provides comprehensive search functionality with relevance scoring,
 * highlighting, and various search options
 */
@Injectable({
  providedIn: 'root'
})
export class SourceSystemSearchPipe {
  /**
   * Transforms source systems based on search term and options
   * @param sourceSystems - Array of source systems to search through
   * @param searchTerm - Search term to filter by
   * @param options - Search configuration options
   * @returns Filtered and sorted array of source systems
   */
  transform(
    sourceSystems: SearchableSourceSystem[] | null | undefined, 
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): SearchableSourceSystem[] {
    if (!sourceSystems || !searchTerm || searchTerm.trim() === '') {
      return sourceSystems || [];
    }

    const normalizedSearchTerm = this.normalizeSearchTerm(searchTerm, options);
    const searchScope = options.searchScope || 'all';

    const filteredSystems = sourceSystems.filter(system => 
      this.matchesSearchCriteria(system, normalizedSearchTerm, searchScope, options)
    );

    return this.sortByRelevance(filteredSystems, normalizedSearchTerm, searchScope, options);
  }

  /**
   * Normalizes search term based on case sensitivity option
   * @param searchTerm - Original search term
   * @param options - Search options containing case sensitivity setting
   * @returns Normalized search term
   */
  private normalizeSearchTerm(searchTerm: string, options: SearchOptions): string {
    if (options.caseSensitive === true) {
      return searchTerm.trim();
    }
    return searchTerm.trim().toLowerCase();
  }

  /**
   * Checks if a system matches the search criteria
   * @param system - Source system to check
   * @param searchTerm - Normalized search term
   * @param searchScope - Scope of search fields
   * @param options - Search configuration options
   * @returns True if system matches search criteria
   */
  private matchesSearchCriteria(
    system: SearchableSourceSystem, 
    searchTerm: string, 
    searchScope: string,
    options: SearchOptions
  ): boolean {
    const searchableText = this.getSystemSearchableText(system, searchScope, options);
    
    if (options.enableRegex) {
      try {
        const regex = new RegExp(searchTerm, options.caseSensitive === true ? '' : 'i');
        return regex.test(searchableText);
      } catch (error) {
        console.warn('Invalid regex pattern:', searchTerm);
        return searchableText.includes(searchTerm);
      }
    }

    return searchableText.includes(searchTerm);
  }

  /**
   * Extracts searchable text from a source system based on search scope
   * @param system - Source system to extract text from
   * @param searchScope - Scope of fields to include in search
   * @param options - Search configuration options
   * @returns Combined searchable text string
   */
  private getSystemSearchableText(system: SearchableSourceSystem, searchScope: string, options: SearchOptions): string {
    const searchableParts: string[] = [];

    if (searchScope === 'all' || searchScope === 'names') {
      searchableParts.push(system.name || '');
      searchableParts.push(system.description || '');
    }

    if (searchScope === 'all' || searchScope === 'urls') {
      searchableParts.push(system.apiUrl || '');
      searchableParts.push(system.apiType || '');
    }

    const combinedText = searchableParts.join(' ').toLowerCase();
    return options.caseSensitive === true ? combinedText : combinedText.toLowerCase();
  }

  /**
   * Sorts systems by relevance score in descending order
   * @param systems - Array of systems to sort
   * @param searchTerm - Search term for relevance calculation
   * @param searchScope - Search scope for relevance calculation
   * @param options - Search configuration options
   * @returns Sorted array of systems
   */
  private sortByRelevance(
    systems: SearchableSourceSystem[], 
    searchTerm: string, 
    searchScope: string,
    options: SearchOptions
  ): SearchableSourceSystem[] {
    const scoredSystems = systems.map(system => ({
      system,
      score: this.calculateRelevanceScore(system, searchTerm, searchScope, options)
    }));

    return scoredSystems
      .sort((a, b) => b.score - a.score)
      .map(item => item.system);
  }

  /**
   * Calculates relevance score for a system based on search term matches
   * @param system - Source system to score
   * @param searchTerm - Search term to match against
   * @param searchScope - Search scope for scoring
   * @param options - Search configuration options
   * @returns Relevance score (higher is more relevant)
   */
  private calculateRelevanceScore(
    system: SearchableSourceSystem, 
    searchTerm: string, 
    searchScope: string,
    options: SearchOptions
  ): number {
    let score = 0;
    const normalizedSearchTerm = searchTerm.toLowerCase();

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

    return score;
  }

  /**
   * Highlights search matches in text using HTML mark tags
   * @param text - Text to highlight matches in
   * @param searchTerm - Search term to highlight
   * @param options - Search configuration options
   * @returns Text with highlighted matches
   */
  public highlightMatches(text: string, searchTerm: string, options: SearchOptions = {}): string {
    if (!text || !searchTerm || searchTerm.trim() === '') {
      return text;
    }

    const isCaseSensitive = options.caseSensitive === true;
    const flags = isCaseSensitive ? 'g' : 'gi';
    
    if (options.enableRegex) {
      try {
        const regex = new RegExp(`(${searchTerm})`, flags);
        return text.replace(regex, '<mark class="search-highlight">$1</mark>');
      } catch (error) {
        console.warn('Invalid regex pattern:', searchTerm);
        return text;
      }
    }

    const escapedTerm = this.escapeRegex(searchTerm);
    const regex = new RegExp(`(${escapedTerm})`, flags);
    return text.replace(regex, '<mark class="search-highlight">$1</mark>');
  }

  /**
   * Escapes special regex characters in a string
   * @param string - String to escape
   * @returns Escaped string safe for regex use
   */
  private escapeRegex(string: string): string {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  /**
   * Gets basic search statistics
   * @param originalCount - Total number of items before filtering
   * @param filteredCount - Number of items after filtering
   * @param searchTerm - Search term used
   * @returns Object containing search statistics
   */
  public getSearchStats(
    originalCount: number, 
    filteredCount: number, 
    searchTerm: string
  ): { total: number; filtered: number; searchTerm: string } {
    return {
      total: originalCount,
      filtered: filteredCount,
      searchTerm: searchTerm || ''
    };
  }

  /**
   * Gets comprehensive search result count information
   * @param sourceSystems - Array of source systems
   * @param searchTerm - Search term used
   * @param options - Search configuration options
   * @returns Detailed count information
   */
  public getSearchResultCount(
    sourceSystems: SearchableSourceSystem[] | null | undefined,
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): SearchResultCount {
    const total = sourceSystems?.length || 0;
    const filtered = this.transform(sourceSystems, searchTerm, options).length;
    const hasResults = filtered > 0;
    const searchScope = options.searchScope || 'all';
    const percentage = total > 0 ? Math.round((filtered / total) * 100) : 0;

    const displayText = searchTerm && searchTerm.trim() !== '' 
      ? `Found ${filtered} of ${total} systems`
      : `Showing all ${total} systems`;

    return {
      total,
      filtered,
      searchTerm: searchTerm || '',
      searchScope,
      hasResults,
      displayText,
      percentage,
      breakdown: {
        byName: 0,
        byDescription: 0,
        byUrl: 0,
        byEndpoints: 0,
        byHeaders: 0
      }
    };
  }

  /**
   * Gets formatted result count text for display
   * @param sourceSystems - Array of source systems
   * @param searchTerm - Search term used
   * @param options - Search configuration options
   * @returns Formatted count text
   */
  public getResultCountText(
    sourceSystems: SearchableSourceSystem[] | null | undefined,
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): string {
    const count = this.getSearchResultCount(sourceSystems, searchTerm, options);
    return count.displayText;
  }

  /**
   * Checks if search has any results
   * @param sourceSystems - Array of source systems
   * @param searchTerm - Search term used
   * @param options - Search configuration options
   * @returns True if search has results
   */
  public hasSearchResults(
    sourceSystems: SearchableSourceSystem[] | null | undefined,
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): boolean {
    const count = this.getSearchResultCount(sourceSystems, searchTerm, options);
    return count.hasResults;
  }

  /**
   * Gets detailed information when no search results are found
   * @param sourceSystems - Array of source systems
   * @param searchTerm - Search term used
   * @param options - Search configuration options
   * @returns Information about no results situation
   */
  public getNoResultsInfo(
    sourceSystems: SearchableSourceSystem[] | null | undefined,
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): NoResultsInfo {
    const totalItems = sourceSystems?.length || 0;
    const filteredItems = this.transform(sourceSystems, searchTerm, options).length;
    const hasResults = filteredItems > 0;
    const searchScope = options.searchScope || 'all';

    const noResultsInfo: NoResultsInfo = {
      hasResults,
      totalItems,
      filteredItems,
      searchTerm: searchTerm || '',
      searchScope,
      suggestions: [],
      alternativeSearchTerms: [],
      message: ''
    };

    if (hasResults) {
      noResultsInfo.message = `Found ${filteredItems} result${filteredItems === 1 ? '' : 's'} out of ${totalItems} total items`;
    } else {
      if (!searchTerm || searchTerm.trim() === '') {
        noResultsInfo.message = `No search term provided. Showing all ${totalItems} items.`;
      } else if (totalItems === 0) {
        noResultsInfo.message = 'No source systems available to search.';
      } else {
        noResultsInfo.message = `No results found for "${searchTerm}". Try adjusting your search term.`;
      }
    }

    return noResultsInfo;
  }

  /**
   * Gets a simple "no results found" message
   * @param sourceSystems - Array of source systems
   * @param searchTerm - Search term used
   * @param options - Search configuration options
   * @returns Simple no results message
   */
  public getNoResultsMessage(
    sourceSystems: SearchableSourceSystem[] | null | undefined,
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): string {
    const noResultsInfo = this.getNoResultsInfo(sourceSystems, searchTerm, options);
    return noResultsInfo.message;
  }

  /**
   * Checks if search has no results
   * @param sourceSystems - Array of source systems
   * @param searchTerm - Search term used
   * @param options - Search configuration options
   * @returns True if search has no results
   */
  public hasNoResults(
    sourceSystems: SearchableSourceSystem[] | null | undefined,
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): boolean {
    const noResultsInfo = this.getNoResultsInfo(sourceSystems, searchTerm, options);
    return !noResultsInfo.hasResults;
  }

  /**
   * Highlights search matches in a source system object
   * @param system - Source system to highlight matches in
   * @param searchTerm - Search term to highlight
   * @param options - Search configuration options
   * @returns Source system with highlighted matches
   */
  public highlightSourceSystemMatches(
    system: SearchableSourceSystem, 
    searchTerm: string, 
    options: SearchOptions = {}
  ): SearchableSourceSystem {
    if (!searchTerm || searchTerm.trim() === '' || !options.highlightMatches) {
      return system;
    }

    const highlightedSystem = { ...system };

    if (highlightedSystem.name) {
      highlightedSystem.name = this.highlightMatches(highlightedSystem.name, searchTerm, options);
    }

    if (highlightedSystem.description) {
      highlightedSystem.description = this.highlightMatches(highlightedSystem.description, searchTerm, options);
    }

    if (highlightedSystem.apiUrl) {
      highlightedSystem.apiUrl = this.highlightMatches(highlightedSystem.apiUrl, searchTerm, options);
    }

    return highlightedSystem;
  }

  /**
   * Highlights search matches in an array of source systems
   * @param systems - Array of source systems to highlight matches in
   * @param searchTerm - Search term to highlight
   * @param options - Search configuration options
   * @returns Array of source systems with highlighted matches
   */
  public highlightSourceSystemsMatches(
    systems: SearchableSourceSystem[], 
    searchTerm: string, 
    options: SearchOptions = {}
  ): SearchableSourceSystem[] {
    if (!searchTerm || searchTerm.trim() === '' || !options.highlightMatches) {
      return systems;
    }

    return systems.map(system => this.highlightSourceSystemMatches(system, searchTerm, options));
  }

  /**
   * Gets default search options configuration
   * @returns Default search options object
   */
  public getDefaultSearchOptions(): SearchOptions {
    return {
      caseSensitive: false,
      enableRegex: false,
      searchScope: 'all',
      highlightMatches: false
    };
  }

  /**
   * Checks if case-insensitive search is the default behavior
   * @returns True if case-insensitive is default
   */
  public isCaseInsensitiveByDefault(): boolean {
    return true;
  }
} 