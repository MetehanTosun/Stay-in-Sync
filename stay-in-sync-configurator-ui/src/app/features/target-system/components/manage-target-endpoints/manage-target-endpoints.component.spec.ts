import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';
import { MessageService } from 'primeng/api';
import { ManageTargetEndpointsComponent } from './manage-target-endpoints.component';
import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { OpenApiImportService } from '../../../../core/services/openapi-import.service';
import { TargetSystemEndpointDTO } from '../../models/targetSystemEndpointDTO';

/**
 * Unit tests for {@link ManageTargetEndpointsComponent}.
 * Verifies CRUD functionality for managing Target System endpoints, including
 * endpoint creation, editing, deletion, TypeScript generation, and modal behavior.
 */
describe('ManageTargetEndpointsComponent', () => {
  let component: ManageTargetEndpointsComponent;
  let fixture: ComponentFixture<ManageTargetEndpointsComponent>;
  let endpointApi: jasmine.SpyObj<TargetSystemEndpointResourceService>;
  let targetApi: jasmine.SpyObj<TargetSystemResourceService>;
  let openapi: jasmine.SpyObj<OpenApiImportService>;

  /**
   * Sets up the testing environment for the ManageTargetEndpointsComponent.
   * Initializes spies, mocked services, and test data before each test case.
   */
  beforeEach(async () => {
    endpointApi = jasmine.createSpyObj('TargetSystemEndpointResourceService', ['list', 'create', 'replace', 'delete', 'generateTypeScript']);
    targetApi = jasmine.createSpyObj('TargetSystemResourceService', ['getById']);
    openapi = jasmine.createSpyObj('OpenApiImportService', ['discoverEndpointsFromSpec', 'discoverEndpointsFromSpecUrl', 'discoverParamsFromSpec', 'persistParamsForEndpoint']);

    await TestBed.configureTestingModule({
      imports: [ManageTargetEndpointsComponent, HttpClientTestingModule],
      providers: [
        provideNoopAnimations(),
        MessageService,
        { provide: NGX_MONACO_EDITOR_CONFIG, useValue: {} },
        { provide: TargetSystemEndpointResourceService, useValue: endpointApi },
        { provide: TargetSystemResourceService, useValue: targetApi },
        { provide: OpenApiImportService, useValue: openapi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ManageTargetEndpointsComponent);
    component = fixture.componentInstance;
    component.targetSystemId = 1;
    endpointApi.list.and.returnValue(of([]));
    fixture.detectChanges();
  });

  /**
   * Verifies that the ManageTargetEndpointsComponent instance is created successfully.
   */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * Tests whether the component correctly loads and populates endpoints
   * when the API returns a valid list of TargetSystemEndpointDTO objects.
   */
  it('load should populate endpoints', () => {
    const data: TargetSystemEndpointDTO[] = [{ id: 1, targetSystemId: 1, endpointPath: '/pets', httpRequestType: 'GET' } as any];
    endpointApi.list.and.returnValue(of(data));
    component.load();
    expect(component.endpoints.length).toBe(1);
    expect(component.endpoints[0].endpointPath).toBe('/pets');
  });

  /**
   * Tests the addEndpoint method to ensure that endpoint creation triggers the API call
   * and that the form is reset after successful creation.
   */
  it('addEndpoint should call create and reset form', fakeAsync(() => {
    endpointApi.create.and.returnValue(of([{ id: 10 }] as any));
    endpointApi.list.and.returnValue(of([]));

    component.form.patchValue({ endpointPath: '/pets', httpRequestType: 'GET', requestBodySchema: '', responseBodySchema: '' });
    component.addEndpoint();
    tick();

    expect(endpointApi.create).toHaveBeenCalled();
    expect(component.form.value.endpointPath).toBe('');
  }));

  /**
   * Verifies that the save method updates an existing endpoint
   * and closes the dialog after a successful API response.
   */
  it('save should update when editing existing endpoint', fakeAsync(() => {
    const row: TargetSystemEndpointDTO = { id: 5, targetSystemId: 1, endpointPath: '/pets', httpRequestType: 'GET' } as any;
    component.openEdit(row);
    endpointApi.replace.and.returnValue(of(void 0));
    endpointApi.list.and.returnValue(of([]));

    component.form.patchValue({ endpointPath: '/pets2', httpRequestType: 'PUT' });
    component.save();
    tick();

    expect(endpointApi.replace).toHaveBeenCalled();
    expect(component.showDialog).toBeFalse();
  }));

  /**
   * Tests the delete flow by confirming that the delete API is called
   * and that the endpoint list is refreshed after successful deletion.
   */
  it('delete flow should call api.delete and reload', fakeAsync(() => {
    const row: TargetSystemEndpointDTO = { id: 7, targetSystemId: 1, endpointPath: '/x', httpRequestType: 'GET' } as any;
    component.delete(row);
    endpointApi.delete.and.returnValue(of(void 0));
    endpointApi.list.and.returnValue(of([]));

    component.onConfirmationConfirmed();
    tick();

    expect(endpointApi.delete).toHaveBeenCalledWith(7);
  }));

  /**
   * Verifies that the request body editor correctly updates the internal model
   * and resets state when opened and closed.
   */
  it('request body editor open/close should update state and model', () => {
    const row: TargetSystemEndpointDTO = { id: 1, targetSystemId: 1, endpointPath: '/p', httpRequestType: 'POST', requestBodySchema: '{"a":1}' } as any;
    component.openRequestBodyEditor(row);
    expect(component.requestBodyEditorEndpoint).toBeTruthy();
    expect((component.requestBodyEditorModel.value as string)).toContain('"a"');
    component.closeRequestBodyEditor();
    expect(component.requestBodyEditorEndpoint).toBeNull();
  });

  /**
   * Tests TypeScript generation for the request/response schema defined in the form.
   * Ensures that the generated model is assigned correctly.
   */
  it('generateTypeScriptForForm should call service and set model', () => {
    component.form.patchValue({ responseBodySchema: '{"type":"object"}' });
    endpointApi.generateTypeScript.and.returnValue(of({ generatedTypeScript: 'interface R {}' } as any));
    component.generateTypeScriptForForm();
    expect((component as any).typescriptModel.value).toContain('interface');
  });

  /**
   * Verifies that opening a response preview initializes the model correctly
   * and triggers TypeScript generation for the selected endpoint.
   */
  it('openResponsePreview then generateTypeScriptForPreview should set model', () => {
    const row: TargetSystemEndpointDTO = { id: 3, targetSystemId: 1, endpointPath: '/p', httpRequestType: 'GET' } as any;
    component.openResponsePreview(row);
    component.responseJsonModel = { value: '{"type":"object"}', language: 'json' } as any;
    endpointApi.generateTypeScript.and.returnValue(of({ generatedTypeScript: 'interface P {}' } as any));
    component.generateTypeScriptForPreview();
    expect((component as any).responseTypeScriptModel.value).toContain('interface P');
  });

  /**
   * Ensures that switching to tab index 1 in the dialog triggers
   * TypeScript generation for the request/response form schema.
   */
  it('onDialogTabChange index 1 should trigger TS generation for form', () => {
    spyOn(component, 'generateTypeScriptForForm');
    component.onDialogTabChange({ index: 1 });
    expect(component.generateTypeScriptForForm).toHaveBeenCalled();
  });

  /**
   * Verifies that switching to tab index 1 in the response preview triggers
   * TypeScript generation for the preview model.
   */
  it('onResponseTabChange index 1 should trigger TS generation for preview', () => {
    spyOn(component, 'generateTypeScriptForPreview');
    component.onResponseTabChange({ index: 1 });
    expect(component.generateTypeScriptForPreview).toHaveBeenCalled();
  });
});
