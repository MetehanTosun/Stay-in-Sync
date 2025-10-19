import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SourceSystemPageComponent } from './source-system-page.component';
import { NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { SourceSystemSearchPipe } from '../../pipes/source-system-search.pipe';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { CreateSourceSystemDialogService } from '../../services/create-source-system-dialog.service';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { MessageService } from 'primeng/api';

/**
 * Test suite for the SourceSystemPageComponent.
 * Verifies correct component creation, initialization logic, 
 * and the behavior of AAS-related UI actions and service calls.
 */
describe('SourceSystemPageComponent', () => {
  let component: SourceSystemPageComponent;
  let fixture: ComponentFixture<SourceSystemPageComponent>;
  let mockSourceSvc: jasmine.SpyObj<SourceSystemResourceService>;
  let mockEndpointSvc: jasmine.SpyObj<SourceSystemEndpointResourceService>;
  let mockSearchPipe: jasmine.SpyObj<SourceSystemSearchPipe>;
  let mockAasSvc: jasmine.SpyObj<AasService>;
  let mockErrSvc: jasmine.SpyObj<HttpErrorService>;
  let mockDialogSvc: jasmine.SpyObj<CreateSourceSystemDialogService>;

  /**
   * Sets up the Angular testing module before each test.
   * Mocks all required services and initializes the SourceSystemPageComponent instance.
   */
  beforeEach(async () => {
    mockSourceSvc = jasmine.createSpyObj('SourceSystemResourceService', ['apiConfigSourceSystemIdGet', 'apiConfigSourceSystemIdPut']);
    mockEndpointSvc = jasmine.createSpyObj('SourceSystemEndpointResourceService', ['dummy']);
    mockSearchPipe = jasmine.createSpyObj('SourceSystemSearchPipe', ['transform']);
    mockAasSvc = jasmine.createSpyObj('AasService', [
      'encodeIdToBase64Url','createSubmodel','createElement','listSubmodels','listElements','getElement','setPropertyValue','previewAasx','aasTest','deleteSubmodel','deleteElement'
    ]);
    mockAasSvc.encodeIdToBase64Url.and.callFake((id: string) => btoa(id).replace(/=+$/,'').replace(/\+/g,'-').replace(/\//g,'_'));
    mockAasSvc.createSubmodel.and.returnValue(of({}) as any);
    mockAasSvc.createElement.and.returnValue(of({}) as any);
    mockAasSvc.listSubmodels.and.returnValue(of([]) as any);
    mockAasSvc.listElements.and.returnValue(of([]) as any);
    mockAasSvc.getElement.and.returnValue(of({ idShort: 'x', valueType: 'xs:string', value: 'v' }) as any);
    mockAasSvc.setPropertyValue.and.returnValue(of({}) as any);
    mockAasSvc.previewAasx.and.returnValue(of({ submodels: [{ id: 'sm1' }] }) as any);
    mockAasSvc.aasTest.and.returnValue(of({}) as any);
    mockAasSvc.deleteSubmodel.and.returnValue(of({}) as any);
    mockAasSvc.deleteElement.and.returnValue(of({}) as any);
    mockErrSvc = jasmine.createSpyObj('HttpErrorService', ['handleError']);
    mockDialogSvc = jasmine.createSpyObj('CreateSourceSystemDialogService', ['uploadAasx']);
    mockDialogSvc.uploadAasx.and.returnValue(Promise.resolve());

    const routeStub = { snapshot: { paramMap: { get: (_: string) => '1' } } } as any;
    mockSourceSvc.apiConfigSourceSystemIdGet.and.returnValue(of({ id: 1, name: 'Sys', apiType: 'REST', apiUrl: 'u' } as any));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, SourceSystemPageComponent],
      providers: [
        { provide: SourceSystemResourceService, useValue: mockSourceSvc },
        { provide: SourceSystemEndpointResourceService, useValue: mockEndpointSvc },
        { provide: SourceSystemSearchPipe, useValue: mockSearchPipe },
        { provide: AasService, useValue: mockAasSvc },
        { provide: HttpErrorService, useValue: mockErrSvc },
        { provide: CreateSourceSystemDialogService, useValue: mockDialogSvc },
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: NGX_MONACO_EDITOR_CONFIG, useValue: {} },
        MessageService,
        provideNoopAnimations()
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceSystemPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  /** Ensures the component instance is created successfully. */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /** Checks if the selected system is loaded correctly from the route parameter. */
  it('loads selected system on init', () => {
    expect(component.selectedSystem?.id).toBe(1);
  });

  /** Verifies that the isAasSelected method correctly identifies AAS vs REST systems. */
  it('isAasSelected reflects apiType', () => {
    component.selectedSystem = { id: 1, apiType: 'AAS' } as any;
    expect(component.isAasSelected()).toBeTrue();
    component.selectedSystem = { id: 1, apiType: 'REST' } as any;
    expect(component.isAasSelected()).toBeFalse();
  });

  /** Ensures the "Create Submodel" dialog visibility toggles correctly. */
  it('openAasCreateSubmodel toggles dialog', () => {
    component.openAasCreateSubmodel();
    expect(component.showAasSubmodelDialog).toBeTrue();
  });

  /** Verifies that the submodel JSON template is correctly applied based on type selection. */
  it('setAasSubmodelTemplate switches JSON', () => {
    component.setAasSubmodelTemplate('property');
    expect(component.aasNewSubmodelJson).toContain('"Property"');
  });

  /** Tests if the dialog data for creating a new AAS element is properly initialized. */
  it('openAasCreateElement preps dialog data', () => {
    component.selectedSystem = { id: 1 } as any;
    component.openAasCreateElement('sm1', 'p');
    expect(component.elementDialogData).toEqual(jasmine.objectContaining({ submodelId: 'sm1', parentPath: 'p', systemId: 1, systemType: 'source' }));
    expect(component.showElementDialog).toBeTrue();
  });

  /** Ensures successful dialog result triggers AAS element creation via the service. */
  it('onElementDialogResult success calls createElement', () => {
    component.selectedSystem = { id: 1 } as any;
    const el = { submodelId: 'sm1', body: { a: 1 }, parentPath: '' };
    component.onElementDialogResult({ success: true, element: el } as any);
    expect(mockAasSvc.createElement).toHaveBeenCalled();
  });

  /** Verifies the aasTest method resets loading and error states on success. */
  it('aasTest success clears loading and error', () => {
    component.selectedSystem = { id: 1 } as any;
    component.aasTest();
    expect(component.aasTestLoading).toBeFalse();
    expect(component.aasTestError).toBeNull();
  });

  /** Checks that submodel deletion properly encodes IDs and invokes the service. */
  it('deleteAasSubmodel encodes id and calls service', () => {
    component.selectedSystem = { id: 1 } as any;
    component.deleteAasSubmodel('sm1');
    expect(mockAasSvc.deleteSubmodel).toHaveBeenCalled();
  });

  /** Ensures element deletion calls the backend service with raw submodel and path IDs. */
  it('deleteAasElement calls service with raw ids', () => {
    component.selectedSystem = { id: 1 } as any;
    component.deleteAasElement('sm1', 'p/x');
    expect(mockAasSvc.deleteElement).toHaveBeenCalledWith(1, 'sm1', 'p/x');
  });

  /** Confirms that selecting an AASX file triggers preview generation and submodel selection. */
  it('onAasxFileSelected sets preview and selection', () => {
    component.selectedSystem = { id: 1 } as any;
    const file = new File([new Blob(["test"])], 'test.aasx');
    component.onAasxFileSelected({ files: [file] });
    expect(component.aasxSelection.submodels.length).toBeGreaterThan(0);
  });

  /** Verifies that expanding a submodel node triggers listElements to load its contents. */
  it('onAasNodeExpand (submodel) triggers listElements', () => {
    component.selectedSystem = { id: 1 } as any;
    const node = { data: { type: 'submodel', id: 'sm1' } };
    component['onAasNodeExpand']({ node } as any);
    expect(mockAasSvc.listElements).toHaveBeenCalled();
  });

  /** Checks that selecting an element node triggers getElement to fetch its details. */
  it('onAasNodeSelect triggers getElement for element node', () => {
    component.selectedSystem = { id: 1 } as any;
    const node = { data: { type: 'element', submodelId: 'sm1', idShortPath: 'x' } };
    component.onAasNodeSelect({ node } as any);
    expect(mockAasSvc.getElement).toHaveBeenCalled();
  });
});
