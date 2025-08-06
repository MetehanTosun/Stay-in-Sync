import { Injectable } from '@angular/core';
import { SourceSystemDTO } from '../models/sourceSystemDTO';

// Extended interface for search functionality
interface ExtendedSourceSystemDTO extends SourceSystemDTO {
  endpoints?: any[];
  headers?: any[];
  metadata?: { [key: string]: any };
}

// Type alias for backward compatibility
type SearchableSourceSystem = SourceSystemDTO;

export interface SearchOptions {
  caseSensitive?: boolean; // Default: false (case-insensitive)
  enableRegex?: boolean; // Default: false
  searchScope?: 'all' | 'names' | 'descriptions' | 'urls' | 'endpoints' | 'headers'; // Default: 'all'
  highlightMatches?: boolean; // Default: false
}

export interface SearchResult {
  item: SourceSystemDTO;
  matches: string[];
  score: number;
}

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

@Injectable({
  providedIn: 'root'
})
export class SourceSystemSearchPipe {
  /**
   * Transform source systems based on search term and options
   */
  transform(
    sourceSystems: SearchableSourceSystem[] | null | undefined, 
    searchTerm: string | null | undefined,
    options: SearchOptions = {}
  ): SearchableSourceSystem[] {
    // Return original array if no search term or empty search term
    if (!sourceSystems || !searchTerm || searchTerm.trim() === '') {
      return sourceSystems || [];
    }

    const normalizedSearchTerm = this.normalizeSearchTerm(searchTerm, options);
    const searchScope = options.searchScope || 'all';

    // Filter systems based on search criteria
    const filteredSystems = sourceSystems.filter(system => 
      this.matchesSearchCriteria(system, normalizedSearchTerm, searchScope, options)
    );

    // Sort by relevance
    return this.sortByRelevance(filteredSystems, normalizedSearchTerm, searchScope, options);
  }

  /**
   * Normalize search term based on options
   */
  private normalizeSearchTerm(searchTerm: string, options: SearchOptions): string {
    if (options.caseSensitive === true) {
      return searchTerm.trim();
    }
    return searchTerm.trim().toLowerCase();
  }

  /**
   * Check if a system matches the search criteria
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
   * Gets all searchable text from a source system based on search scope
   */
  private getSystemSearchableText(system: SearchableSourceSystem, searchScope: string, options: SearchOptions): string {
    const searchableParts: string[] = [];

    // Always include basic system information
    if (searchScope === 'all' || searchScope === 'names') {
      searchableParts.push(system.name || '');
      searchableParts.push(system.description || '');
    }

    if (searchScope === 'all' || searchScope === 'urls') {
      searchableParts.push(system.apiUrl || '');
      searchableParts.push(system.apiType || '');
    }

    // Note: endpoints and headers search is not available in current SourceSystemDTO
    // These would need to be implemented separately if needed

    const combinedText = searchableParts.join(' ').toLowerCase();
    return options.caseSensitive === true ? combinedText : combinedText.toLowerCase();
  }

  /**
   * Sort systems by relevance score
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
   * Calculate relevance score for a system
   */
  private calculateRelevanceScore(
    system: SearchableSourceSystem, 
    searchTerm: string, 
    searchScope: string,
    options: SearchOptions
  ): number {
    let score = 0;
    const normalizedSearchTerm = searchTerm.toLowerCase();

    // Name matches (highest priority)
    if (system.name) {
      const normalizedName = system.name.toLowerCase();
      if (normalizedName === normalizedSearchTerm) {
        score += 100;
      } else if (normalizedName.includes(normalizedSearchTerm)) {
        score += 50;
      }
    }

    // Description matches
    if (system.description) {
      const normalizedDesc = system.description.toLowerCase();
      if (normalizedDesc.includes(normalizedSearchTerm)) {
        score += 30;
      }
    }

    // URL matches
    if (system.apiUrl) {
      const normalizedUrl = system.apiUrl.toLowerCase();
      if (normalizedUrl.includes(normalizedSearchTerm)) {
        score += 25;
      }
    }

    // API type matches
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
   * Highlight matches in text
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
   * Escape regex special characters
   */
  private escapeRegex(string: string): string {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  /**
   * Get search statistics
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
   * Get search result count information
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
   * Get result count text
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
   * Check if search has results
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
   * Get no results information
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
   * Get a simple "no results found" message
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
   * Check if search has no results
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
   * Highlights matches in a source system object
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

    // Highlight in name
    if (highlightedSystem.name) {
      highlightedSystem.name = this.highlightMatches(highlightedSystem.name, searchTerm, options);
    }

    // Highlight in description
    if (highlightedSystem.description) {
      highlightedSystem.description = this.highlightMatches(highlightedSystem.description, searchTerm, options);
    }

    // Highlight in API URL
    if (highlightedSystem.apiUrl) {
      highlightedSystem.apiUrl = this.highlightMatches(highlightedSystem.apiUrl, searchTerm, options);
    }

    return highlightedSystem;
  }

  /**
   * Highlights matches in an array of source systems
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
   * Get default search options
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
   * Check if case-insensitive is default
   */
  public isCaseInsensitiveByDefault(): boolean {
    return true;
  }
} 