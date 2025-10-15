import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ResponsePreviewModalComponent } from './response-preview-modal.component';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';

describe('ResponsePreviewModalComponent', () => {
  let component: ResponsePreviewModalComponent;
  let fixture: ComponentFixture<ResponsePreviewModalComponent>;

  let svcSpy: jasmine.SpyObj<SourceSystemEndpointResourceService>;

  beforeEach(async () => {
    svcSpy = jasmine.createSpyObj('SourceSystemEndpointResourceService', ['generateTypeScript']);
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ResponsePreviewModalComponent],
      providers: [
        { provide: SourceSystemEndpointResourceService, useValue: svcSpy },
        provideNoopAnimations(),
        { provide: NGX_MONACO_EDITOR_CONFIG, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ResponsePreviewModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should update jsonEditorModel when responseBodySchema is set', () => {
    const validJson = '{"a":1}';
    component.responseBodySchema = validJson;
    component.ngOnChanges({} as any);
    expect(component.jsonEditorModel.value).toContain('"a"');
  });

  it('should expose jsonEditorOptions and typescriptEditorOptions', () => {
    expect(component.jsonEditorOptions.language).toBe('json');
    expect(component.typescriptEditorOptions.language).toBe('typescript');
  });

  it('should emit visibleChange and hide on onClose()', () => {
    component.visible = true;
    spyOn(component.visibleChange, 'emit');
    component.onClose();
    expect(component.visible).toBeFalse();
    expect(component.visibleChange.emit).toHaveBeenCalledWith(false);
  });

  it('should compute modalTitle from method and path', () => {
    component.httpMethod = 'GET';
    component.endpointPath = '/pets';
    expect(component.modalTitle).toBe('Response Body Schema - GET /pets');
  });

  it('hasResponseBody true when schema present or DTS present', () => {
    component.responseBodySchema = '';
    component.responseDts = '';
    expect(component.hasResponseBody).toBeFalse();

    component.responseBodySchema = '{"a":1}';
    expect(component.hasResponseBody).toBeTrue();

    component.responseBodySchema = '';
    component.responseDts = 'interface X {}';
    expect(component.hasResponseBody).toBeTrue();
  });

  it('switching to TS tab generates TypeScript when valid schema and endpointId provided', fakeAsync(() => {
    component.responseBodySchema = '{"type":"object","properties":{"id":{"type":"number"}}}';
    component.endpointId = 5;
    svcSpy.generateTypeScript.and.returnValue(of({ generatedTypeScript: 'interface ResponseBody { id: number; }' } as any));

    component.setActiveTab(1); // triggers onTabChange
    tick();

    expect(svcSpy.generateTypeScript).toHaveBeenCalled();
    expect(component.isGeneratingTypeScript).toBeFalse();
    expect(component.generatedTypeScript.length).toBeGreaterThan(0);
    expect(component.typescriptEditorModel.value.length).toBeGreaterThan(0);
  }));

  it('does not call generation when missing schema or endpointId', () => {
    component.responseBodySchema = null as any;
    component.endpointId = 5;
    component.setActiveTab(1);
    expect(component.typescriptError).toContain('missing schema');
    expect(svcSpy.generateTypeScript).not.toHaveBeenCalled();

    component.responseBodySchema = '{"a":1}';
    component.endpointId = null as any;
    component.setActiveTab(1);
    expect(component.typescriptError).toContain('missing schema');
    expect(svcSpy.generateTypeScript).not.toHaveBeenCalled();
  });

  it('on invalid JSON shows validation error and skips backend', () => {
    component.responseBodySchema = '{ invalid json';
    component.endpointId = 1;
    component.setActiveTab(1);
    expect(component.typescriptError).toContain('Invalid JSON schema');
    expect(svcSpy.generateTypeScript).not.toHaveBeenCalled();
  });

  it('respects provided responseDts and sets TS tab without calling backend', fakeAsync(() => {
    component.responseDts = 'export interface ResponseBody {}';
    component.responseBodySchema = '{"a":1}';
    component.endpointId = 1;
    component.setActiveTab(1);
    tick();
    expect(component.typescriptEditorModel.value).toContain('interface ResponseBody');
    expect(svcSpy.generateTypeScript).not.toHaveBeenCalled();
  }));
}); 