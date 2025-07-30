import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { HttpResponse } from '@angular/common/http';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { CreateSourceSystemDTO } from '../../models/createSourceSystemDTO';

describe('CreateSourceSystemComponent', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let mockService: jasmine.SpyObj<SourceSystemResourceService>;

  // Mock-Daten für erfolgreiche Response
  const mockSourceSystemResponse: SourceSystemDTO = {
    id: 1,
    name: 'Test Source System',
    apiUrl: 'https://api.test.com',
    description: 'Test Description',
    apiType: 'REST_OPENAPI',
    openApiSpec: undefined
  };

  const mockCreateDTO: CreateSourceSystemDTO = {
    name: 'Test Source System',
    apiUrl: 'https://api.test.com',
    description: 'Test Description',
    apiType: 'REST_OPENAPI'
  };

  beforeEach(async () => {
    // Service Mock erstellen
    mockService = jasmine.createSpyObj('SourceSystemResourceService', [
      'apiConfigSourceSystemPost'
    ]);

    // Default Service-Response
    mockService.apiConfigSourceSystemPost.and.returnValue(
      of(mockSourceSystemResponse) // Direkt SourceSystemDTO zurückgeben
    );

    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        CreateSourceSystemComponent // Standalone Component
      ],
      providers: [
        { provide: SourceSystemResourceService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.form.get('name')?.value).toBe('');
    expect(component.form.get('apiUrl')?.value).toBe('');
    expect(component.form.get('description')?.value).toBe('');
    expect(component.form.get('apiType')?.value).toBe('REST_OPENAPI');
  });

  it('should validate required fields', () => {
    // Name ist required
    const nameControl = component.form.get('name');
    nameControl?.setValue('');
    nameControl?.markAsTouched();
    expect(nameControl?.hasError('required')).toBe(true);

    // API URL ist required
    const apiUrlControl = component.form.get('apiUrl');
    apiUrlControl?.setValue('');
    apiUrlControl?.markAsTouched();
    expect(apiUrlControl?.hasError('required')).toBe(true);
  });

  it('should create source system when form is valid', () => {
    // Form mit gültigen Daten füllen
    component.form.patchValue(mockCreateDTO);
    
    // KORRIGIERT: Verwende mockSourceSystemResponse statt HttpResponse
    mockService.apiConfigSourceSystemPost.and.returnValue(of(mockSourceSystemResponse));
    
    // Save aufrufen
    component.save();
    
    // Service sollte aufgerufen werden
    expect(mockService.apiConfigSourceSystemPost).toHaveBeenCalled();
  });

  it('should not save when form is invalid', () => {
    // Form ungültig lassen (leere required fields)
    component.form.patchValue({
      name: '', // Required field leer
      apiUrl: '',
      description: 'Test',
      apiType: 'REST_OPENAPI'
    });

    // Save aufrufen
    component.save();

    // Service sollte NICHT aufgerufen werden
    expect(mockService.apiConfigSourceSystemPost).not.toHaveBeenCalled();
  });

  it('should handle file selection', () => {
    const mockFile = new File(['test content'], 'test.json', { type: 'application/json' });
    const mockEvent = {
      files: [mockFile]
    };

    component.onFileSelected(mockEvent as any);

    expect(component.selectedFile).toBe(mockFile);
    expect(component.fileSelected).toBe(true);
  });

  it('should handle OpenAPI URL input', () => {
    const testUrl = 'https://api.example.com/openapi.json';
    
    component.form.get('openApiSpec')?.setValue(testUrl);
    
    expect(component.form.get('openApiSpec')?.value).toBe(testUrl);
  });

  it('should update form when sourceSystem input changes', () => {
    const testSourceSystem: SourceSystemDTO = {
      id: 1,
      name: 'Updated System',
      apiUrl: 'https://updated-api.com',
      description: 'Updated Description',
      apiType: 'REST_OPENAPI'
    };

    // Simuliere Input-Änderung
    component.sourceSystem = testSourceSystem;
    component.ngOnChanges({
      sourceSystem: {
        currentValue: testSourceSystem,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true
      }
    });

    // Form sollte aktualisiert werden
    expect(component.form.get('name')?.value).toBe('Updated System');
    expect(component.form.get('apiUrl')?.value).toBe('https://updated-api.com');
    expect(component.form.get('description')?.value).toBe('Updated Description');
    expect(component.form.get('apiType')?.value).toBe('REST_OPENAPI');
  });

  it('should reset current step when sourceSystem changes', () => {
    component.currentStep = 2; // Setze auf Step 2

    const testSourceSystem: SourceSystemDTO = {
      id: 1,
      name: 'Test System',
      apiUrl: 'https://api.test.com',
      apiType: 'REST_OPENAPI'
    };

    component.sourceSystem = testSourceSystem;
    component.ngOnChanges({
      sourceSystem: {
        currentValue: testSourceSystem,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true
      }
    });

    // Should reset to step 0
    expect(component.currentStep).toBe(0);
  });

  it('should handle save with file upload', () => {
    // Mock file
    const mockFile = new File(['{"openapi": "3.0.0"}'], 'openapi.json', { type: 'application/json' });
    component.selectedFile = mockFile;
    component.fileSelected = true;

    // Form mit Daten füllen
    component.form.patchValue({
      name: 'Test System',
      apiUrl: 'https://api.test.com',
      apiType: 'REST_OPENAPI'
    });

    // Mock FileReader
    spyOn(window, 'FileReader').and.returnValue({
      readAsText: jasmine.createSpy('readAsText').and.callFake(function(this: any) {
        this.result = '{"openapi": "3.0.0"}';
        this.onload();
      }),
      result: '{"openapi": "3.0.0"}'
    } as any);

    mockService.apiConfigSourceSystemPost.and.returnValue(of(mockSourceSystemResponse));

    component.save();

    // FileReader sollte verwendet werden
    expect(window.FileReader).toHaveBeenCalled();
  });
});