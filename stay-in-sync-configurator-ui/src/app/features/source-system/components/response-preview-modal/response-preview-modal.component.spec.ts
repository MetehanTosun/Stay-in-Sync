/**
 * Unit tests for `ResponsePreviewModalComponent` covering:
 * - Component creation and basic bindings
 * - JSON editor model updates and editor options exposure
 * - Modal visibility interactions and derived title
 * - Presence checks for response body via schema or DTS
 * - TypeScript tab behavior including generation preconditions and success/failure paths
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ResponsePreviewModalComponent } from './response-preview-modal.component';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';

/** Test suite for response preview modal behavior and editor interactions. */
describe('ResponsePreviewModalComponent', () => {
  let component: ResponsePreviewModalComponent;
  let fixture: ComponentFixture<ResponsePreviewModalComponent>;

  /** Spy for backend TypeScript generation calls. */
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

  /** Verifies testbed creates the component successfully. */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /** Sets a valid JSON schema and ensures the JSON editor model reflects it. */
  it('should update jsonEditorModel when responseBodySchema is set', () => {
    const validJson = '{"a":1}';
    component.responseBodySchema = validJson;
    component.ngOnChanges({} as any);
    expect(component.jsonEditorModel.value).toContain('"a"');
  });

  /** Exposes editor options for JSON and TypeScript editors. */
  it('should expose jsonEditorOptions and typescriptEditorOptions', () => {
    expect(component.jsonEditorOptions.language).toBe('json');
    expect(component.typescriptEditorOptions.language).toBe('typescript');
  });

  /** Closing the modal should emit and update visibility state. */
  it('should emit visibleChange and hide on onClose()', () => {
    component.visible = true;
    spyOn(component.visibleChange, 'emit');
    component.onClose();
    expect(component.visible).toBeFalse();
    expect(component.visibleChange.emit).toHaveBeenCalledWith(false);
  });

  /** Computes title from HTTP method and endpoint path. */
  it('should compute modalTitle from method and path', () => {
    component.httpMethod = 'GET';
    component.endpointPath = '/pets';
    expect(component.modalTitle).toBe('Response Body Schema - GET /pets');
  });

  /** True when either JSON schema or pre-provided DTS exists. */
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

  /** Switching to TS tab with valid inputs triggers generation and updates models. */
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

  /** Missing schema or endpoint ID prevents generation and shows error. */
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

  /** Invalid JSON is validated locally and backend is not called. */
  it('on invalid JSON shows validation error and skips backend', () => {
    component.responseBodySchema = '{ invalid json';
    component.endpointId = 1;
    component.setActiveTab(1);
    expect(component.typescriptError).toContain('Invalid JSON schema');
    expect(svcSpy.generateTypeScript).not.toHaveBeenCalled();
  });

  /** Uses provided DTS if present and avoids backend generation. */
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