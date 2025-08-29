import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { ResponsePreviewModalComponent } from './response-preview-modal.component';

describe('ResponsePreviewModalComponent', () => {
  let component: ResponsePreviewModalComponent;
  let fixture: ComponentFixture<ResponsePreviewModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ResponsePreviewModalComponent,
        MonacoEditorModule,
        DialogModule,
        ButtonModule,
        ProgressSpinnerModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ResponsePreviewModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Input Properties', () => {
    it('should accept visible input', () => {
      component.visible = true;
      expect(component.visible).toBe(true);
    });

    it('should accept endpointId input', () => {
      component.endpointId = 123;
      expect(component.endpointId).toBe(123);
    });

    it('should accept endpointPath input', () => {
      component.endpointPath = '/api/test';
      expect(component.endpointPath).toBe('/api/test');
    });

    it('should accept httpMethod input', () => {
      component.httpMethod = 'POST';
      expect(component.httpMethod).toBe('POST');
    });

    it('should accept responseBodySchema input', () => {
      const schema = '{"type": "object", "properties": {"id": {"type": "integer"}}}';
      component.responseBodySchema = schema;
      expect(component.responseBodySchema).toBe(schema);
    });
  });

  describe('Modal Title Generation', () => {
    it('should generate correct modal title', () => {
      component.endpointPath = '/api/users';
      component.httpMethod = 'GET';
      
      expect(component.modalTitle).toBe('Response Preview - GET /api/users');
    });

    it('should handle empty endpoint path', () => {
      component.endpointPath = '';
      component.httpMethod = 'POST';
      
      expect(component.modalTitle).toBe('Response Preview - POST ');
    });
  });

  describe('Response Body Detection', () => {
    it('should detect valid JSON response body', () => {
      component.responseBodySchema = '{"type": "object"}';
      expect(component.hasResponseBody).toBe(true);
    });

    it('should detect invalid JSON response body', () => {
      component.responseBodySchema = '{invalid json}';
      expect(component.hasResponseBody).toBe(false);
    });

    it('should handle null response body', () => {
      component.responseBodySchema = null;
      expect(component.hasResponseBody).toBe(false);
    });

    it('should handle undefined response body', () => {
      component.responseBodySchema = undefined;
      expect(component.hasResponseBody).toBe(false);
    });

    it('should handle empty response body', () => {
      component.responseBodySchema = '';
      expect(component.hasResponseBody).toBe(false);
    });
  });

  describe('Editor Model Updates', () => {
    it('should update editor model with valid JSON', () => {
      const validJson = '{"type": "object", "properties": {"id": {"type": "integer"}}}';
      component.responseBodySchema = validJson;
      
      component.ngOnInit();
      
      expect(component.editorModel.value).toBe(JSON.stringify(JSON.parse(validJson), null, 2));
      expect(component.editorModel.language).toBe('json');
      expect(component.error).toBeNull();
    });

    it('should handle invalid JSON gracefully', () => {
      component.responseBodySchema = '{invalid json}';
      
      component.ngOnInit();
      
      expect(component.editorModel.value).toBe('{invalid json}');
      expect(component.editorModel.language).toBe('json');
      expect(component.error).toBe('Response body schema is not valid JSON');
    });

    it('should handle null response body', () => {
      component.responseBodySchema = null;
      
      component.ngOnInit();
      
      expect(component.editorModel.value).toBe('// No response body schema available for this endpoint');
      expect(component.editorModel.language).toBe('json');
      expect(component.error).toBeNull();
    });

    it('should handle empty response body', () => {
      component.responseBodySchema = '';
      
      component.ngOnInit();
      
      expect(component.editorModel.value).toBe('// No response body schema available for this endpoint');
      expect(component.editorModel.language).toBe('json');
      expect(component.error).toBeNull();
    });

    it('should handle undefined response body', () => {
      component.responseBodySchema = undefined;
      
      component.ngOnInit();
      
      expect(component.editorModel.value).toBe('// No response body schema available for this endpoint');
      expect(component.editorModel.language).toBe('json');
      expect(component.error).toBeNull();
    });
  });

  describe('Modal Visibility', () => {
    it('should emit visible change when closing modal', () => {
      const visibleChangeSpy = jasmine.createSpy('visibleChange');
      component.visibleChange.subscribe(visible => {
        visibleChangeSpy(visible);
      });
      component.visible = true;

      component.onClose();

      expect(visibleChangeSpy).toHaveBeenCalledWith(false);
    });
  });

  describe('Editor Options', () => {
    it('should have correct editor options', () => {
      expect(component.editorOptions).toEqual({
        theme: 'vs-dark',
        language: 'json',
        readOnly: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        automaticLayout: true
      });
    });
  });

  describe('Change Detection', () => {
    it('should update editor model when responseBodySchema changes', () => {
      const initialSchema = '{"type": "object"}';
      const newSchema = '{"type": "array", "items": {"type": "string"}}';
      
      component.responseBodySchema = initialSchema;
      component.ngOnInit();
      const initialValue = component.editorModel.value;
      
      component.responseBodySchema = newSchema;
      component.ngOnInit();
      const newValue = component.editorModel.value;
      
      expect(newValue).not.toBe(initialValue);
      expect(newValue).toContain('"type": "array"');
    });
  });
}); 