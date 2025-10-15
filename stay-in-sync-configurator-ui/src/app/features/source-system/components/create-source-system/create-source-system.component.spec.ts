import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';

describe('CreateSourceSystemComponent', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let aasService: jasmine.SpyObj<AasService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let httpErrorService: jasmine.SpyObj<HttpErrorService>;

  beforeEach(async () => {
    const aasServiceSpy = jasmine.createSpyObj('AasService', [
      'createElement', 'deleteElement', 'getElement', 'listElements', 'encodeIdToBase64Url'
    ]);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);
    const httpErrorServiceSpy = jasmine.createSpyObj('HttpErrorService', ['handleError']);

    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        ReactiveFormsModule,
        FormsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: AasService, useValue: aasServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: HttpErrorService, useValue: httpErrorServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    aasService = TestBed.inject(AasService) as jasmine.SpyObj<AasService>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
    httpErrorService = TestBed.inject(HttpErrorService) as jasmine.SpyObj<HttpErrorService>;

    // Set up component state
    component.createdSourceSystemId = 1;
    component.targetSubmodelId = 'test-submodel-id';
    component.parentPath = 'test/parent';
    component.newElementJson = JSON.stringify({
      idShort: 'test-element',
      modelType: 'Property',
      valueType: 'xs:string',
      value: 'test-value'
    });
  });

  describe('createElement', () => {
    it('should create element successfully', () => {
      aasService.createElement.and.returnValue(of({}));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.createElement();

      expect(aasService.createElement).toHaveBeenCalledWith(
        1,
        'encoded-id',
        jasmine.any(Object),
        'test/parent'
      );
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
    });

    it('should handle creation error', () => {
      const error = { status: 500, message: 'Server error', name: 'HttpErrorResponse', ok: false, headers: {} as any, url: '', error: null, statusText: '', type: 4 };
      aasService.createElement.and.returnValue(throwError(() => error));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.createElement();

      expect(httpErrorService.handleError).toHaveBeenCalledWith(error);
    });

    it('should handle JSON parse error', () => {
      component.newElementJson = 'invalid-json';

      component.createElement();

      expect(httpErrorService.handleError).toHaveBeenCalled();
    });

    it('should not create element if missing required data', () => {
      component.createdSourceSystemId = undefined as any;
      component.targetSubmodelId = '';

      component.createElement();

      expect(aasService.createElement).not.toHaveBeenCalled();
    });
  });

  describe('deleteElement', () => {
    beforeEach(() => {
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');
    });

    it('should delete element successfully', () => {
      aasService.getElement.and.returnValue(of({ idShort: 'test-element' }));
      aasService.deleteElement.and.returnValue(of({}));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.getElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path', 'LIVE');
      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'encoded-id', 'test/element/path');
    });

    it('should handle element not found (404)', () => {
      const error = { status: 404, message: 'Not found' };
      aasService.getElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.getElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path', 'LIVE');
      expect(aasService.deleteElement).not.toHaveBeenCalled();
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'warn',
        summary: 'Element Not Found',
        detail: 'Element does not exist or has already been deleted.',
        life: 3000
      });
    });

    it('should handle other errors when getting element', () => {
      const error = { status: 500, message: 'Server error', name: 'HttpErrorResponse', ok: false, headers: {} as any, url: '', error: null, statusText: '', type: 4 };
      aasService.getElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(httpErrorService.handleError).toHaveBeenCalledWith(error);
    });

    it('should handle delete error', () => {
      aasService.getElement.and.returnValue(of({ idShort: 'test-element' }));
      const error = { status: 500, message: 'Delete failed', name: 'HttpErrorResponse', ok: false, headers: {} as any, url: '', error: null, statusText: '', type: 4 };
      aasService.deleteElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'encoded-id', 'test/element/path');
      expect(httpErrorService.handleError).toHaveBeenCalledWith(error);
    });

    it('should not delete if missing required data', () => {
      component.createdSourceSystemId = undefined as any;

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.getElement).not.toHaveBeenCalled();
      expect(aasService.deleteElement).not.toHaveBeenCalled();
    });
  });

  describe('inferModelType', () => {
    it('should detect Property type', () => {
      const element = { valueType: 'xs:string', value: 'test' };
      const result = component['inferModelType'](element);
      expect(result).toBe('Property');
    });

    it('should detect MultiLanguageProperty type', () => {
      const element = { 
        value: [
          { language: 'en', text: 'English text' },
          { language: 'de', text: 'German text' }
        ]
      };
      const result = component['inferModelType'](element);
      expect(result).toBe('MultiLanguageProperty');
    });

    it('should detect SubmodelElementCollection type', () => {
      const element = { 
        value: [
          { idShort: 'child1' },
          { idShort: 'child2' }
        ],
        hasChildren: true
      };
      const result = component['inferModelType'](element);
      expect(result).toBe('SubmodelElementCollection');
    });

    it('should detect File type', () => {
      const element = { 
        contentType: 'text/plain',
        fileName: 'test.txt'
      };
      const result = component['inferModelType'](element);
      expect(result).toBe('File');
    });

    it('should return undefined for unknown type', () => {
      const element = { idShort: 'unknown' };
      const result = component['inferModelType'](element);
      expect(result).toBeUndefined();
    });
  });

  describe('mapElementToNode', () => {
    it('should map element to tree node correctly', () => {
      const element = {
        idShort: 'test-element',
        modelType: 'Property',
        hasChildren: false,
        value: 'test-value'
      };
      const submodelId = 'test-submodel';

      const result = component['mapElementToNode'](submodelId, element);

      expect(result.key).toBe(`${submodelId}::test-element`);
      expect(result.label).toBe('test-element');
      expect(result.data.type).toBe('element');
      expect(result.data.submodelId).toBe(submodelId);
      expect(result.data.modelType).toBe('Property');
      expect(result.leaf).toBe(true);
    });

    it('should handle elements with children', () => {
      const element = {
        idShort: 'test-collection',
        modelType: 'SubmodelElementCollection',
        hasChildren: true,
        value: [{ idShort: 'child1' }]
      };
      const submodelId = 'test-submodel';

      const result = component['mapElementToNode'](submodelId, element);

      expect(result.leaf).toBe(false);
    });
  });

  describe('loadChildren', () => {
    it('should load children and filter correctly', () => {
      const submodelId = 'test-submodel';
      const parentPath = 'parent';
      const node = { children: [] };
      const mockResponse = [
        { idShort: 'child1', idShortPath: 'parent/child1' },
        { idShort: 'child2', idShortPath: 'parent/child2' },
        { idShort: 'nested', idShortPath: 'parent/child1/nested' }
      ];

      aasService.listElements.and.returnValue(of(mockResponse));
      component.createdSourceSystemId = 1;

      component['loadChildren'](submodelId, parentPath, node as any);

      expect(aasService.listElements).toHaveBeenCalledWith(
        1,
        submodelId,
        { depth: 'shallow', parentPath, source: 'SNAPSHOT' }
      );
    });
  });
});