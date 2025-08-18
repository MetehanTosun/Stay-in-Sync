import { TestBed } from '@angular/core/testing';
import { SourceSystemSearchPipe, SearchOptions, NoResultsInfo } from './source-system-search.pipe';
import { SourceSystemDTO } from '../features/source-system/models/sourceSystemDTO';
import { SourceSystemEndpointDTO } from '../features/source-system/models/sourceSystemEndpointDTO';

describe('SourceSystemSearchPipe', () => {
  let pipe: SourceSystemSearchPipe;

  // Test data
  const mockSourceSystems: SourceSystemDTO[] = [
    {
      id: 1,
      name: 'Test API System',
      description: 'A test system for API testing',
      apiType: 'REST',
      apiUrl: 'https://api.test.com',
      openApiSpec: null,
      metadata: null,
      endpoints: [
        {
          id: 1,
          endpointPath: '/api/users',
          httpRequestType: 'GET',
          description: 'Get all users',
          queryParams: [
            {
              id: 1,
              paramName: 'limit',
              queryParamType: 'QUERY',
              schemaType: 'INTEGER',
              values: ['10', '20', '50']
            }
          ],
          apiRequestConfigurations: [],
          authConfig: null
        },
        {
          id: 2,
          endpointPath: '/api/auth/login',
          httpRequestType: 'POST',
          description: 'User authentication endpoint',
          queryParams: [],
          apiRequestConfigurations: [],
          authConfig: {
            authType: 'Bearer',
            username: 'testuser',
            password: 'testpass',
            token: 'test-token',
            apiKey: null
          }
        }
      ],
      headers: [
        {
          id: 1,
          headerName: 'Authorization',
          headerType: 'Authorization',
          values: ['Bearer token123', 'Basic credentials']
        },
        {
          id: 2,
          headerName: 'Content-Type',
          headerType: 'Content-Type',
          values: ['application/json', 'application/xml']
        }
      ]
    },
    {
      id: 2,
      name: 'GraphQL Service',
      description: 'GraphQL API for data queries',
      apiType: 'GraphQL',
      apiUrl: 'https://graphql.service.com',
      openApiSpec: null,
      metadata: null,
      endpoints: [
        {
          id: 3,
          endpointPath: '/graphql',
          httpRequestType: 'POST',
          description: 'GraphQL query endpoint',
          queryParams: [],
          apiRequestConfigurations: [],
          authConfig: null
        }
      ],
      headers: [
        {
          id: 3,
          headerName: 'X-API-Key',
          headerType: 'Custom',
          values: ['api-key-123']
        }
      ]
    }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SourceSystemSearchPipe]
    });
    pipe = TestBed.inject(SourceSystemSearchPipe);
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  describe('transform method', () => {
    it('should return all systems when no search term is provided', () => {
      const result = pipe.transform(mockSourceSystems, '', {});
      expect(result).toEqual(mockSourceSystems);
    });

    it('should return all systems when search term is null', () => {
      const result = pipe.transform(mockSourceSystems, null, {});
      expect(result).toEqual(mockSourceSystems);
    });

    it('should return all systems when search term is undefined', () => {
      const result = pipe.transform(mockSourceSystems, undefined, {});
      expect(result).toEqual(mockSourceSystems);
    });

    it('should return empty array when source systems is null', () => {
      const result = pipe.transform(null, 'test', {});
      expect(result).toEqual([]);
    });

    it('should return empty array when source systems is undefined', () => {
      const result = pipe.transform(undefined, 'test', {});
      expect(result).toEqual([]);
    });

    it('should filter systems by name (case-insensitive)', () => {
      const result = pipe.transform(mockSourceSystems, 'test api', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should filter systems by description', () => {
      const result = pipe.transform(mockSourceSystems, 'graphql', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('GraphQL Service');
    });

    it('should filter systems by API URL', () => {
      const result = pipe.transform(mockSourceSystems, 'api.test.com', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should filter systems by API type', () => {
      const result = pipe.transform(mockSourceSystems, 'REST', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should filter systems by endpoint path', () => {
      const result = pipe.transform(mockSourceSystems, '/api/users', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should filter systems by HTTP method', () => {
      const result = pipe.transform(mockSourceSystems, 'POST', {});
      expect(result.length).toBe(2); // Both systems have POST endpoints
    });

    it('should filter systems by header name', () => {
      const result = pipe.transform(mockSourceSystems, 'Authorization', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should filter systems by header value', () => {
      const result = pipe.transform(mockSourceSystems, 'Bearer token123', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should filter systems by query parameter name', () => {
      const result = pipe.transform(mockSourceSystems, 'limit', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should filter systems by query parameter value', () => {
      const result = pipe.transform(mockSourceSystems, '10', {});
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should support case-sensitive search', () => {
      const result = pipe.transform(mockSourceSystems, 'TEST API', { caseSensitive: true });
      expect(result.length).toBe(0); // Should not find 'Test API System' with case-sensitive search
    });

    it('should support regex search', () => {
      const result = pipe.transform(mockSourceSystems, 'test.*api', { enableRegex: true });
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should search only in names when scope is set to names', () => {
      const result = pipe.transform(mockSourceSystems, 'graphql', { searchScope: 'names' });
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('GraphQL Service');
    });

    it('should search only in descriptions when scope is set to descriptions', () => {
      const result = pipe.transform(mockSourceSystems, 'testing', { searchScope: 'descriptions' });
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should search only in URLs when scope is set to urls', () => {
      const result = pipe.transform(mockSourceSystems, 'service.com', { searchScope: 'urls' });
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('GraphQL Service');
    });

    it('should search only in endpoints when scope is set to endpoints', () => {
      const result = pipe.transform(mockSourceSystems, 'login', { searchScope: 'endpoints' });
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('Test API System');
    });

    it('should search only in headers when scope is set to headers', () => {
      const result = pipe.transform(mockSourceSystems, 'X-API-Key', { searchScope: 'headers' });
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('GraphQL Service');
    });
  });

  describe('highlightMatches method', () => {
    it('should highlight matches in text', () => {
      const result = pipe.highlightMatches('Test API System', 'api', {});
      expect(result).toContain('<mark class="search-highlight">api</mark>');
    });

    it('should highlight matches case-insensitively by default', () => {
      const result = pipe.highlightMatches('Test API System', 'API', {});
      expect(result).toContain('<mark class="search-highlight">API</mark>');
    });

    it('should highlight matches case-sensitively when specified', () => {
      const result = pipe.highlightMatches('Test API System', 'API', { caseSensitive: true });
      expect(result).toContain('<mark class="search-highlight">API</mark>');
    });

    it('should support regex highlighting', () => {
      const result = pipe.highlightMatches('Test API System', 'test.*api', { enableRegex: true });
      expect(result).toContain('<mark class="search-highlight">Test API</mark>');
    });

    it('should return original text when no search term is provided', () => {
      const result = pipe.highlightMatches('Test API System', '', {});
      expect(result).toBe('Test API System');
    });

    it('should return original text when search term is null', () => {
      const result = pipe.highlightMatches('Test API System', null, {});
      expect(result).toBe('Test API System');
    });
  });

  describe('getNoResultsInfo method', () => {
    it('should return hasResults true when results are found', () => {
      const result = pipe.getNoResultsInfo(mockSourceSystems, 'test', {});
      expect(result.hasResults).toBe(true);
      expect(result.message).toContain('Found');
    });

    it('should return hasResults false when no results are found', () => {
      const result = pipe.getNoResultsInfo(mockSourceSystems, 'nonexistent', {});
      expect(result.hasResults).toBe(false);
      expect(result.message).toContain('No results found');
    });

    it('should provide suggestions when no results are found', () => {
      const result = pipe.getNoResultsInfo(mockSourceSystems, 'nonexistent', {});
      expect(result.suggestions.length).toBeGreaterThan(0);
    });

    it('should provide alternative search terms when no results are found', () => {
      const result = pipe.getNoResultsInfo(mockSourceSystems, 'nonexistent', {});
      expect(result.alternativeSearchTerms.length).toBeGreaterThan(0);
    });

    it('should handle empty search term', () => {
      const result = pipe.getNoResultsInfo(mockSourceSystems, '', {});
      expect(result.message).toContain('No search term provided');
    });

    it('should handle null source systems', () => {
      const result = pipe.getNoResultsInfo(null, 'test', {});
      expect(result.hasResults).toBe(false);
      expect(result.totalItems).toBe(0);
    });

    it('should handle empty source systems array', () => {
      const result = pipe.getNoResultsInfo([], 'test', {});
      expect(result.hasResults).toBe(false);
      expect(result.totalItems).toBe(0);
    });
  });

  describe('getNoResultsMessage method', () => {
    it('should return appropriate message when results are found', () => {
      const result = pipe.getNoResultsMessage(mockSourceSystems, 'test', {});
      expect(result).toContain('Found');
    });

    it('should return appropriate message when no results are found', () => {
      const result = pipe.getNoResultsMessage(mockSourceSystems, 'nonexistent', {});
      expect(result).toContain('No results found');
    });
  });

  describe('hasNoResults method', () => {
    it('should return false when results are found', () => {
      const result = pipe.hasNoResults(mockSourceSystems, 'test', {});
      expect(result).toBe(false);
    });

    it('should return true when no results are found', () => {
      const result = pipe.hasNoResults(mockSourceSystems, 'nonexistent', {});
      expect(result).toBe(true);
    });
  });

  describe('searchEndpoints method', () => {
    it('should find systems with matching endpoints', () => {
      const result = pipe.searchEndpoints(mockSourceSystems, 'users', {});
      expect(result.length).toBe(1);
      expect(result[0].system.name).toBe('Test API System');
      expect(result[0].matchingEndpoints.length).toBe(1);
    });

    it('should return empty array when no matching endpoints', () => {
      const result = pipe.searchEndpoints(mockSourceSystems, 'nonexistent', {});
      expect(result).toEqual([]);
    });
  });

  describe('searchHeaders method', () => {
    it('should find systems with matching headers', () => {
      const result = pipe.searchHeaders(mockSourceSystems, 'Authorization', {});
      expect(result.length).toBe(1);
      expect(result[0].system.name).toBe('Test API System');
      expect(result[0].matchingHeaders.length).toBe(1);
    });

    it('should return empty array when no matching headers', () => {
      const result = pipe.searchHeaders(mockSourceSystems, 'nonexistent', {});
      expect(result).toEqual([]);
    });
  });

  describe('getDefaultSearchOptions method', () => {
    it('should return default search options', () => {
      const result = pipe.getDefaultSearchOptions();
      expect(result.caseSensitive).toBe(false);
      expect(result.enableRegex).toBe(false);
      expect(result.searchScope).toBe('all');
      expect(result.highlightMatches).toBe(false);
    });
  });

  describe('isCaseInsensitiveByDefault method', () => {
    it('should return true', () => {
      const result = pipe.isCaseInsensitiveByDefault();
      expect(result).toBe(true);
    });
  });

  describe('getCommonHeaderTypes method', () => {
    it('should return array of common header types', () => {
      const result = pipe.getCommonHeaderTypes();
      expect(Array.isArray(result)).toBe(true);
      expect(result.length).toBeGreaterThan(0);
      expect(result).toContain('Authorization');
      expect(result).toContain('Content-Type');
    });
  });

  describe('getCommonHeaderValuePatterns method', () => {
    it('should return object with header value patterns', () => {
      const result = pipe.getCommonHeaderValuePatterns();
      expect(typeof result).toBe('object');
      expect(result['Authorization']).toBeDefined();
      expect(result['Content-Type']).toBeDefined();
    });
  });

  describe('getHighlightingClasses method', () => {
    it('should return object with highlighting classes', () => {
      const result = pipe.getHighlightingClasses();
      expect(typeof result).toBe('object');
      expect(result['search-highlight']).toBeDefined();
      expect(result['exact-match']).toBeDefined();
    });
  });

  describe('advancedHighlight method', () => {
    it('should highlight with specific match type class', () => {
      const result = pipe.advancedHighlight('Test API System', 'api', 'name', {});
      expect(result).toContain('search-highlight-name');
    });

    it('should highlight with default class when match type is not specified', () => {
      const result = pipe.advancedHighlight('Test API System', 'api', 'partial', {});
      expect(result).toContain('search-highlight-partial');
    });
  });

  describe('highlightWithClass method', () => {
    it('should highlight with custom CSS class', () => {
      const result = pipe.highlightWithClass('Test API System', 'api', 'custom-highlight', {});
      expect(result).toContain('custom-highlight');
    });

    it('should use default class when no custom class is provided', () => {
      const result = pipe.highlightWithClass('Test API System', 'api', undefined, {});
      expect(result).toContain('search-highlight');
    });
  });

  describe('highlightSourceSystemMatches method', () => {
    it('should highlight matches in source system object', () => {
      const system = mockSourceSystems[0];
      const result = pipe.highlightSourceSystemMatches(system, 'api', { highlightMatches: true });
      expect(result.name).toContain('<mark class="search-highlight">api</mark>');
    });

    it('should return original system when highlightMatches is false', () => {
      const system = mockSourceSystems[0];
      const result = pipe.highlightSourceSystemMatches(system, 'api', { highlightMatches: false });
      expect(result.name).toBe(system.name);
    });
  });

  describe('highlightSourceSystemsMatches method', () => {
    it('should highlight matches in multiple source systems', () => {
      const result = pipe.highlightSourceSystemsMatches(mockSourceSystems, 'api', { highlightMatches: true });
      expect(result[0].name).toContain('<mark class="search-highlight">api</mark>');
    });

    it('should return original systems when highlightMatches is false', () => {
      const result = pipe.highlightSourceSystemsMatches(mockSourceSystems, 'api', { highlightMatches: false });
      expect(result[0].name).toBe(mockSourceSystems[0].name);
    });
  });

  describe('getSearchStats method', () => {
    it('should return correct search statistics', () => {
      const result = pipe.getSearchStats(10, 3, 'test');
      expect(result.total).toBe(10);
      expect(result.filtered).toBe(3);
      expect(result.searchTerm).toBe('test');
    });
  });

  describe('Edge cases and error handling', () => {
    it('should handle invalid regex gracefully', () => {
      const result = pipe.transform(mockSourceSystems, '[invalid', { enableRegex: true });
      expect(result).toEqual([]);
    });

    it('should handle empty strings in searchable fields', () => {
      const systemWithEmptyFields: SourceSystemDTO = {
        id: 3,
        name: '',
        description: '',
        apiType: '',
        apiUrl: '',
        openApiSpec: null,
        metadata: null,
        endpoints: [],
        headers: []
      };
      const result = pipe.transform([systemWithEmptyFields], 'test', {});
      expect(result).toEqual([]);
    });

    it('should handle null values in searchable fields', () => {
      const systemWithNullFields: SourceSystemDTO = {
        id: 4,
        name: null,
        description: null,
        apiType: null,
        apiUrl: null,
        openApiSpec: null,
        metadata: null,
        endpoints: null,
        headers: null
      };
      const result = pipe.transform([systemWithNullFields], 'test', {});
      expect(result).toEqual([]);
    });

    it('should handle very long search terms', () => {
      const longSearchTerm = 'a'.repeat(1000);
      const result = pipe.transform(mockSourceSystems, longSearchTerm, {});
      expect(result).toEqual([]);
    });

    it('should handle special characters in search terms', () => {
      const result = pipe.transform(mockSourceSystems, 'test@#$%^&*()', {});
      expect(result).toEqual([]);
    });
  });
}); 