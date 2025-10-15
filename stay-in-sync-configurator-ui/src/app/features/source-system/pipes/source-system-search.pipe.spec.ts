/** Unit tests for `SourceSystemSearchPipe`. */
import { TestBed } from '@angular/core/testing';
import { SourceSystemSearchPipe } from './source-system-search.pipe';
import { SourceSystemDTO } from '../models/sourceSystemDTO';


/** Verifies search, highlight, and helper behaviors. */
describe('SourceSystemSearchPipe', () => {
  let pipe: SourceSystemSearchPipe;

  const systems: SourceSystemDTO[] = [
    {
      id: 1,
      name: 'Test API System',
      description: 'A test system for API testing',
      apiType: 'REST',
      apiUrl: 'https://api.test.com',
      openApiSpec: ''
    },
    {
      id: 2,
      name: 'GraphQL Service',
      description: 'GraphQL API for data queries',
      apiType: 'GraphQL',
      apiUrl: 'https://graphql.service.com',
      openApiSpec: ''
    }
  ];

  /** Configure testing module and inject pipe. */
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [SourceSystemSearchPipe] });
    pipe = TestBed.inject(SourceSystemSearchPipe);
  });

  /** Should instantiate the pipe. */
  it('should create', () => {
    expect(pipe).toBeTruthy();
  });

  /** Transform behavior tests. */
  describe('transform', () => {
    /** Returns all systems when search term is empty/undefined. */
    it('returns all when search term empty/undefined', () => {
      expect(pipe.transform(systems, '', {})).toEqual(systems);
      expect(pipe.transform(systems, undefined as any, {})).toEqual(systems);
    });

    /** Returns empty array when systems input is null/undefined. */
    it('returns [] when systems is null/undefined', () => {
      expect(pipe.transform(undefined as any, 'test', {})).toEqual([]);
    });

    /** Filters by name, description, url, and type (case-insensitive). */
    it('filters by name/description/url/type', () => {
      expect(pipe.transform(systems, 'api', {})).toEqual([systems[0], systems[1]]);
      expect(pipe.transform(systems, 'graphql', {})).toEqual([systems[1]]);
      expect(pipe.transform(systems, 'service.com', {})).toEqual([systems[1]]);
      expect(pipe.transform(systems, 'REST', {})).toEqual([systems[0]]);
    });
  });

  /** Highlighting behavior tests. */
  describe('highlightMatches', () => {
    /** Wraps matches with a mark tag. */
    it('wraps matches with mark tag', () => {
      const res = pipe.highlightMatches('Test API System', 'api', {});
      expect(res).toContain('<mark class="search-highlight">API</mark>');
    });

    /** Returns original text when term is empty. */
    it('returns original when term empty', () => {
      const res = pipe.highlightMatches('Test API System', '' as any, {});
      expect(res).toBe('Test API System');
    });
  });

  /** No-results helper methods. */
  describe('no results helpers', () => {
    /** getNoResultsInfo returns expected flags and counts. */
    it('getNoResultsInfo provides message', () => {
      const info = pipe.getNoResultsInfo(systems, 'nonexistent', {} as any);
      expect(info.hasResults).toBe(false);
      expect(info.totalItems).toBe(2);
    });

    /** getNoResultsMessage summarizes no-results state. */
    it('getNoResultsMessage summarizes', () => {
      const msg = pipe.getNoResultsMessage(systems, 'nonexistent', {} as any);
      expect(msg).toContain('No results');
    });

    /** hasNoResults reflects the filtering outcome. */
    it('hasNoResults reflects result', () => {
      expect(pipe.hasNoResults(systems, 'nonexistent', {} as any)).toBe(true);
      expect(pipe.hasNoResults(systems, 'api', {} as any)).toBe(false);
    });
  });

  /** Counts and statistics helpers. */
  describe('counts and stats', () => {
    /** getSearchStats returns shape with totals. */
    it('getSearchStats returns shape', () => {
      const stats = pipe.getSearchStats(10, 3, 'a');
      expect(stats.total).toBe(10);
      expect(stats.filtered).toBe(3);
    });

    /** getSearchResultCount returns default count breakdown. */
    it('getSearchResultCount returns default breakdown', () => {
      const count = pipe.getSearchResultCount(systems, 'api', {});
      expect(count.total).toBe(2);
      expect(typeof count.percentage).toBe('number');
    });

    /** getResultCountText returns a summary string. */
    it('getResultCountText returns string', () => {
      const txt = pipe.getResultCountText(systems, '', {});
      expect(typeof txt).toBe('string');
    });
  });
}); 