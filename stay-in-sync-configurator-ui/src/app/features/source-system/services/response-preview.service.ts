import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { SourceSystemEndpointDTO } from '../models/sourceSystemEndpointDTO';
import { SourceSystemDTO } from '../models/sourceSystemDTO';
import { HttpErrorService } from '../../../core/services/http-error.service';

export interface ResponsePreviewData {
  endpoint: SourceSystemEndpointDTO;
  sourceSystem: SourceSystemDTO;
  response?: any;
  error?: string;
  isLoading: boolean;
  requestBody?: any;
  queryParams?: { [key: string]: any };
  pathParams?: { [key: string]: any };
}

export interface ApiTestRequest {
  endpoint: SourceSystemEndpointDTO;
  sourceSystem: SourceSystemDTO;
  requestBody?: any;
  queryParams?: { [key: string]: any };
  pathParams?: { [key: string]: any };
}

@Injectable({
  providedIn: 'root'
})
export class ResponsePreviewService {

  private previewDataSubject = new BehaviorSubject<ResponsePreviewData | null>(null);

  constructor(
    private http: HttpClient,
    private errorService: HttpErrorService
  ) {}

  /**
   * Get observable for response preview data
   */
  getPreviewData(): Observable<ResponsePreviewData | null> {
    return this.previewDataSubject.asObservable();
  }

  /**
   * Test API endpoint and update preview data
   */
  testEndpoint(request: ApiTestRequest): void {
    const previewData: ResponsePreviewData = {
      endpoint: request.endpoint,
      sourceSystem: request.sourceSystem,
      isLoading: true,
      requestBody: request.requestBody,
      queryParams: request.queryParams,
      pathParams: request.pathParams
    };

    this.previewDataSubject.next(previewData);

    this.performApiTest(request).subscribe({
      next: (response) => {
        this.previewDataSubject.next({
          ...previewData,
          isLoading: false,
          response: response,
          error: undefined
        });
      },
      error: (error) => {
        this.previewDataSubject.next({
          ...previewData,
          isLoading: false,
          response: undefined,
          error: this.formatError(error)
        });
      }
    });
  }

  /**
   * Clear preview data
   */
  clearPreview(): void {
    this.previewDataSubject.next(null);
  }

  /**
   * Get current preview data synchronously
   */
  getCurrentPreviewData(): ResponsePreviewData | null {
    return this.previewDataSubject.value;
  }

  /**
   * Generate example request body from endpoint schema
   */
  generateExampleRequestBody(endpoint: SourceSystemEndpointDTO): any {
    if (!endpoint.requestBodySchema) {
      return null;
    }

    try {
      const schema = JSON.parse(endpoint.requestBodySchema);
      return this.generateExampleFromSchema(schema);
    } catch (error) {
      return {};
    }
  }

  /**
   * Generate example query parameters
   */
  generateExampleQueryParams(endpoint: SourceSystemEndpointDTO): { [key: string]: any } {
    return {};
  }

  /**
   * Generate example path parameters
   */
  generateExamplePathParams(endpoint: SourceSystemEndpointDTO): { [key: string]: any } {
    return {};
  }

  /**
   * Build full API URL with path parameters
   */
  buildApiUrl(sourceSystem: SourceSystemDTO, endpoint: SourceSystemEndpointDTO, pathParams?: { [key: string]: any }): string {
    let url = sourceSystem.apiUrl;
    if (!url.endsWith('/')) url += '/';
    
    let path = endpoint.endpointPath;
    if (path.startsWith('/')) path = path.substring(1);

    if (pathParams) {
      Object.entries(pathParams).forEach(([key, value]) => {
        path = path.replace(`{${key}}`, encodeURIComponent(String(value)));
      });
    }

    return url + path;
  }

  /**
   * Validate request data before testing
   */
  validateRequest(request: ApiTestRequest): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!request.endpoint) {
      errors.push('Endpoint is required');
    }

    if (!request.sourceSystem) {
      errors.push('Source system is required');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  /**
   * Format response for display
   */
  formatResponse(response: any): string {
    if (response === null || response === undefined) {
      return 'null';
    }

    if (typeof response === 'string') {
      try {
        const parsed = JSON.parse(response);
        return JSON.stringify(parsed, null, 2);
      } catch {
        return response;
      }
    }

    return JSON.stringify(response, null, 2);
  }

  /**
   * Extract response headers for display
   */
  extractResponseHeaders(response: any): { [key: string]: string } {
    if (!response || !response.headers) {
      return {};
    }

    const headers: { [key: string]: string } = {};

    if (typeof response.headers.keys === 'function') {
      response.headers.keys().forEach((key: string) => {
        headers[key] = response.headers.get(key);
      });
    } else if (typeof response.headers === 'object') {
      Object.assign(headers, response.headers);
    }

    return headers;
  }

  /**
   * Perform actual API test
   */
  private performApiTest(request: ApiTestRequest): Observable<any> {
    const url = this.buildApiUrl(request.sourceSystem, request.endpoint, request.pathParams);
    const method = request.endpoint.httpRequestType.toUpperCase();

    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    let params = new HttpParams();
    if (request.queryParams) {
      Object.entries(request.queryParams).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
          params = params.set(key, String(value));
        }
      });
    }

    const options = {
      headers,
      params,
      observe: 'response' as const,
      responseType: 'text' as const
    };

    switch (method) {
      case 'GET':
        return this.http.get(url, options);
      case 'POST':
        return this.http.post(url, request.requestBody || null, options);
      case 'PUT':
        return this.http.put(url, request.requestBody || null, options);
      case 'DELETE':
        return this.http.delete(url, options);
      case 'PATCH':
        return this.http.patch(url, request.requestBody || null, options);
      case 'HEAD':
        return this.http.head(url, options);
      case 'OPTIONS':
        return this.http.options(url, options);
      default:
        throw new Error(`Unsupported HTTP method: ${method}`);
    }
  }

  /**
   * Generate example value for parameter
   */
  private generateExampleValueForParam(param: any): any {
    switch (param.type) {
      case 'STRING':
        return 'example';
      case 'NUMBER':
        return 123;
      case 'BOOLEAN':
        return true;
      case 'ARRAY':
        return ['item1', 'item2'];
      default:
        return 'example';
    }
  }

  /**
   * Generate example from JSON schema
   */
  private generateExampleFromSchema(schema: any): any {
    if (!schema || typeof schema !== 'object') return {};

    if (schema.type === 'object' && schema.properties) {
      const example: any = {};
      Object.entries(schema.properties).forEach(([key, prop]) => {
        example[key] = this.generateExampleFromSchema(prop);
      });
      return example;
    }

    if (schema.type === 'array' && schema.items) {
      return [this.generateExampleFromSchema(schema.items)];
    }

    switch (schema.type) {
      case 'string':
        return schema.example || 'string';
      case 'number':
      case 'integer':
        return schema.example || 0;
      case 'boolean':
        return schema.example || false;
      default:
        return schema.example || null;
    }
  }

  /**
   * Format error for display
   */
  private formatError(error: any): string {
    if (error.error) {
      return error.error;
    }

    if (error.message) {
      return error.message;
    }

    if (error.status) {
      return `HTTP ${error.status}: ${error.statusText || 'Unknown error'}`;
    }

    return 'Unknown error occurred';
  }
}
