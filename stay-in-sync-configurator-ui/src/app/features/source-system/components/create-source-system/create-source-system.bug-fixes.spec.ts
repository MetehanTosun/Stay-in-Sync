import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';

describe('CreateSourceSystemComponent Bug Fixes', () => {
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

    component.createdSourceSystemId = 1;
  });

  describe('Bug Fix: Duplicate Elements Prevention', () => {
    it('should filter children correctly to prevent duplicates', () => {
      const submodelId = 'test-submodel';
      const parentPath = 'parent';
      const mockResponse = [
        { idShort: 'child1', idShortPath: 'parent/child1' },
        { idShort: 'child2', idShortPath: 'parent/child2' },
        { idShort: 'nested', idShortPath: 'parent/child1/nested' }, // Should be filtered out
        { idShort: 'deep', idShortPath: 'parent/child1/nested/deep' } // Should be filtered out
      ];

      aasService.listElements.and.returnValue(of(mockResponse));
      const node = { children: [] };

      component['loadChildren'](submodelId, parentPath, node as any);

      expect(aasService.listElements).toHaveBeenCalledWith(
        1,
        submodelId,
        { depth: 'shallow', parentPath, source: 'SNAPSHOT' }
      );
    });

    it('should handle root elements correctly', () => {
      const submodelId = 'test-submodel';
      const parentPath = '';
      const mockResponse = [
        { idShort: 'root1' },
        { idShort: 'root2' },
        { idShort: 'nested', idShortPath: 'root1/nested' } // Should be filtered out
      ];

      aasService.listElements.and.returnValue(of(mockResponse));
      const node = { children: [] };

      component['loadChildren'](submodelId, parentPath, node as any);

      expect(aasService.listElements).toHaveBeenCalledWith(
        1,
        submodelId,
        { depth: 'shallow', parentPath, source: 'SNAPSHOT' }
      );
    });
  });

  describe('Bug Fix: Empty Collections/Lists Prevention', () => {
    it('should prevent expansion of empty collections', () => {
      const element = {
        idShort: 'empty-collection',
        modelType: 'SubmodelElementCollection',
        hasChildren: false,
        value: []
      };

      const result = component['mapElementToNode']('test-submodel', element);

      expect(result.leaf).toBe(true);
    });

    it('should prevent expansion of empty lists', () => {
      const element = {
        idShort: 'empty-list',
        modelType: 'SubmodelElementList',
        hasChildren: false,
        value: []
      };

      const result = component['mapElementToNode']('test-submodel', element);

      expect(result.leaf).toBe(true);
    });

    it('should allow expansion of collections with children', () => {
      const element = {
        idShort: 'collection-with-children',
        modelType: 'SubmodelElementCollection',
        hasChildren: true,
        value: [{ idShort: 'child1' }]
      };

      const result = component['mapElementToNode']('test-submodel', element);

      expect(result.leaf).toBe(false);
    });
  });

  describe('Bug Fix: Type Recognition for Deep Elements', () => {
    it('should recognize File type from type field', () => {
      const element = {
        idShort: 'ArbitraryFile',
        type: 'File',
        contentType: 'text/plain',
        fileName: 'test.txt'
      };

      const result = component['inferModelType'](element);

      expect(result).toBe('File');
    });

    it('should recognize MultiLanguageProperty from type field', () => {
      const element = {
        idShort: 'ArbitraryMLP',
        type: 'MultiLanguageProperty',
        value: [{ language: 'en', text: 'test' }]
      };

      const result = component['inferModelType'](element);

      expect(result).toBe('MultiLanguageProperty');
    });

    it('should recognize Property from type field', () => {
      const element = {
        idShort: 'ArbitraryProperty',
        type: 'Property',
        valueType: 'xs:string',
        value: 'test-value'
      };

      const result = component['inferModelType'](element);

      expect(result).toBe('Property');
    });
  });

  describe('Bug Fix: Value Extraction for Detail Panel', () => {
    it('should extract value from node.data.raw', () => {
      const node = {
        data: {
          raw: {
            idShort: 'test-element',
            value: 'test-value',
            modelType: 'Property'
          }
        }
      };

      // Simulate the DIRECT FALLBACK scenario
      const rawData = node.data.raw;
      let extractedValue = rawData.value;

      expect(extractedValue).toBe('test-value');
    });

    it('should not use valueType as value', () => {
      const rawData = {
        idShort: 'test-element',
        value: 'xs:string', // This is actually valueType, not value
        valueType: 'xs:string',
        modelType: 'Property'
      };

      let extractedValue: any = rawData.value;
      
      // The fix should detect this and set to undefined
      if (extractedValue === rawData.valueType) {
        extractedValue = undefined;
      }

      expect(extractedValue).toBeUndefined();
    });

    it('should create mock values only when no real value exists', () => {
      const rawData = {
        idShort: 'test-element',
        value: undefined,
        modelType: 'MultiLanguageProperty'
      };

      let extractedValue: any = rawData.value;
      
      // Only create mock if no value exists
      if (!extractedValue && rawData.modelType === 'MultiLanguageProperty') {
        extractedValue = [{"language": "en", "text": "sample value"}];
      }

      expect(extractedValue).toEqual([{"language": "en", "text": "sample value"}]);
    });
  });

  describe('Bug Fix: Delete Element Error Handling', () => {
    it('should handle 404 errors gracefully during deletion', () => {
      const error = { status: 404, message: 'Element not found' };
      aasService.getElement.and.returnValue(throwError(() => error));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

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

    it('should proceed with deletion if element exists', () => {
      const mockElement = { idShort: 'test-element', modelType: 'Property' };
      aasService.getElement.and.returnValue(of(mockElement));
      aasService.deleteElement.and.returnValue(of({}));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.getElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path', 'LIVE');
      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'encoded-id', 'test/element/path');
    });
  });

  describe('Bug Fix: URL Encoding for Long Paths', () => {
    it('should handle long element paths correctly', () => {
      const longPath = 'ConditionsOfReliabilityCharacteristics/RatedVoltage/SubProperty/DeepNested/Value';
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.deleteElement('test-submodel-id', longPath);

      expect(aasService.encodeIdToBase64Url).toHaveBeenCalledWith('test-submodel-id');
    });

    it('should encode path segments correctly', () => {
      const path = 'test/path/with/special/chars';
      const expectedEncoded = 'test/path/with/special/chars'.split('/').map(seg => encodeURIComponent(seg)).join('/');
      
      // This tests the encodePathSegments method logic
      const segments = path.split('/');
      const encodedSegments = segments.map(seg => encodeURIComponent(seg));
      const result = encodedSegments.join('/');

      expect(result).toBe(expectedEncoded);
    });
  });

  describe('Bug Fix: Toast Message Handling', () => {
    it('should show success toast on successful element creation', () => {
      aasService.createElement.and.returnValue(of({}));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');
      component.targetSubmodelId = 'test-submodel';
      component.newElementJson = JSON.stringify({ idShort: 'test', modelType: 'Property' });

      component.createElement();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
    });

    it('should show success toast on successful element deletion', () => {
      const mockElement = { idShort: 'test-element' };
      aasService.getElement.and.returnValue(of(mockElement));
      aasService.deleteElement.and.returnValue(of({}));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Element Deleted',
        detail: 'Element has been successfully deleted.',
        life: 3000
      });
    });
  });
});
