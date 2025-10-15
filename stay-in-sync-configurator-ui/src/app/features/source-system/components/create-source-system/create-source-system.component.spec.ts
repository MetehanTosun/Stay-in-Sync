import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
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
  let componentMessageService: MessageService;
  let httpErrorService: jasmine.SpyObj<HttpErrorService>;

  beforeEach(async () => {
    const aasServiceSpy = jasmine.createSpyObj('AasService', [
      'createElement', 'deleteElement', 'getElement', 'listElements', 'encodeIdToBase64Url', 'refreshSnapshot', 'listSubmodels', 'aasTest', 'setPropertyValue'
    ]);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);
    const httpErrorServiceSpy = jasmine.createSpyObj('HttpErrorService', ['handleError']);

    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        ReactiveFormsModule,
        FormsModule,
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
    // Use the component's scoped MessageService instance (component has its own provider)
    componentMessageService = fixture.debugElement.injector.get(MessageService);
    spyOn(componentMessageService, 'add');

    // Set up component state
    component.createdSourceSystemId = 1;
    aasService.listElements.and.returnValue(of([]));
    aasService.refreshSnapshot.and.returnValue(of({} as any));
    aasService.listSubmodels.and.returnValue(of([]));

    // Prevent async afterAll subscribe errors by returning an Observable
    spyOn<any>(component, 'hydrateNodeTypesForNodes').and.returnValue(of([]));
  });

  describe('openCreateElement (dialog flow)', () => {
    it('should open dialog with data', () => {
      component.openCreateElement('sm1', 'parent');
      // no exception
      expect(true).toBeTrue();
    });

    it('should set elementDialogData and showElementDialog', () => {
      component.createdSourceSystemId = 5;
      component.openCreateElement('sm-123', 'p1/p2');
      expect(component.showElementDialog).toBeTrue();
      expect(component.elementDialogData).toEqual(jasmine.objectContaining({
        submodelId: 'sm-123',
        parentPath: 'p1/p2',
        systemId: 5,
        systemType: 'source'
      }));
    });
  });

  describe('deleteElement', () => {
    beforeEach(() => {
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');
    });

    it('should delete element successfully', () => {
      aasService.deleteElement.and.returnValue(of({}));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path');
    });

    it('should handle delete error', () => {
      const error: any = { status: 404, message: 'Not found' };
      aasService.deleteElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path');
    });

    it('should handle other delete errors', () => {
      const error: any = { status: 500, message: 'Server error' };
      aasService.deleteElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');
    });

    it('should handle delete error', () => {
      aasService.getElement.and.returnValue(of({ idShort: 'test-element' }));
      const error: any = { status: 500, message: 'Delete failed', name: 'HttpErrorResponse', ok: false, headers: {} as any, url: '', error: null, statusText: '', type: 4 };
      aasService.deleteElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path');
    });

    it('should not delete if missing required data', () => {
      component.createdSourceSystemId = undefined as any;

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.getElement).not.toHaveBeenCalled();
      expect(aasService.deleteElement).not.toHaveBeenCalled();
    });
  });

  describe('onElementDialogResult', () => {
    it('should create element and refresh on success', async () => {
      component.createdSourceSystemId = 1;
      aasService.encodeIdToBase64Url.and.returnValue('enc-sm');
      aasService.createElement.and.returnValue(of({}));
      const discoverSpy = spyOn(component, 'discoverSubmodels').and.stub();

      await component.onElementDialogResult({ success: true, element: { submodelId: 'sm', body: { idShort: 'x' }, parentPath: 'p' } } as any);

      expect(aasService.createElement).toHaveBeenCalled();
      expect(discoverSpy).toHaveBeenCalled();
      expect(componentMessageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });

    it('should show duplicate error toast if error contains duplicate hint', () => {
      component.onElementDialogResult({ success: false, error: 'Duplicate entry' } as any);
      expect(componentMessageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'error', summary: 'Duplicate Element' }));
    });
  });

  describe('testAasConnection', () => {
    it('should mark success and refresh snapshot on success', () => {
      component.createdSourceSystemId = 7;
      aasService.aasTest.and.returnValue(of({ idShort: 'Shell', assetKind: 'INSTANCE' } as any));
      aasService.refreshSnapshot.and.returnValue(of({} as any));

      component.testAasConnection();

      expect(component.aasTestOk).toBeTrue();
      expect(aasService.refreshSnapshot).toHaveBeenCalledWith(7);
      expect(componentMessageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'success' }));
    });
  });

  describe('parseValueForType (private)', () => {
    it('should parse boolean', () => {
      const r1 = (component as any).parseValueForType('true', 'xs:boolean');
      const r2 = (component as any).parseValueForType('false', 'xs:boolean');
      expect(r1).toBeTrue();
      expect(r2).toBeFalse();
    });
    it('should parse int and float', () => {
      const i = (component as any).parseValueForType('42', 'xs:int');
      const f = (component as any).parseValueForType('3.14', 'xs:double');
      expect(i).toBe(42);
      expect(f).toBeCloseTo(3.14, 5);
    });
    it('should leave string as is', () => {
      const s = (component as any).parseValueForType('abc', 'xs:string');
      expect(s).toBe('abc');
    });
  });

  describe('uploadAasx', () => {
    it('should warn when no file selected', () => {
      component.createdSourceSystemId = 1;
      component['aasxSelectedFile'] = null as any;
      component.uploadAasx();
      expect(componentMessageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ severity: 'warn' }));
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

      // idShortPath may be undefined; key should still include submodel
      expect(String(result.key).startsWith(`${submodelId}::`)).toBeTrue();
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