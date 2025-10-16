import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';
import { TargetResponsePreviewModalComponent } from './response-preview-modal.component';
import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';

/**
 * Unit tests for {@link TargetResponsePreviewModalComponent}.
 * Verifies functionality related to schema previewing, TypeScript generation,
 * tab switching, error handling, and modal closing behavior.
 */
describe('TargetResponsePreviewModalComponent', () => {
  let component: TargetResponsePreviewModalComponent;
  let fixture: ComponentFixture<TargetResponsePreviewModalComponent>;
  let svcSpy: jasmine.SpyObj<TargetSystemEndpointResourceService>;

  /**
   * Sets up the testing environment for the TargetResponsePreviewModalComponent.
   * Initializes spies, testing modules, and component fixture before each test.
   */
  beforeEach(async () => {
    svcSpy = jasmine.createSpyObj('TargetSystemEndpointResourceService', ['generateTypeScript']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, TargetResponsePreviewModalComponent],
      providers: [
        provideNoopAnimations(),
        { provide: NGX_MONACO_EDITOR_CONFIG, useValue: {} },
        { provide: TargetSystemEndpointResourceService, useValue: svcSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TargetResponsePreviewModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  /**
   * Verifies that the component is successfully created and initialized.
   */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * Tests whether editor models are correctly updated when input properties change.
   */
  it('should update editor models when inputs set', () => {
    component.responseBodySchema = '{"a":1}';
    component.ngOnChanges({} as any);
    expect(component.jsonEditorModel.value).toContain('"a"');
  });

  /**
   * Tests TypeScript generation when switching to tab index 1
   * and both schema and endpoint ID are provided.
   */
  it('setActiveTab 1 triggers generation when schema and id provided', fakeAsync(() => {
    component.responseBodySchema = '{"type":"object"}';
    component.endpointId = 5;
    svcSpy.generateTypeScript.and.returnValue(of({ generatedTypeScript: 'interface X {}' } as any));

    component.setActiveTab(1);
    tick();

    expect(svcSpy.generateTypeScript).toHaveBeenCalled();
    expect(component.generatedTypeScript.length).toBeGreaterThan(0);
  }));

  /**
   * Verifies that when a pre-generated DTS is available,
   * switching tabs directly uses the provided DTS instead of re-generating it.
   */
  it('onTabChange with DTS present uses DTS directly', () => {
    component.responseDts = 'interface Pre {}';
    component.onTabChange({ index: 1 });
    expect(component.typescriptEditorModel.value).toContain('interface Pre');
  });

  /**
   * Ensures that a missing schema or endpoint ID triggers a proper warning message.
   */
  it('loadTypeScript should warn when missing schema or id', () => {
    component.responseBodySchema = null as any;
    component.endpointId = 1;
    component.loadTypeScript();
    expect(component.typescriptError).toContain('missing schema');
  });

  /**
   * Verifies error handling when the provided JSON schema is invalid.
   */
  it('loadTypeScript should handle invalid JSON schema', () => {
    component.responseBodySchema = '{ invalid';
    component.endpointId = 1;
    component.loadTypeScript();
    expect(component.typescriptError).toContain('Invalid JSON schema');
  });

  /**
   * Ensures that a backend error during TypeScript generation
   * results in fallback DTS content and a clear error message.
   */
  it('loadTypeScript should set fallback on backend error', fakeAsync(() => {
    component.responseBodySchema = '{"type":"object"}';
    component.endpointId = 1;
    svcSpy.generateTypeScript.and.returnValue(throwError(() => new Error('fail')));

    component.setActiveTab(1);
    tick();

    expect(component.generatedTypeScript).toContain('Fallback');
    expect(component.typescriptError).toContain('failed');
  }));

  /**
   * Tests the modal close behavior by verifying that visibility is set to false,
   * the event is emitted, and pending timeouts are cleared.
   */
  it('onClose should emit visibleChange false and clear timeout', () => {
    component.visible = true;
    spyOn(component.visibleChange, 'emit');
    (component as any)['typescriptGenerationTimeout'] = setTimeout(() => {}, 10000);
    component.onClose();
    expect(component.visible).toBeFalse();
    expect(component.visibleChange.emit).toHaveBeenCalledWith(false);
    expect((component as any)['typescriptGenerationTimeout']).toBeNull();
  });

  /**
   * Verifies that the modal title dynamically includes the HTTP method and endpoint path.
   */
  it('modalTitle should include method and path', () => {
    component.httpMethod = 'GET';
    component.endpointPath = '/pets';
    expect(component.modalTitle).toBe('Response Body Schema - GET /pets');
  });
});
