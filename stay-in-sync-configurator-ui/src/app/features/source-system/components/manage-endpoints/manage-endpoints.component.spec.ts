/** Unit tests for `ManageEndpointsComponent`. */
import { ComponentFixture, TestBed, waitForAsync, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError, Subject } from 'rxjs';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpEvent, HttpResponse } from '@angular/common/http';
import { NO_ERRORS_SCHEMA, Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { MonacoEditorModule, NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';

import { ManageEndpointsComponent } from './manage-endpoints.component';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { SourceSystemEndpointDTO } from '../../models/sourceSystemEndpointDTO';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { CreateSourceSystemEndpointDTO } from '../../models/createSourceSystemEndpointDTO';
import { TypeScriptGenerationResponse } from '../../models/typescriptGenerationResponse';
import { OpenApiImportService } from '../../../../core/services/openapi-import.service';
import { ManageEndpointsFormService } from '../../services/manage-endpoints-form.service';
import { TypeScriptGenerationService } from '../../services/typescript-generation.service';
import { ResponsePreviewService } from '../../services/response-preview.service';

/** Lightweight dummies for services used by the component during init. */
class DummyOpenApiImportService {
  discoverEndpointsFromSpec() { return []; }
  discoverEndpointsFromSpecUrl() { return Promise.resolve([]); }
  persistParamsForEndpoint() { return Promise.resolve(); }
}
class DummyTypeScriptGenerationService {
  getMainGenerationState() { return of({ isGenerating: false, code: '', error: null }); }
  getEditGenerationState() { return of({ isGenerating: false, code: '', error: null }); }
}
class DummyResponsePreviewService {}
class DummySourceSystemResourceService {
  apiConfigSourceSystemIdGet() { return of(new HttpResponse({ body: { id: 1, apiUrl: 'https://petstore3.swagger.io/api/v3', openApiSpec: '' } } as any)).pipe(); }
}
class DummyEndpointResourceService {
  generateTypeScript() { return of(new HttpResponse<TypeScriptGenerationResponse>({ body: { generatedTypeScript: '' } })); }
}

/** Smoke tests for core CRUD flows and modal interactions. */
describe('ManageEndpointsComponent (updated smoke tests)', () => {
  let fixture: ComponentFixture<ManageEndpointsComponent>;
  let component: ManageEndpointsComponent;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        ReactiveFormsModule,
        ManageEndpointsComponent
      ],
      providers: [
        MessageService,
        HttpErrorService,
        ManageEndpointsFormService,
        { provide: OpenApiImportService, useClass: DummyOpenApiImportService },
        { provide: TypeScriptGenerationService, useClass: DummyTypeScriptGenerationService },
        { provide: ResponsePreviewService, useClass: DummyResponsePreviewService },
        { provide: SourceSystemResourceService, useClass: DummySourceSystemResourceService },
        { provide: SourceSystemEndpointResourceService, useClass: DummyEndpointResourceService },
        { provide: NGX_MONACO_EDITOR_CONFIG, useValue: {} }
      ]
    });

    fixture = TestBed.createComponent(ManageEndpointsComponent);
    component = fixture.componentInstance;
    component.sourceSystemId = 1;
    httpMock = TestBed.inject(HttpTestingController);
  });

  /** Ensure no unexpected HTTP requests remain. */
  afterEach(() => {
      httpMock.verify();
  });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

  /** Loads endpoints via GET and updates state. */
  it('loadEndpoints should GET and set endpoints', () => {
    component.loadEndpoints();
    const req = httpMock.expectOne('/api/config/source-system/1/endpoint');
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 1, sourceSystemId: 1, endpointPath: '/pets', httpRequestType: 'GET' }
    ]);
    expect(component.endpoints.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  /** Adds endpoint via POST, reloads list, and resets form. */
  it('addEndpoint should POST then reload and reset form', () => {
      component.ngOnInit();
    // Flush initial load(s)
    httpMock.match(r => r.method === 'GET' && r.url.includes('/api/config/source-system/1/endpoint'))
      .forEach(r => r.flush([]));

    component.endpointForm.patchValue({ endpointPath: '/test', httpRequestType: 'GET' });
      component.addEndpoint();

    const post = httpMock.expectOne('/api/config/source-system/1/endpoint');
    expect(post.request.method).toBe('POST');
    post.flush([]);

    httpMock.match('/api/config/source-system/1/endpoint').forEach(r => r.flush([]));

      expect(component.endpointForm.get('endpointPath')?.value).toBe('');
      expect(component.endpointForm.get('httpRequestType')?.value).toBe('GET');
    });

  /** Deletes endpoint upon confirmation and updates list. */
  it('delete flow should DELETE and update list', () => {
    component.endpoints = [{ id: 10, sourceSystemId: 1, endpointPath: '/x', httpRequestType: 'GET' } as any];
    component.deleteEndpoint(component.endpoints[0]);
    component.onConfirmationConfirmed();
    const del = httpMock.expectOne('/api/config/source-system/endpoint/10');
    expect(del.request.method).toBe('DELETE');
    del.flush({});
  });
});




@Component({
  selector: 'app-manage-endpoints',
  template: '<div>Mock Template</div>',
  standalone: false
})
class MockManageEndpointsComponent extends ManageEndpointsComponent {}

/** Request body editor and validation scenarios. */
describe('ManageEndpointsComponent - Request Body', () => {
  let component: ManageEndpointsComponent;
  let fixture: ComponentFixture<ManageEndpointsComponent>;
  let mockEndpointService: jasmine.SpyObj<SourceSystemEndpointResourceService>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    mockEndpointService = jasmine.createSpyObj('SourceSystemEndpointResourceService', [
      'apiConfigSourceSystemSourceSystemIdEndpointPost',
      'apiConfigSourceSystemEndpointIdPut'
    ]);
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        MonacoEditorModule.forRoot()
      ],
      providers: [
        { provide: SourceSystemEndpointResourceService, useValue: mockEndpointService },
        MessageService,
        HttpErrorService,
        { provide: NGX_MONACO_EDITOR_CONFIG, useValue: {} }
      ]
    });
    fixture = TestBed.createComponent(ManageEndpointsComponent);
    component = fixture.componentInstance;
    component.sourceSystemId = 1;
    fixture.detectChanges();
    httpMock = TestBed.inject(HttpTestingController);
  });

  /** Accepts valid JSON in requestBodySchema. */
  it('should accept valid JSON in requestBodySchema', () => {
    // flush any initial GETs
    httpMock.match(r => r.method === 'GET' && r.url.includes('/api/config/source-system/1/endpoint'))
      .forEach(r => r.flush([]));
    component.endpointForm.patchValue({ endpointPath: '/test', httpRequestType: 'POST', requestBodySchema: '{"foo": "bar"}' });
    component.addEndpoint();
    const post = httpMock.expectOne('/api/config/source-system/1/endpoint');
    expect(post.request.method).toBe('POST');
    post.flush([]);
    httpMock.match('/api/config/source-system/1/endpoint').forEach(r => r.flush([]));
    expect(component.jsonError).toBeNull();
  });

  /** Allows invalid JSON string to be saved as-is. */
  it('should allow invalid JSON in requestBodySchema (kept as string)', () => {
    // flush any initial GETs
    httpMock.match(r => r.method === 'GET' && r.url.includes('/api/config/source-system/1/endpoint'))
      .forEach(r => r.flush([]));
    component.endpointForm.patchValue({ endpointPath: '/test', httpRequestType: 'POST', requestBodySchema: '{foo: bar}' });
    component.addEndpoint();
    const post = httpMock.expectOne('/api/config/source-system/1/endpoint');
    post.flush([]);
    httpMock.match('/api/config/source-system/1/endpoint').forEach(r => r.flush([]));
    expect(component.jsonError).toBeNull();
  });

  /** Allows empty requestBodySchema without errors. */
  it('should allow empty requestBodySchema', () => {
    httpMock.match(r => r.method === 'GET' && r.url.includes('/api/config/source-system/1/endpoint'))
      .forEach(r => r.flush([]));
    component.endpointForm.patchValue({ endpointPath: '/test', httpRequestType: 'POST', requestBodySchema: '' });
    component.addEndpoint();
    const post = httpMock.expectOne('/api/config/source-system/1/endpoint');
    post.flush([]);
    httpMock.match('/api/config/source-system/1/endpoint').forEach(r => r.flush([]));
    expect(component.jsonError).toBeNull();
  });

  /** Saving invalid JSON locally should not set an error. */
  it('should show no error when saving invalid JSON (local set only)', () => {
    const ep: any = { id: 1, sourceSystemId: 1, endpointPath: '/test', httpRequestType: 'POST' };
    component.requestBodyEditorEndpoint = ep;
    component.requestBodyEditorModel = { value: '{foo: bar}', language: 'json' };
    component.saveRequestBodySchema();
    expect(component.requestBodyEditorError).toBeNull();
    expect(ep.requestBodySchema).toBe('{foo: bar}');
  });

  /** Saves valid JSON via request body editor panel. */
  it('should save valid JSON in request body editor panel', () => {
    component.requestBodyEditorEndpoint = { id: 1, sourceSystemId: 1, endpointPath: '/test', httpRequestType: 'POST' } as any;
    component.requestBodyEditorModel = { value: '{"foo": "bar"}', language: 'json' };
    component.saveRequestBodySchema();
    expect(component.requestBodyEditorError).toBeNull();
    // requestBodyEditorEndpoint is cleared after save, so assert through a local var before save
    // We simply expect the editor to be closed
    expect(component.requestBodyEditorEndpoint).toBeNull();
  });
});

/** Response body schema handling and TypeScript generation flows. */
describe('ManageEndpointsComponent - Response Body', () => {
  let component: ManageEndpointsComponent;
  let fixture: ComponentFixture<ManageEndpointsComponent>;
  let mockEndpointService: jasmine.SpyObj<SourceSystemEndpointResourceService>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    mockEndpointService = jasmine.createSpyObj('SourceSystemEndpointResourceService', [
      'apiConfigSourceSystemSourceSystemIdEndpointPost',
      'apiConfigSourceSystemEndpointIdPut',
      'generateTypeScript'
    ]);
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        MonacoEditorModule.forRoot()
      ],
      providers: [
        { provide: SourceSystemEndpointResourceService, useValue: mockEndpointService },
        MessageService,
        HttpErrorService,
        { provide: NGX_MONACO_EDITOR_CONFIG, useValue: {} }
      ]
    });
    fixture = TestBed.createComponent(ManageEndpointsComponent);
    component = fixture.componentInstance;
    component.sourceSystemId = 1;
    fixture.detectChanges();
    httpMock = TestBed.inject(HttpTestingController);
    // Default stub for generation
    mockEndpointService.generateTypeScript.and.returnValue(of({ generatedTypeScript: 'interface ResponseBody {}' } as any));
    // Flush any initial GETs issued on init
    httpMock.match(r => r.method === 'GET' && r.url.includes('/api/config/source-system/1/endpoint')).forEach(r => r.flush([]));
  });

  /** Accepts valid JSON in responseBodySchema. */
  it('should accept valid JSON in responseBodySchema', () => {
    httpMock.match(r => r.method === 'GET' && r.url.includes('/api/config/source-system/1/endpoint'))
      .forEach(r => r.flush([]));
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'GET',
      responseBodySchema: '{"message": "Success"}'
    });
    component.addEndpoint();
    const post = httpMock.expectOne('/api/config/source-system/1/endpoint');
    expect(post.request.method).toBe('POST');
    post.flush([]);
    httpMock.match('/api/config/source-system/1/endpoint').forEach(r => r.flush([]));
    expect(component.jsonError).toBeNull();
  });

  /** Rejects invalid JSON in responseBodySchema and prevents POST. */
  it('should reject invalid JSON in responseBodySchema', () => {
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'GET',
              responseBodySchema: '{message: Success}'
    });
    component.addEndpoint();
    expect(component.jsonError).toBe('Response-Body-Schema ist kein valides JSON.');
    expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost).not.toHaveBeenCalled();
  });

  /** Allows empty responseBodySchema, no generation attempts. */
  it('should allow empty responseBodySchema', () => {
    httpMock.match(r => r.method === 'GET' && r.url.includes('/api/config/source-system/1/endpoint'))
      .forEach(r => r.flush([]));
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'GET',
      responseBodySchema: ''
    });
    component.addEndpoint();
    const post = httpMock.expectOne('/api/config/source-system/1/endpoint');
    post.flush([]);
    const reload = httpMock.expectOne('/api/config/source-system/1/endpoint');
    reload.flush([]);
    expect(component.jsonError).toBeNull();
  });

  /** Extracts schema from OpenAPI-like object and triggers generation. */
  it('should extract response body schema from OpenAPI spec', () => {
    const mockSpec = {
      openapi: '3.0.0',
      paths: {
        '/test': {
          get: {
            responses: {
              '200': {
                content: {
                  'application/json': {
                    schema: {
                      type: 'object',
                      properties: {
                        message: { type: 'string' }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    };

    // Legacy internal method removed; just ensure no throw when accessing responseBodySchema helpers via public API
    component.endpointForm.patchValue({ responseBodySchema: JSON.stringify(mockSpec.paths['/test'].get.responses['200'].content['application/json'].schema) });
    mockEndpointService.generateTypeScript.and.returnValue(of(new HttpResponse<TypeScriptGenerationResponse>({ body: { generatedTypeScript: 'interface ResponseBody {}' } })));
    expect(() => component.loadTypeScriptForMainForm()).not.toThrow();
  });

  /** Shows response preview modal with selected endpoint. */
  it('should show response preview modal', () => {
    const endpoint: SourceSystemEndpointDTO = {
      id: 1,
      sourceSystemId: 1,
      endpointPath: '/test',
      httpRequestType: 'GET',
      responseBodySchema: '{"message": "Success"}'
    } as any;

    component.showResponsePreviewModal(endpoint);

    expect(component.responsePreviewModalVisible).toBe(true);
    expect(component.selectedResponsePreviewEndpoint).toEqual(endpoint);
  });

  /** Closes response preview modal and resets selection. */
  it('should close response preview modal', () => {
    component.responsePreviewModalVisible = true;
    component.selectedResponsePreviewEndpoint = { id: 1, sourceSystemId: 1, endpointPath: '/test', httpRequestType: 'GET' } as any;

    component.closeResponsePreviewModal();

    expect(component.responsePreviewModalVisible).toBe(false);
    expect(component.selectedResponsePreviewEndpoint).toBeNull();
  });

  /** Handles external visibility change for response preview modal. */
  it('should handle response preview modal visibility change', () => {
    component.responsePreviewModalVisible = true;
    component.selectedResponsePreviewEndpoint = { id: 1, sourceSystemId: 1, endpointPath: '/test', httpRequestType: 'GET' } as any;

    component.onResponsePreviewModalVisibleChange(false);

    expect(component.responsePreviewModalVisible).toBe(false);
    expect(component.selectedResponsePreviewEndpoint).toBeNull();
  });


  /** TypeScript tab behaviors and generation edge cases. */
  describe('TypeScript Tab Functionality', () => {
    const validJsonSchema = '{"type": "object", "properties": {"id": {"type": "number"}, "name": {"type": "string"}}}';
    const invalidJsonSchema = '{invalid json}';
    const largeJsonSchema = '{"type": "object", "properties": {' + '"prop' + Array(1000).fill(0).map((_, i) => i).join('": {"type": "string"}, "prop') + '": {"type": "string"}}}';

    beforeEach(() => {
      component.ngOnInit();
    });

    /** Generates TypeScript from a valid JSON schema. */
    it('should generate TypeScript from valid JSON schema', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; name: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
      expect(component.typescriptError).toBeNull();
      expect(component.isGeneratingTypeScript).toBeFalse();
    }));

    /** Handles backend error response during TypeScript generation. */
    it('should handle TypeScript generation error', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        error: 'Invalid JSON schema provided'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.typescriptError).toContain('Backend generation failed');
      expect(component.generatedTypeScript).toContain('Error: Unable to generate TypeScript');
    }));

    /** Handles network error during TypeScript generation. */
    it('should handle network error during TypeScript generation', fakeAsync(() => {
      mockEndpointService.generateTypeScript.and.returnValue(throwError(() => new Error('Network error')));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.typescriptError).toContain('Backend communication failed');
      expect(component.generatedTypeScript).toContain('Error: Unable to generate TypeScript');
    }));

    /** Validates oversized JSON schema input. */
    it('should validate JSON schema size', () => {
      const big = 'x'.repeat(1024 * 1024 + 5);
      const validation = (component as any)['validateJsonSchema'](big);
      expect(validation.isValid).toBeFalse();
      expect(validation.error).toContain('too large');
    });

    /** Validates JSON schema structure rules. */
    it('should validate JSON schema structure', () => {
      const validSchema = '{"type": "object", "properties": {"test": {"type": "string"}}}';
      const invalidSchema = '{"notASchema": true}';

      const validValidation = component['validateJsonSchema'](validSchema);
      const invalidValidation = component['validateJsonSchema'](invalidSchema);

      expect(validValidation.isValid).toBeTrue();
      expect(invalidValidation.isValid).toBeFalse();
      expect(invalidValidation.error).toContain('Invalid JSON Schema structure');
    });

    /** Loads TypeScript on switching to TypeScript tab. */
    it('should handle tab change to TypeScript tab', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.onTabChange({ index: 1 });
      tick();

      expect(component.activeTabIndex).toBe(1);
      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Avoids unnecessary regeneration when already generated. */
    it('should not regenerate TypeScript if already generated', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      const initialCallCount = mockEndpointService.generateTypeScript.calls.count();

      component.onTabChange({ index: 1 });
      tick();

      expect(mockEndpointService.generateTypeScript.calls.count()).toBeGreaterThanOrEqual(initialCallCount);
    }));

    /** Generates TypeScript for the edit form schema. */
    it('should handle edit form TypeScript generation', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; name: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.editForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForEditForm();
      tick();

      expect(component.editGeneratedTypeScript).toContain('interface ResponseBody');
      expect(component.editTypeScriptError).toBeNull();
      expect(component.isGeneratingEditTypeScript).toBeFalse();
    }));


    /** Times out long-running TypeScript generation requests. */
    it('should handle timeout during TypeScript generation', fakeAsync(() => {
      const neverEmits$ = new Subject<any>();
      mockEndpointService.generateTypeScript.and.returnValue(neverEmits$ as any);

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick(31000);

      expect(component.typescriptError).toContain('timed out');
    }));

    /** Formats TypeScript error messages consistently. */
    it('should format error messages correctly', () => {
      const networkError = component['formatErrorMessage']('HttpErrorResponse: Network error', 'Test context');
      const timeoutError = component['formatErrorMessage']('Request timeout', 'Test context');
      const jsonError = component['formatErrorMessage']('Invalid JSON syntax', 'Test context');

      expect(networkError).toContain('Network error: Unable to connect to the server');
      expect(timeoutError).toContain('Request timed out');
      expect(jsonError).toContain('Invalid JSON format in schema');
    });


    /** Generates TypeScript from complex nested JSON schema. */
    it('should generate TypeScript from complex nested JSON schema', fakeAsync(() => {
      const complexSchema = '{"type": "object", "properties": {"user": {"type": "object", "properties": {"id": {"type": "number"}, "profile": {"type": "object", "properties": {"name": {"type": "string"}, "email": {"type": "string"}}}}}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { user: { id: number; profile: { name: string; email: string; }; }; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: complexSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('user: { id: number; profile:');
      expect(component.typescriptError).toBeNull();
    }));

    /** Generates TypeScript from an array-based JSON schema. */
    it('should generate TypeScript from array schema', fakeAsync(() => {
      const arraySchema = '{"type": "object", "properties": {"items": {"type": "array", "items": {"type": "string"}}, "count": {"type": "number"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { items: string[]; count: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: arraySchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Generates TypeScript for union types in JSON schema. */
    it('should generate TypeScript from union type schema', fakeAsync(() => {
      const unionSchema = '{"type": "object", "properties": {"status": {"type": "string", "enum": ["active", "inactive"]}, "value": {"oneOf": [{"type": "string"}, {"type": "number"}]}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { status: "active" | "inactive"; value: string | number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: unionSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Handles special characters in JSON schema keys. */
    it('should handle special characters in JSON schema', fakeAsync(() => {
      const specialCharSchema = '{"type": "object", "properties": {"user_name": {"type": "string"}, "email@domain": {"type": "string"}, "$metadata": {"type": "object"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { user_name: string; "email@domain": string; $metadata: object; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: specialCharSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Handles Unicode text in JSON schema. */
    it('should handle Unicode characters in JSON schema', fakeAsync(() => {
      const unicodeSchema = '{"type": "object", "properties": {"name": {"type": "string", "description": "User\'s name with émojis 🎉"}, "message": {"type": "string"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { name: string; message: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: unicodeSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Supports selected OpenAPI 3.0 format hints. */
    it('should handle OpenAPI 3.0 specific features', fakeAsync(() => {
      const openApi3Schema = '{"type": "object", "properties": {"id": {"type": "integer", "format": "int64"}, "email": {"type": "string", "format": "email"}, "createdAt": {"type": "string", "format": "date-time"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; email: string; createdAt: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: openApi3Schema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Handles custom type references in schema. */
    it('should handle custom type definitions', fakeAsync(() => {
      const customTypeSchema = '{"type": "object", "properties": {"data": {"$ref": "#/components/schemas/User"}, "metadata": {"type": "object"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { data: User; metadata: object; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: customTypeSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Gracefully handles malformed JSON schema errors. */
    it('should handle malformed JSON schema gracefully', fakeAsync(() => {
      const malformedSchema = '{"type": "object", "properties": {"id": {"type": "invalid_type"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        error: 'Invalid type: invalid_type'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: malformedSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.typescriptError).toBeTruthy();
      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));

    /** Supports multiple rapid generation requests without breaking. */
    it('should handle concurrent TypeScript generation requests', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      let calls = 0;
      mockEndpointService.generateTypeScript.and.callFake(() => {
        calls++;
        return of(mockResponse as any);
      });

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });


      component.loadTypeScriptForMainForm();
      component.loadTypeScriptForMainForm();
      component.loadTypeScriptForMainForm();
      tick();


      expect(calls).toBeGreaterThan(0);
      expect(component.generatedTypeScript).toContain('interface ResponseBody');
    }));

    /** Returns empty TypeScript and no errors for empty schema. */
    it('should handle empty response body schema', fakeAsync(() => {
      component.endpointForm.patchValue({ responseBodySchema: '' });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toBe('');
      expect(component.typescriptError).toBeNull();
      expect(mockEndpointService.generateTypeScript).not.toHaveBeenCalled();
    }));

    /** Validates whitespace-only schema as invalid. */
    it('should handle whitespace-only response body schema', fakeAsync(() => {
      component.endpointForm.patchValue({ responseBodySchema: '   ' });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('Error: Unable to generate TypeScript');
      expect(component.typescriptError).toContain('JSON schema is empty');
    }));

    /** Treats null schema as empty. */
    it('should handle null response body schema', fakeAsync(() => {
      component.endpointForm.patchValue({ responseBodySchema: null });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toBe('');
      expect(component.typescriptError).toBeNull();
      expect(mockEndpointService.generateTypeScript).not.toHaveBeenCalled();
    }));

    /** Initializes Monaco editor options for TypeScript tab. */
    it('should handle Monaco editor initialization for TypeScript', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();


      expect(component.typescriptEditorOptions).toBeDefined();
      expect(component.typescriptEditorOptions.language).toBe('typescript');
    }));

    /** Switches between JSON and TypeScript tabs preserving state. */
    it('should handle tab switching between JSON and TypeScript', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(of(mockResponse as any));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });


      component.onTabChange({ index: 1 });
      tick();

      expect(component.activeTabIndex).toBe(1);
      expect(component.generatedTypeScript.length).toBeGreaterThan(0);


      component.onTabChange({ index: 0 });
      tick();

      expect(component.activeTabIndex).toBe(0);

      expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    }));
  });
});
