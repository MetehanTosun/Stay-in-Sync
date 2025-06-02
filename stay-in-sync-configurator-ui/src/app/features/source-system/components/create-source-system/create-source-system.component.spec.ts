import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of } from 'rxjs';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import 'zone.js'
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';

describe('CreateSourceSystemComponent - REST_SAMPLE', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let aasService: jasmine.SpyObj<AasService>;

  beforeEach(async () => {
    const aasServiceSpy = jasmine.createSpyObj('AasService', ['getAll']);

    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        FormsModule,
        HttpClientTestingModule,
        ButtonModule,
        DialogModule,
        DropdownModule,
        InputTextModule,
        InputTextarea
      ],
      providers: [
        { provide: AasService, useValue: aasServiceSpy },
        provideAnimationsAsync() // HINZUGEFÜGT: Löst Zone.js Problem
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    aasService = TestBed.inject(AasService) as jasmine.SpyObj<AasService>;
    
    aasService.getAll.and.returnValue(of([]));
    fixture.detectChanges();
  });

  // ENTFERNT: afterEach mit httpMock.verify() da wir HttpTestingController nicht verwenden

  describe('REST_SAMPLE functionality', () => {
    beforeEach(() => {
      component.sourceType = 'REST_SAMPLE';
    });

    it('should initialize REST_SAMPLE with default values', () => {
      expect(component.restSampleBaseUrl).toBe('');
      expect(component.restSampleQueryParams).toEqual([{ key: '', value: '' }]);
      expect(component.restSampleHeaders).toEqual([{ key: '', value: '' }]);
      expect(component.restSampleRequestBody).toBe('');
      expect(component.inferredSchema).toBeNull();
    });

    it('should add query parameter', () => {
      const initialLength = component.restSampleQueryParams.length;
      
      component.addQueryParam();
      
      expect(component.restSampleQueryParams.length).toBe(initialLength + 1);
      expect(component.restSampleQueryParams[component.restSampleQueryParams.length - 1]).toEqual({ key: '', value: '' });
    });

    it('should remove query parameter', () => {
      component.restSampleQueryParams = [
        { key: 'param1', value: 'value1' },
        { key: 'param2', value: 'value2' }
      ];
      
      component.removeQueryParam(0);
      
      expect(component.restSampleQueryParams.length).toBe(1);
      expect(component.restSampleQueryParams[0]).toEqual({ key: 'param2', value: 'value2' });
    });

    it('should not remove last query parameter', () => {
      component.restSampleQueryParams = [{ key: 'param1', value: 'value1' }];
      
      component.removeQueryParam(0);
      
      expect(component.restSampleQueryParams.length).toBe(1);
    });

    it('should add header', () => {
      const initialLength = component.restSampleHeaders.length;
      
      component.addHeader();
      
      expect(component.restSampleHeaders.length).toBe(initialLength + 1);
      expect(component.restSampleHeaders[component.restSampleHeaders.length - 1]).toEqual({ key: '', value: '' });
    });

    it('should build sample URL correctly', () => {
      component.restSampleBaseUrl = 'https://api.example.com/data';
      component.restSampleQueryParams = [
        { key: 'userId', value: '1' },
        { key: 'limit', value: '10' },
        { key: '', value: '' } // Sollte gefiltert werden
      ];
      
      const url = component['buildSampleUrl']();
      
      expect(url).toBe('https://api.example.com/data?userId=1&limit=10');
    });

    it('should build sample URL without query params', () => {
      component.restSampleBaseUrl = 'https://api.example.com/data';
      component.restSampleQueryParams = [{ key: '', value: '' }];
      
      const url = component['buildSampleUrl']();
      
      expect(url).toBe('https://api.example.com/data');
    });

    it('should infer JSON schema from object', () => {
      const testData = {
        id: 1,
        name: 'test',
        active: true
      };
      
      const schema = component['inferJsonSchema'](testData);
      
      expect(schema.type).toBe('object');
      expect(schema.properties.id.type).toBe('number');
      expect(schema.properties.name.type).toBe('string');
      expect(schema.properties.active.type).toBe('boolean');
    });

    it('should infer JSON schema from array', () => {
      const testData = [
        { id: 1, name: 'test1' },
        { id: 2, name: 'test2' }
      ];
      
      const schema = component['inferJsonSchema'](testData);
      
      expect(schema.type).toBe('array');
      expect(schema.items.type).toBe('object');
      expect(schema.items.properties.id.type).toBe('number');
      expect(schema.items.properties.name.type).toBe('string');
    });

    it('should show error when base URL is missing for schema inference', async () => {
      component.restSampleBaseUrl = '';
      
      await component.inferSchemaFromSample();
      
      expect(component.schemaInferenceError).toBe('Please enter a base URL.');
    });

    it('should save REST_SAMPLE configuration correctly', () => {
      // Setup
      component.source.name = 'Test API';
      component.restSampleBaseUrl = 'https://api.example.com/data';
      component.restSampleQueryParams = [
        { key: 'userId', value: '1' },
        { key: '', value: '' } // Sollte gefiltert werden
      ];
      component.restSampleHeaders = [
        { key: 'Authorization', value: 'Bearer token' }
      ];
      component.restSampleRequestBody = '{"test": "data"}';
      component.inferredSchema = { type: 'object', properties: {} };
      
      spyOn(component.sourceSaved, 'emit');
      
      // Action
      component.save();
      
      // Assert
      expect(component.sourceSaved.emit).toHaveBeenCalledWith({
        name: 'Test API',
        type: 'REST_SAMPLE',
        baseUrl: 'https://api.example.com/data',
        sampleConfig: {
          queryParams: [{ key: 'userId', value: '1' }],
          headers: [{ key: 'Authorization', value: 'Bearer token' }],
          requestBody: '{"test": "data"}'
        },
        inferredSchema: { type: 'object', properties: {} }
      });
    });

    it('should not save without inferred schema', () => {
      component.source.name = 'Test API';
      component.restSampleBaseUrl = 'https://api.example.com/data';
      component.inferredSchema = null;
      
      spyOn(component.sourceSaved, 'emit');
      
      component.save();
      
      expect(component.sourceSaved.emit).not.toHaveBeenCalled();
      expect(component.schemaInferenceError).toBe('Schema must be inferred before saving.');
    });
  });
});