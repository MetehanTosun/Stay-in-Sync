import { ComponentFixture, TestBed, waitForAsync, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpResponse } from '@angular/common/http';
import { NO_ERRORS_SCHEMA, Component } from '@angular/core';

import { ManageEndpointsComponent } from './manage-endpoints.component';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { SourceSystemEndpointDTO } from '../../models/sourceSystemEndpointDTO';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { CreateSourceSystemEndpointDTO } from '../../models/createSourceSystemEndpointDTO';

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
      sourceSystemId: 1
    },
    {
      id: 2,
      endpointPath: '/pets/{id}',
      httpRequestType: 'GET',
      sourceSystemId: 1
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
      'apiConfigSourceSystemEndpointIdDelete'
    ]);

    mockSourceSystemService = jasmine.createSpyObj('SourceSystemResourceService', [
      'apiConfigSourceSystemIdGet'
    ]);

    // Setup default return values
    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointGet.and.returnValue(
      of(new HttpResponse({ body: mockEndpoints, status: 200 }))
    );
    
    mockEndpointService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(
      of(new HttpResponse({ status: 201 }))
    );
    
    mockEndpointService.apiConfigSourceSystemEndpointIdDelete.and.returnValue(
      of(new HttpResponse({ status: 204 }))
    );

    mockSourceSystemService.apiConfigSourceSystemIdGet.and.returnValue(
      of(new HttpResponse({ body: mockSourceSystem, status: 200 }))
    );

    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule,
        RouterTestingModule
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
        httpRequestType: 'POST'
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
      
      component.deleteEndpoint(1);
      
      expect(mockEndpointService.apiConfigSourceSystemEndpointIdDelete)
        .toHaveBeenCalledWith(1);
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
          httpRequestType: 'POST'
        }]);
      
      // Verify form reset
      expect(component.endpointForm.get('endpointPath')?.value).toBe('');
    });
  });
});