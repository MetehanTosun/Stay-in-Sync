import { TestBed } from '@angular/core/testing';
import { SourceSystemSearchPipe } from './source-system-search.pipe';
import { SourceSystemDTO } from '../models/sourceSystemDTO';


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

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [SourceSystemSearchPipe] });
    pipe = TestBed.inject(SourceSystemSearchPipe);
  });

  it('should create', () => {
    expect(pipe).toBeTruthy();
  });

  describe('transform', () => {
    it('returns all when search term empty/undefined', () => {
      expect(pipe.transform(systems, '', {})).toEqual(systems);
      expect(pipe.transform(systems, undefined as any, {})).toEqual(systems);
    });

    it('returns [] when systems is null/undefined', () => {
      expect(pipe.transform(undefined as any, 'test', {})).toEqual([]);
    });

    it('filters by name/description/url/type', () => {
      // 'api' appears in name of #1 and description of #2 (case-insensitive)
      expect(pipe.transform(systems, 'api', {})).toEqual([systems[0], systems[1]]);
      expect(pipe.transform(systems, 'graphql', {})).toEqual([systems[1]]);
      expect(pipe.transform(systems, 'service.com', {})).toEqual([systems[1]]);
      expect(pipe.transform(systems, 'REST', {})).toEqual([systems[0]]);
    });
  });

  describe('highlightMatches', () => {
    it('wraps matches with mark tag', () => {
      const res = pipe.highlightMatches('Test API System', 'api', {});
      expect(res).toContain('<mark class="search-highlight">API</mark>');
    });

    it('returns original when term empty', () => {
      const res = pipe.highlightMatches('Test API System', '' as any, {});
      expect(res).toBe('Test API System');
    });
  });

  describe('no results helpers', () => {
    it('getNoResultsInfo provides message', () => {
      const info = pipe.getNoResultsInfo(systems, 'nonexistent', {} as any);
      expect(info.hasResults).toBe(false);
      expect(info.totalItems).toBe(2);
    });

    it('getNoResultsMessage summarizes', () => {
      const msg = pipe.getNoResultsMessage(systems, 'nonexistent', {} as any);
      expect(msg).toContain('No results');
    });

    it('hasNoResults reflects result', () => {
      expect(pipe.hasNoResults(systems, 'nonexistent', {} as any)).toBe(true);
      expect(pipe.hasNoResults(systems, 'api', {} as any)).toBe(false);
    });
  });

  describe('counts and stats', () => {
    it('getSearchStats returns shape', () => {
      const stats = pipe.getSearchStats(10, 3, 'a');
      expect(stats.total).toBe(10);
      expect(stats.filtered).toBe(3);
    });

    it('getSearchResultCount returns default breakdown', () => {
      const count = pipe.getSearchResultCount(systems, 'api', {});
      expect(count.total).toBe(2);
      expect(typeof count.percentage).toBe('number');
    });

    it('getResultCountText returns string', () => {
      const txt = pipe.getResultCountText(systems, '', {});
      expect(typeof txt).toBe('string');
    });
  });
}); 