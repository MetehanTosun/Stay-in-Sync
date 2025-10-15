import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';

describe('CreateSourceSystemComponent - Simple Tests', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let aasService: jasmine.SpyObj<AasService>;
  let messageService: jasmine.SpyObj<MessageService>;

  beforeEach(async () => {
    const aasServiceSpy = jasmine.createSpyObj('AasService', [
      'createElement', 'deleteElement', 'getElement', 'listElements', 'encodeIdToBase64Url'
    ]);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        HttpClientTestingModule
      ],
      providers: [
        { provide: AasService, useValue: aasServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: HttpErrorService, useValue: jasmine.createSpyObj('HttpErrorService', ['handleError']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    aasService = TestBed.inject(AasService) as jasmine.SpyObj<AasService>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;

    component.createdSourceSystemId = 1;
  });

  describe('Element Creation', () => {
    it('should create element successfully', () => {
      aasService.createElement.and.returnValue(of({}));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');
      component.targetSubmodelId = 'test-submodel';
      component.newElementJson = JSON.stringify({ idShort: 'test', modelType: 'Property' });

      component.createElement();

      expect(aasService.createElement).toHaveBeenCalled();
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
    });
  });

  describe('Element Deletion', () => {
    it('should delete element successfully', () => {
      aasService.getElement.and.returnValue(of({ idShort: 'test-element' }));
      aasService.deleteElement.and.returnValue(of({}));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.getElement).toHaveBeenCalled();
      expect(aasService.deleteElement).toHaveBeenCalled();
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Element Deleted',
        detail: 'Element has been successfully deleted.',
        life: 3000
      });
    });

    it('should handle element not found (404)', () => {
      const error = { status: 404, message: 'Not found' };
      aasService.getElement.and.returnValue(throwError(() => error));
      aasService.encodeIdToBase64Url.and.returnValue('encoded-id');

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.getElement).toHaveBeenCalled();
      expect(aasService.deleteElement).not.toHaveBeenCalled();
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'warn',
        summary: 'Element Not Found',
        detail: 'Element does not exist or has already been deleted.',
        life: 3000
      });
    });
  });

  describe('Type Inference', () => {
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

    it('should detect File type', () => {
      const element = { 
        contentType: 'text/plain',
        fileName: 'test.txt'
      };
      const result = component['inferModelType'](element);
      expect(result).toBe('File');
    });
  });

  describe('Tree Node Mapping', () => {
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
  });

  describe('URL Encoding', () => {
    it('should encode submodel ID correctly', () => {
      const id = 'https://admin-shell.io/idta/SubmodelTemplate/CarbonFootprint/0/9';
      const encoded = aasService.encodeIdToBase64Url(id);
      
      expect(encoded).toBeDefined();
      expect(encoded).not.toBe(id);
    });
  });
});
