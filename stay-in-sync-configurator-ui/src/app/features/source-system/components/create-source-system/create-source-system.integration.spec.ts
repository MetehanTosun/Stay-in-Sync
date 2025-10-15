import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';

describe('CreateSourceSystemComponent Integration Tests', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let httpMock: HttpTestingController;
  let aasService: AasService;
  let messageService: MessageService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        HttpClientTestingModule
      ],
      providers: [
        AasService,
        MessageService,
        HttpErrorService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    aasService = TestBed.inject(AasService);
    messageService = TestBed.inject(MessageService);

    // Set up component state
    component.createdSourceSystemId = 1;
    component.targetSubmodelId = 'https://admin-shell.io/idta/SubmodelTemplate/CarbonFootprint/0/9';
    component.parentPath = 'ConditionsOfReliabilityCharacteristics';
    component.newElementJson = JSON.stringify({
      idShort: 'RatedVoltage',
      modelType: 'Property',
      valueType: 'xs:double',
      value: 230.0
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Element Creation Flow', () => {
    it('should create element with correct API call', () => {
      spyOn(component, 'refreshTreeAfterCreate').and.stub();
      
      component.createElement();

      const req = httpMock.expectOne((request) => {
        return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
               request.url.includes('/elements') &&
               request.method === 'POST';
      });

      expect(req.request.body).toEqual({
        idShort: 'RatedVoltage',
        modelType: 'Property',
        valueType: 'xs:double',
        value: 230.0
      });
      expect(req.request.params.get('parentPath')).toBe('ConditionsOfReliabilityCharacteristics');

      req.flush({});
    });

    it('should handle creation error', () => {
      spyOn(component, 'refreshTreeAfterCreate').and.stub();
      
      component.createElement();

      const req = httpMock.expectOne((request) => {
        return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
               request.url.includes('/elements') &&
               request.method === 'POST';
      });

      req.flush({ error: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('Element Deletion Flow', () => {
    it('should verify element exists before deletion', () => {
      const elementPath = 'ConditionsOfReliabilityCharacteristics/RatedVoltage';
      const mockElement = {
        idShort: 'RatedVoltage',
        modelType: 'Property',
        value: 230.0
      };

      component.deleteElement('https://admin-shell.io/idta/SubmodelTemplate/CarbonFootprint/0/9', elementPath);

      // First request: getElement to verify existence
      const getReq = httpMock.expectOne((request) => {
        return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
               request.url.includes('/elements/') &&
               request.method === 'GET';
      });
      getReq.flush(mockElement);

      // Second request: deleteElement
      const deleteReq = httpMock.expectOne((request) => {
        return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
               request.url.includes('/elements/') &&
               request.method === 'DELETE';
      });
      deleteReq.flush({});
    });

    it('should handle element not found during verification', () => {
      const elementPath = 'ConditionsOfReliabilityCharacteristics/RatedVoltage';

      component.deleteElement('https://admin-shell.io/idta/SubmodelTemplate/CarbonFootprint/0/9', elementPath);

      // First request: getElement returns 404
      const getReq = httpMock.expectOne((request) => {
        return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
               request.url.includes('/elements/') &&
               request.method === 'GET';
      });
      getReq.flush({ error: 'Not found' }, { status: 404, statusText: 'Not Found' });

      // Should not make delete request
      httpMock.expectNone((request) => request.method === 'DELETE');
    });
  });

  describe('Tree Loading Flow', () => {
    it('should load children with correct filtering', () => {
      const submodelId = 'https://admin-shell.io/idta/SubmodelTemplate/CarbonFootprint/0/9';
      const parentPath = 'ConditionsOfReliabilityCharacteristics';
      const mockChildren = [
        { idShort: 'RatedVoltage', idShortPath: 'ConditionsOfReliabilityCharacteristics/RatedVoltage' },
        { idShort: 'Temperature', idShortPath: 'ConditionsOfReliabilityCharacteristics/Temperature' },
        { idShort: 'Nested', idShortPath: 'ConditionsOfReliabilityCharacteristics/RatedVoltage/Nested' }
      ];

      component.createdSourceSystemId = 1;
      const node = { children: [] };

      component['loadChildren'](submodelId, parentPath, node as any);

      const req = httpMock.expectOne((request) => {
        return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
               request.url.includes('/elements') &&
               request.method === 'GET';
      });

      expect(req.request.params.get('depth')).toBe('shallow');
      expect(req.request.params.get('parentPath')).toBe(parentPath);
      expect(req.request.params.get('source')).toBe('SNAPSHOT');

      req.flush(mockChildren);
    });
  });

  describe('Type Inference Flow', () => {
    it('should correctly infer types for various AAS elements', () => {
      const testCases = [
        {
          element: { valueType: 'xs:string', value: 'test' },
          expectedType: 'Property'
        },
        {
          element: { 
            value: [
              { language: 'en', text: 'English' },
              { language: 'de', text: 'German' }
            ]
          },
          expectedType: 'MultiLanguageProperty'
        },
        {
          element: { 
            contentType: 'text/plain',
            fileName: 'document.txt'
          },
          expectedType: 'File'
        },
        {
          element: { 
            value: [{ idShort: 'child1' }],
            hasChildren: true
          },
          expectedType: 'SubmodelElementCollection'
        }
      ];

      testCases.forEach(({ element, expectedType }) => {
        const result = component['inferModelType'](element);
        expect(result).toBe(expectedType);
      });
    });
  });

  describe('Error Handling Flow', () => {
    it('should handle network errors gracefully', () => {
      spyOn(component, 'refreshTreeAfterCreate').and.stub();
      
      component.createElement();

      const req = httpMock.expectOne((request) => {
        return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
               request.url.includes('/elements') &&
               request.method === 'POST';
      });

      req.error(new ErrorEvent('Network error'));
    });

    it('should handle malformed JSON gracefully', () => {
      component.newElementJson = 'invalid-json';
      
      component.createElement();

      // Should not make HTTP request with invalid JSON
      httpMock.expectNone((request) => request.method === 'POST');
    });
  });
});
