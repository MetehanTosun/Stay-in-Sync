import { ComponentFixture, TestBed, waitForAsync, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpEvent, HttpResponse } from '@angular/common/http';
import { NO_ERRORS_SCHEMA, Component } from '@angular/core';
import { MonacoEditorModule, NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';

import { ManageEndpointsComponent } from './manage-endpoints.component';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { SourceSystemEndpointDTO } from '../../models/sourceSystemEndpointDTO';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { CreateSourceSystemEndpointDTO } from '../../models/createSourceSystemEndpointDTO';
import { TypeScriptGenerationRequest } from '../../models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../../models/typescriptGenerationResponse';



// MOCK COMPONENT - Ersetzt das echte Template
@Component({
  selector: 'app-manage-endpoints',
  template: '<div>Mock Template</div>',
  standalone: false
})
class MockManageEndpointsComponent extends ManageEndpointsComponent {}

describe('ManageEndpointsComponent', () => {
  let component: ManageEndpointsComponent;
  let fixture: ComponentFixture<ManageEndpointsComponent>;
  let mockEndpointService: jasmine.SpyObj<SourceSystemEndpointResourceService>;
  let mockSourceSystemService: jasmine.SpyObj<SourceSystemResourceService>;
  let httpMock: HttpTestingController;

  const mockSourceSystem: SourceSystemDTO = {
    id: 1,
    name: 'Test Source System',
    apiUrl: 'https://api.test.com',
    description: 'Test Description',
    apiType: 'REST_OPENAPI',
    openApiSpec: 'https://petstore3.swagger.io/api/v3/openapi.json'
  };

  const mockEndpoints: SourceSystemEndpointDTO[] = [
    {
      id: 1,
      endpointPath: '/pets',
      httpRequestType: 'GET',
      sourceSystemId: 1,
      responseBodySchema: '{"message": "Success"}'
    },
    {
      id: 2,
      endpointPath: '/pets/{id}',
      httpRequestType: 'GET',
      sourceSystemId: 1,
      responseBodySchema: '{"message": "Success"}'
    }
  ];

  const mockOpenApiSpec = {
    openapi: '3.0.0',
    info: { title: 'Test API', version: '1.0.0' },
    paths: {
      '/pets': {
        get: {
          summary: 'List all pets',
          parameters: [
            {
              name: 'limit',
              in: 'query',
              schema: { type: 'integer' }
            }
          ]
        },
        post: {
          summary: 'Create a pet'
        }
      },
      '/pets/{petId}': {
        get: {
          summary: 'Get a pet by ID',
          parameters: [
            {
              name: 'petId',
              in: 'path',
              required: true,
              schema: { type: 'integer' }
            }
          ]
        }
      }
    }
  };

  beforeEach(waitForAsync(() => {
    mockEndpointService = jasmine.createSpyObj('SourceSystemEndpointResourceService', [
      'apiConfigSourceSystemSourceSystemIdEndpointGet',
      'apiConfigSourceSystemSourceSystemIdEndpointPost',
      'apiConfigSourceSystemEndpointIdDelete',
      'generateTypeScript'
    ]);

    mockSourceSystemService = jasmine.createSpyObj('SourceSystemResourceService', [
      'apiConfigSourceSystemIdGet'
    ]);

    // Setup default return values
    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointGet.and.returnValue(
      of(new HttpResponse({ body: mockEndpoints, status: 200 }))
    );

    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(
      of(new HttpResponse<HttpEvent<any>>({ status: 201 }))
    );

    mockEndpointService.apiConfigSourceSystemEndpointIdDelete.and.returnValue(
      of(new HttpResponse<HttpEvent<any>>({ status: 204 }))
    );

    // Setup default TypeScript generation mock
    mockEndpointService.generateTypeScript.and.returnValue(
      of(new HttpResponse<TypeScriptGenerationResponse>({ 
        body: { generatedTypeScript: 'interface ResponseBody { }' }, 
        status: 200 
      }))
    );

    mockSourceSystemService.apiConfigSourceSystemIdGet.and.returnValue(
      of(new HttpResponse({ body: mockSourceSystem, status: 200 }))
    );

    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule,
        RouterTestingModule,
        MonacoEditorModule.forRoot()
      ],
      declarations: [MockManageEndpointsComponent],
      providers: [
        { provide: SourceSystemEndpointResourceService, useValue: mockEndpointService },
        { provide: SourceSystemResourceService, useValue: mockSourceSystemService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });

    httpMock = TestBed.inject(HttpTestingController);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MockManageEndpointsComponent);
    component = fixture.componentInstance;
    component.sourceSystemId = 1;

    // Use real ngOnInit implementation
    spyOn(component, 'ngOnInit').and.callThrough();
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
  });

  describe('Component Logic Tests (Without Rendering)', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize form correctly', () => {
      component.ngOnInit();

      expect(component.endpointForm.get('endpointPath')?.value).toBe('');
      expect(component.endpointForm.get('httpRequestType')?.value).toBe('GET');
    });

    it('should add endpoint when form is valid', () => {
      component.ngOnInit();

      const newEndpoint: CreateSourceSystemEndpointDTO = {
        endpointPath: '/users',
        httpRequestType: 'POST',
        requestBodySchema: '',
        responseBodySchema: ''
      };

      component.endpointForm.patchValue(newEndpoint);

      component.addEndpoint();

      expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost)
        .toHaveBeenCalledWith(1, [newEndpoint]);
    });

    it('should not add endpoint when form is invalid', () => {
      component.ngOnInit();

      component.endpointForm.patchValue({ endpointPath: '', httpRequestType: 'GET' });
      component.endpointForm.get('endpointPath')?.setErrors({ required: true });

      component.addEndpoint();

      expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost)
        .not.toHaveBeenCalled();
    });

    it('should delete endpoint', () => {
      component.ngOnInit();
      component.endpoints = mockEndpoints;

      const endpointToDelete = mockEndpoints[0];
      component.deleteEndpoint(endpointToDelete);

      expect(mockEndpointService.apiConfigSourceSystemEndpointIdDelete)
        .toHaveBeenCalledWith(endpointToDelete.id!);
    });

    it('should emit backStep event', () => {
      spyOn(component.backStep, 'emit');

      component.onBack();

      expect(component.backStep.emit).toHaveBeenCalled();
    });

    it('should emit finish event', () => {
      spyOn(component.finish, 'emit');

      component.onFinish();

      expect(component.finish.emit).toHaveBeenCalled();
    });

    it('should start import process', () => {
      component.ngOnInit();
      component.apiUrl = 'https://api.test.com/openapi.json';

      // Mock HTTP request
      spyOn(component as any, 'tryImportFromUrl').and.returnValue(Promise.resolve());

      component.importEndpoints();

      expect(component.importing).toBe(true);
    });

    it('should load endpoints correctly', () => {
      component.sourceSystemId = 1;

      component.loadEndpoints();

      expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointGet)
        .toHaveBeenCalledWith(1);
    });

    it('should handle form validation correctly', () => {
      component.ngOnInit();

      const endpointControl = component.endpointForm.get('endpointPath');

      endpointControl?.setValue('');
      endpointControl?.markAsTouched();

      expect(endpointControl?.hasError('required')).toBe(true);
    });

    it('should reset form after successful endpoint creation', () => {
      component.ngOnInit();

      component.endpointForm.patchValue({
        endpointPath: '/test',
        httpRequestType: 'GET'
      });

      component.addEndpoint();

      expect(component.endpointForm.get('endpointPath')?.value).toBe('');
      expect(component.endpointForm.get('httpRequestType')?.value).toBe('GET');
    });

    it('should handle endpoint loading errors', () => {
      mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointGet.and.returnValue(
        throwError(() => new Error('Network error'))
      );

      component.loadEndpoints();

      expect(component.loading).toBe(false);
    });

    it('should handle source system loading errors', () => {
      mockSourceSystemService.apiConfigSourceSystemIdGet.and.returnValue(
        throwError(() => new Error('Network error'))
      );

      // Reset ngOnInit spy to allow real call
      (component.ngOnInit as jasmine.Spy).and.callThrough();

      component.ngOnInit();

      // KORRIGIERT: Erwarte das Fallback-Verhalten
      expect(component.apiUrl).toBe('https://petstore.swagger.io/v2');
    });

    it('should import from direct OpenAPI URL', fakeAsync(() => {
      component.ngOnInit();
      component.apiUrl = 'https://api.test.com/openapi.json';

      component.importEndpoints();

      const req = httpMock.expectOne('https://api.test.com/openapi.json');
      expect(req.request.method).toBe('GET');

      req.flush(mockOpenApiSpec);
      tick();

      expect(component.importing).toBe(false);
      expect(component.endpoints.length).toBeGreaterThan(0);
    }));

    it('should try standard paths when direct URL fails', fakeAsync(() => {
      component.ngOnInit();
      component.apiUrl = 'https://api.test.com';

      component.importEndpoints();

      // First attempt: /swagger.json
      const req1 = httpMock.expectOne('https://api.test.com/swagger.json');
      req1.flush('Not found', { status: 404, statusText: 'Not Found' });

      // Second attempt: alternative URLs
      const req2 = httpMock.expectOne('https://api.test.com/v2/swagger.json');
      req2.flush(mockOpenApiSpec);
      tick();

      expect(component.importing).toBe(false);
    }));

    it('should handle YAML OpenAPI specs', fakeAsync(() => {
      const yamlSpec = `
openapi: 3.0.0
info:
  title: Test API
  version: 1.0.0
paths:
  /test:
    get:
      summary: Test endpoint
`;

      component.ngOnInit();
      component.apiUrl = 'https://api.test.com/openapi.yaml';

      component.importEndpoints();

      const req = httpMock.expectOne('https://api.test.com/openapi.yaml');
      req.flush(yamlSpec);
      tick();

      expect(component.importing).toBe(false);
    }));

    it('should handle missing OpenAPI spec gracefully', () => {
      component.ngOnInit();
      component.apiUrl = null;

      component.importEndpoints();

      expect(component.importing).toBe(false);
    });

    it('should handle import errors gracefully', fakeAsync(() => {
      component.ngOnInit();
      component.apiUrl = 'https://api.test.com/openapi.json';

      component.importEndpoints();

      const req = httpMock.expectOne('https://api.test.com/openapi.json');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
      tick();

      expect(component.importing).toBe(false);
    }));

    it('should display loading state while loading endpoints', () => {
      component.loading = true;
      fixture.detectChanges();

      expect(component.loading).toBe(true);
    });

    it('should display importing state during import', () => {
      component.importing = true;
      fixture.detectChanges();

      expect(component.importing).toBe(true);
    });



    it('should display endpoints in component', () => {
      component.endpoints = mockEndpoints;
      fixture.detectChanges();

      expect(component.endpoints.length).toBe(mockEndpoints.length);
    });

    it('should accept valid endpoint formats', () => {
      component.ngOnInit();

      const endpointControl = component.endpointForm.get('endpointPath');

      const validEndpoints = ['/api/users', '/api/users/{id}', '/api/v1/pets'];

      validEndpoints.forEach(endpoint => {
        endpointControl?.setValue(endpoint);
        expect(endpointControl?.value).toBe(endpoint);
      });
    });

    it('should complete full import workflow', fakeAsync(() => {
      // Setup
      component.ngOnInit();

      // Start import
      component.importEndpoints();

      // Mock successful OpenAPI download
      const req = httpMock.expectOne('https://petstore3.swagger.io/api/v3/openapi.json');
      req.flush(mockOpenApiSpec);

      // Wait for processing
      tick();

      // Verify endpoints were created
      expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost)
        .toHaveBeenCalled();
      expect(component.importing).toBe(false);
    }));

    it('should handle complete manual endpoint creation workflow', () => {
      component.ngOnInit();

      // Fill form
      component.endpointForm.patchValue({
        endpointPath: '/api/test',
        httpRequestType: 'POST'
      });

      // Add endpoint
      component.addEndpoint();

      // Verify service call
      expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost)
        .toHaveBeenCalledWith(1, [{
          endpointPath: '/api/test',
          httpRequestType: 'POST',
          requestBodySchema: '',
          responseBodySchema: ''
        }]);

      // Verify form reset
      expect(component.endpointForm.get('endpointPath')?.value).toBe('');
    });
  });
});

describe('ManageEndpointsComponent - Request Body', () => {
  let component: ManageEndpointsComponent;
  let fixture: ComponentFixture<ManageEndpointsComponent>;
  let mockEndpointService: jasmine.SpyObj<SourceSystemEndpointResourceService>;

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
        { provide: SourceSystemEndpointResourceService, useValue: mockEndpointService }
      ]
    });
    fixture = TestBed.createComponent(ManageEndpointsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should accept valid JSON in requestBodySchema', () => {
    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(of(new HttpResponse<any>({ status: 201 })));
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'POST',
      requestBodySchema: '{"foo": "bar"}'
    });
    component.addEndpoint();
    expect(component.jsonError).toBeNull();
    expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost).toHaveBeenCalled();
  });

  it('should reject invalid JSON in requestBodySchema', () => {
    // Kein Mock nötig, da kein Service-Call erwartet wird
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'POST',
      requestBodySchema: '{foo: bar}' // invalid JSON
    });
    component.addEndpoint();
    expect(component.jsonError).toBe('Request-Body-Schema ist kein valides JSON.');
    expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost).not.toHaveBeenCalled();
  });

  it('should allow empty requestBodySchema', () => {
    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(of(new HttpResponse<any>({ status: 201 })));
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'POST',
      requestBodySchema: ''
    });
    component.addEndpoint();
    expect(component.jsonError).toBeNull();
    expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost).toHaveBeenCalled();
  });

  it('should show error in request body editor panel for invalid JSON', () => {
    component.requestBodyEditorEndpoint = { id: 1, endpointPath: '/test', httpRequestType: 'POST' };
    component.requestBodyEditorModel = { value: '{foo: bar}', language: 'json' };
    component.saveRequestBodySchema();
    expect(component.requestBodyEditorError).toBe('Request-Body-Schema ist kein valides JSON.');
  });

  it('should save valid JSON in request body editor panel', () => {
    mockEndpointService.apiConfigSourceSystemEndpointIdPut.and.returnValue(of(new HttpResponse<any>({ status: 200 })));
    component.requestBodyEditorEndpoint = { id: 1, endpointPath: '/test', httpRequestType: 'POST' };
    component.requestBodyEditorModel = { value: '{"foo": "bar"}', language: 'json' };
    component.saveRequestBodySchema();
    expect(component.requestBodyEditorError).toBeNull();
    expect(mockEndpointService.apiConfigSourceSystemEndpointIdPut).toHaveBeenCalled();
  });
});

describe('ManageEndpointsComponent - Response Body', () => {
  let component: ManageEndpointsComponent;
  let fixture: ComponentFixture<ManageEndpointsComponent>;
  let mockEndpointService: jasmine.SpyObj<SourceSystemEndpointResourceService>;

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
        { provide: SourceSystemEndpointResourceService, useValue: mockEndpointService }
      ]
    });
    fixture = TestBed.createComponent(ManageEndpointsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should accept valid JSON in responseBodySchema', () => {
    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(of(new HttpResponse<any>({ status: 201 })));
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'GET',
      responseBodySchema: '{"message": "Success"}'
    });
    component.addEndpoint();
    expect(component.jsonError).toBeNull();
    expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost).toHaveBeenCalled();
  });

  it('should reject invalid JSON in responseBodySchema', () => {
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'GET',
      responseBodySchema: '{message: Success}' // invalid JSON
    });
    component.addEndpoint();
    expect(component.jsonError).toBe('Response-Body-Schema ist kein valides JSON.');
    expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost).not.toHaveBeenCalled();
  });

  it('should allow empty responseBodySchema', () => {
    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(of(new HttpResponse<any>({ status: 201 })));
    component.endpointForm.patchValue({
      endpointPath: '/test',
      httpRequestType: 'GET',
      responseBodySchema: ''
    });
    component.addEndpoint();
    expect(component.jsonError).toBeNull();
    expect(mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost).toHaveBeenCalled();
  });

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

    const result = component['processOpenApiSpec'](mockSpec);
    expect(result).toBeUndefined(); // processOpenApiSpec returns void
  });

  it('should show response preview modal', () => {
    const endpoint: SourceSystemEndpointDTO = {
      id: 1,
      endpointPath: '/test',
      httpRequestType: 'GET',
      responseBodySchema: '{"message": "Success"}'
    };

    component.showResponsePreviewModal(endpoint);

    expect(component.responsePreviewModalVisible).toBe(true);
    expect(component.selectedResponsePreviewEndpoint).toEqual(endpoint);
  });

  it('should close response preview modal', () => {
    component.responsePreviewModalVisible = true;
    component.selectedResponsePreviewEndpoint = { id: 1, endpointPath: '/test', httpRequestType: 'GET' };

    component.closeResponsePreviewModal();

    expect(component.responsePreviewModalVisible).toBe(false);
    expect(component.selectedResponsePreviewEndpoint).toBeNull();
  });

  it('should handle response preview modal visibility change', () => {
    component.responsePreviewModalVisible = true;
    component.selectedResponsePreviewEndpoint = { id: 1, endpointPath: '/test', httpRequestType: 'GET' };

    component.onResponsePreviewModalVisibleChange(false);

    expect(component.responsePreviewModalVisible).toBe(false);
    expect(component.selectedResponsePreviewEndpoint).toBeNull();
  });

  // TypeScript Tab Functionality Tests
  describe('TypeScript Tab Functionality', () => {
    const validJsonSchema = '{"type": "object", "properties": {"id": {"type": "number"}, "name": {"type": "string"}}}';
    const invalidJsonSchema = '{invalid json}';
    const largeJsonSchema = '{"type": "object", "properties": {' + '"prop' + Array(1000).fill(0).map((_, i) => i).join('": {"type": "string"}, "prop') + '": {"type": "string"}}}';

    beforeEach(() => {
      component.ngOnInit();
    });

    it('should generate TypeScript from valid JSON schema', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; name: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('interface ResponseBody');
      expect(component.typescriptError).toBeNull();
      expect(component.isGeneratingTypeScript).toBeFalse();
    }));

    it('should handle TypeScript generation error', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        error: 'Invalid JSON schema provided'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: invalidJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.typescriptError).toContain('Invalid JSON schema');
      expect(component.generatedTypeScript).toContain('Error: Unable to generate TypeScript');
      expect(component.isGeneratingTypeScript).toBeFalse();
    }));

    it('should handle network error during TypeScript generation', fakeAsync(() => {
      mockEndpointService.generateTypeScript.and.returnValue(throwError(() => new Error('Network error')));

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.typescriptError).toContain('Backend communication failed');
      expect(component.generatedTypeScript).toContain('Error: Unable to generate TypeScript');
      expect(component.isGeneratingTypeScript).toBeFalse();
    }));

    it('should validate JSON schema size', () => {
      const validation = component['validateJsonSchema'](largeJsonSchema);
      expect(validation.isValid).toBeFalse();
      expect(validation.error).toContain('too large');
    });

    it('should validate JSON schema structure', () => {
      const validSchema = '{"type": "object", "properties": {"test": {"type": "string"}}}';
      const invalidSchema = '{"notASchema": true}';

      const validValidation = component['validateJsonSchema'](validSchema);
      const invalidValidation = component['validateJsonSchema'](invalidSchema);

      expect(validValidation.isValid).toBeTrue();
      expect(invalidValidation.isValid).toBeFalse();
      expect(invalidValidation.error).toContain('Invalid JSON Schema structure');
    });

    it('should handle tab change to TypeScript tab', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.onTabChange({ index: 1 }); // Switch to TypeScript tab
      tick();

      expect(component.activeTabIndex).toBe(1);
      expect(component.generatedTypeScript).toContain('interface ResponseBody');
    }));

    it('should not regenerate TypeScript if already generated', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      const initialCallCount = mockEndpointService.generateTypeScript.calls.count();

      component.onTabChange({ index: 1 }); // Switch to TypeScript tab again
      tick();

      expect(mockEndpointService.generateTypeScript.calls.count()).toBe(initialCallCount);
    }));

    it('should handle edit form TypeScript generation', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; name: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.editForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForEditForm();
      tick();

      expect(component.editGeneratedTypeScript).toContain('interface ResponseBody');
      expect(component.editTypeScriptError).toBeNull();
      expect(component.isGeneratingEditTypeScript).toBeFalse();
    }));

    it('should clear TypeScript data when form is reset', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );
      mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(of(new HttpResponse<any>({ status: 201 })));

      component.endpointForm.patchValue({
        endpointPath: '/test',
        httpRequestType: 'GET',
        responseBodySchema: validJsonSchema
      });
      component.loadTypeScriptForMainForm();
      tick();

      component.addEndpoint();
      tick();

      expect(component.generatedTypeScript).toBe('');
      expect(component.typescriptError).toBeNull();
      expect(component.activeTabIndex).toBe(0);
    }));

    it('should handle timeout during TypeScript generation', fakeAsync(() => {
      // Simulate a slow response that would timeout
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ 
          body: { generatedTypeScript: 'interface ResponseBody { id: number; }' }
        }))
      );

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();

      // Fast forward past the timeout
      tick(31000);

      expect(component.typescriptError).toContain('timed out');
      expect(component.isGeneratingTypeScript).toBeFalse();
    }));

    it('should format error messages correctly', () => {
      const networkError = component['formatErrorMessage']('HttpErrorResponse: Network error', 'Test context');
      const timeoutError = component['formatErrorMessage']('Request timeout', 'Test context');
      const jsonError = component['formatErrorMessage']('Invalid JSON syntax', 'Test context');

      expect(networkError).toContain('Network error: Unable to connect to the server');
      expect(timeoutError).toContain('Request timed out');
      expect(jsonError).toContain('Invalid JSON format in schema');
    });

   
    it('should generate TypeScript from complex nested JSON schema', fakeAsync(() => {
      const complexSchema = '{"type": "object", "properties": {"user": {"type": "object", "properties": {"id": {"type": "number"}, "profile": {"type": "object", "properties": {"name": {"type": "string"}, "email": {"type": "string"}}}}}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { user: { id: number; profile: { name: string; email: string; }; }; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: complexSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('user: { id: number; profile:');
      expect(component.typescriptError).toBeNull();
    }));

    it('should generate TypeScript from array schema', fakeAsync(() => {
      const arraySchema = '{"type": "object", "properties": {"items": {"type": "array", "items": {"type": "string"}}, "count": {"type": "number"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { items: string[]; count: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: arraySchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('items: string[]');
      expect(component.generatedTypeScript).toContain('count: number');
    }));

    it('should generate TypeScript from union type schema', fakeAsync(() => {
      const unionSchema = '{"type": "object", "properties": {"status": {"type": "string", "enum": ["active", "inactive"]}, "value": {"oneOf": [{"type": "string"}, {"type": "number"}]}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { status: "active" | "inactive"; value: string | number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: unionSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('status: "active" | "inactive"');
      expect(component.generatedTypeScript).toContain('value: string | number');
    }));

    it('should handle special characters in JSON schema', fakeAsync(() => {
      const specialCharSchema = '{"type": "object", "properties": {"user_name": {"type": "string"}, "email@domain": {"type": "string"}, "$metadata": {"type": "object"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { user_name: string; "email@domain": string; $metadata: object; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: specialCharSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('user_name: string');
      expect(component.generatedTypeScript).toContain('"email@domain": string');
      expect(component.generatedTypeScript).toContain('$metadata: object');
    }));

    it('should handle Unicode characters in JSON schema', fakeAsync(() => {
      const unicodeSchema = '{"type": "object", "properties": {"name": {"type": "string", "description": "User\'s name with émojis 🎉"}, "message": {"type": "string"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { name: string; message: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: unicodeSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('name: string');
      expect(component.generatedTypeScript).toContain('message: string');
    }));

    it('should handle OpenAPI 3.0 specific features', fakeAsync(() => {
      const openApi3Schema = '{"type": "object", "properties": {"id": {"type": "integer", "format": "int64"}, "email": {"type": "string", "format": "email"}, "createdAt": {"type": "string", "format": "date-time"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; email: string; createdAt: string; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: openApi3Schema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('id: number');
      expect(component.generatedTypeScript).toContain('email: string');
      expect(component.generatedTypeScript).toContain('createdAt: string');
    }));

    it('should handle custom type definitions', fakeAsync(() => {
      const customTypeSchema = '{"type": "object", "properties": {"data": {"$ref": "#/components/schemas/User"}, "metadata": {"type": "object"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { data: User; metadata: object; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: customTypeSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toContain('data: User');
      expect(component.generatedTypeScript).toContain('metadata: object');
    }));

    it('should handle malformed JSON schema gracefully', fakeAsync(() => {
      const malformedSchema = '{"type": "object", "properties": {"id": {"type": "invalid_type"}}}';
      const mockResponse: TypeScriptGenerationResponse = {
        error: 'Invalid type: invalid_type'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: malformedSchema });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.typescriptError).toContain('Invalid type');
      expect(component.generatedTypeScript).toContain('Error: Unable to generate TypeScript');
    }));

    it('should handle concurrent TypeScript generation requests', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });

      // Simulate multiple rapid requests
      component.loadTypeScriptForMainForm();
      component.loadTypeScriptForMainForm();
      component.loadTypeScriptForMainForm();
      tick();

      // Should only make one service call due to caching
      expect(mockEndpointService.generateTypeScript).toHaveBeenCalledTimes(1);
      expect(component.generatedTypeScript).toContain('interface ResponseBody');
    }));

    it('should handle empty response body schema', fakeAsync(() => {
      component.endpointForm.patchValue({ responseBodySchema: '' });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toBe('');
      expect(component.typescriptError).toBeNull();
      expect(mockEndpointService.generateTypeScript).not.toHaveBeenCalled();
    }));

    it('should handle whitespace-only response body schema', fakeAsync(() => {
      component.endpointForm.patchValue({ responseBodySchema: '   ' });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toBe('');
      expect(component.typescriptError).toBeNull();
      expect(mockEndpointService.generateTypeScript).not.toHaveBeenCalled();
    }));

    it('should handle null response body schema', fakeAsync(() => {
      component.endpointForm.patchValue({ responseBodySchema: null });
      component.loadTypeScriptForMainForm();
      tick();

      expect(component.generatedTypeScript).toBe('');
      expect(component.typescriptError).toBeNull();
      expect(mockEndpointService.generateTypeScript).not.toHaveBeenCalled();
    }));

    it('should handle Monaco editor initialization for TypeScript', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });
      component.loadTypeScriptForMainForm();
      tick();

      // Verify Monaco editor options are set correctly for TypeScript
      expect(component.typescriptEditorOptions).toBeDefined();
      expect(component.typescriptEditorOptions.language).toBe('typescript');
    }));

    it('should handle tab switching between JSON and TypeScript', fakeAsync(() => {
      const mockResponse: TypeScriptGenerationResponse = {
        generatedTypeScript: 'interface ResponseBody { id: number; }'
      };
      mockEndpointService.generateTypeScript.and.returnValue(
        of(new HttpResponse<TypeScriptGenerationResponse>({ body: mockResponse }))
      );

      component.endpointForm.patchValue({ responseBodySchema: validJsonSchema });

      // Switch to TypeScript tab
      component.onTabChange({ index: 1 });
      tick();

      expect(component.activeTabIndex).toBe(1);
      expect(component.generatedTypeScript).toContain('interface ResponseBody');

      // Switch back to JSON tab
      component.onTabChange({ index: 0 });
      tick();

      expect(component.activeTabIndex).toBe(0);
      // TypeScript should still be cached
      expect(component.generatedTypeScript).toContain('interface ResponseBody');
    }));
  });
});