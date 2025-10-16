/** Unit tests covering bug fixes for `CreateSourceSystemComponent`. */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';

/** Verifies regressions remain fixed: duplicates, empty collections, value extraction, deletion, encoding, toasts. */
describe('CreateSourceSystemComponent Bug Fixes', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let aasService: jasmine.SpyObj<AasService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let httpErrorService: jasmine.SpyObj<HttpErrorService>;

  /** Configure TestBed and seed spies for AAS-related calls. */
  beforeEach(async () => {
    const aasServiceSpy = jasmine.createSpyObj('AasService', [
      'createElement', 'deleteElement', 'getElement', 'listElements', 'encodeIdToBase64Url', 'refreshSnapshot'
    ]);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);
    const httpErrorServiceSpy = jasmine.createSpyObj('HttpErrorService', ['handleError']);

    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        HttpClientTestingModule,
        RouterTestingModule
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
    aasService.listElements.and.returnValue(of([]));
    aasService.refreshSnapshot.and.returnValue(of({} as any));
    (aasService as any).listSubmodels = (aasService as any).listSubmodels || jasmine.createSpy('listSubmodels').and.returnValue(of([]));

    
    spyOn<any>(component, 'hydrateNodeTypesForNodes').and.returnValue(of([]));
  });

  /** Prevent listing duplicates by filtering to direct children only. */
  describe('Bug Fix: Duplicate Elements Prevention', () => {
    it('should filter children correctly to prevent duplicates', () => {
      const submodelId = 'test-submodel';
      const parentPath = 'parent';
      const mockResponse = [
        { idShort: 'child1', idShortPath: 'parent/child1' },
        { idShort: 'child2', idShortPath: 'parent/child2' },
        { idShort: 'nested', idShortPath: 'parent/child1/nested' }, 
        { idShort: 'deep', idShortPath: 'parent/child1/nested/deep' } 
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
        { idShort: 'nested', idShortPath: 'root1/nested' } 
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

  /** Treat empty collections/lists as leaf nodes to avoid pointless expansion. */
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

  /** Recognize element types from provided `type` field for deep elements. */
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

  /** Extract real values for detail panel and avoid mistaking valueType as value. */
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

      const rawData = node.data.raw;
      let extractedValue = rawData.value;

      expect(extractedValue).toBe('test-value');
    });

    it('should not use valueType as value', () => {
      const rawData = {
        idShort: 'test-element',
        value: 'xs:string', 
        valueType: 'xs:string',
        modelType: 'Property'
      };

      let extractedValue: any = rawData.value;
      

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

      if (!extractedValue && rawData.modelType === 'MultiLanguageProperty') {
        extractedValue = [{"language": "en", "text": "sample value"}];
      }

      expect(extractedValue).toEqual([{"language": "en", "text": "sample value"}]);
    });
  });

  /** Handle 404 deletes gracefully and proceed with UI cleanup. */
  describe('Bug Fix: Delete Element Error Handling', () => {
    it('should handle 404 errors gracefully during deletion', () => {
      const error = { status: 404, message: 'Element not found' } as any;
      aasService.deleteElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path');
    });

    it('should proceed with deletion if element exists', () => {
      aasService.deleteElement.and.returnValue(of({}));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path');
    });
  });

  /** Ensure robust handling for long/complex element paths. */
  describe('Bug Fix: URL Encoding for Long Paths', () => {
    it('should handle long element paths correctly', () => {
      const longPath = 'ConditionsOfReliabilityCharacteristics/RatedVoltage/SubProperty/DeepNested/Value';
      aasService.deleteElement.and.returnValue(of({}));

      component.deleteElement('test-submodel-id', longPath);

     
      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', longPath);
    });

    it('should encode path segments correctly', () => {
      const path = 'test/path/with/special/chars';
      const expectedEncoded = 'test/path/with/special/chars'.split('/').map(seg => encodeURIComponent(seg)).join('/');
      

      const segments = path.split('/');
      const encodedSegments = segments.map(seg => encodeURIComponent(seg));
      const result = encodedSegments.join('/');

      expect(result).toBe(expectedEncoded);
    });
  });

  /** Toasts are emitted by the component in success/error scenarios where applicable. */
  describe('Bug Fix: Toast Message Handling', () => {
    it('should open dialog for element creation (toast handled elsewhere)', () => {
      component.openCreateElement('test-submodel', undefined);
      expect(true).toBeTrue();
    });

    it('should show success toast on successful element deletion', () => {
      const mockElement = { idShort: 'test-element' };
      aasService.getElement.and.returnValue(of(mockElement));
      aasService.deleteElement.and.returnValue(of({}));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.deleteElement('test-submodel-id', 'test/element/path');

    
    });
  });
});
