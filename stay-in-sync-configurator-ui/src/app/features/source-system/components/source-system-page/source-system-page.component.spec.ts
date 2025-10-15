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

describe('SourceSystemPageComponent', () => {
  let component: SourceSystemPageComponent;
  let fixture: ComponentFixture<SourceSystemPageComponent>;
  let mockSourceSvc: jasmine.SpyObj<SourceSystemResourceService>;
  let mockEndpointSvc: jasmine.SpyObj<SourceSystemEndpointResourceService>;
  let mockSearchPipe: jasmine.SpyObj<SourceSystemSearchPipe>;
  let mockAasSvc: jasmine.SpyObj<AasService>;
  let mockErrSvc: jasmine.SpyObj<HttpErrorService>;
  let mockDialogSvc: jasmine.SpyObj<CreateSourceSystemDialogService>;

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

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads selected system on init', () => {
    expect(component.selectedSystem?.id).toBe(1);
  });

  it('isAasSelected reflects apiType', () => {
    component.selectedSystem = { id: 1, apiType: 'AAS' } as any;
    expect(component.isAasSelected()).toBeTrue();
    component.selectedSystem = { id: 1, apiType: 'REST' } as any;
    expect(component.isAasSelected()).toBeFalse();
  });

  it('openAasCreateSubmodel toggles dialog', () => {
    component.openAasCreateSubmodel();
    expect(component.showAasSubmodelDialog).toBeTrue();
  });

  it('setAasSubmodelTemplate switches JSON', () => {
    component.setAasSubmodelTemplate('property');
    expect(component.aasNewSubmodelJson).toContain('"Property"');
  });

  it('openAasCreateElement preps dialog data', () => {
    component.selectedSystem = { id: 1 } as any;
    component.openAasCreateElement('sm1', 'p');
    expect(component.elementDialogData).toEqual(jasmine.objectContaining({ submodelId: 'sm1', parentPath: 'p', systemId: 1, systemType: 'source' }));
    expect(component.showElementDialog).toBeTrue();
  });

  it('onElementDialogResult success calls createElement', () => {
    component.selectedSystem = { id: 1 } as any;
    const el = { submodelId: 'sm1', body: { a: 1 }, parentPath: '' };
    component.onElementDialogResult({ success: true, element: el } as any);
    expect(mockAasSvc.createElement).toHaveBeenCalled();
  });

  it('aasTest success clears loading and error', () => {
    component.selectedSystem = { id: 1 } as any;
    component.aasTest();
    expect(component.aasTestLoading).toBeFalse();
    expect(component.aasTestError).toBeNull();
  });

  it('deleteAasSubmodel encodes id and calls service', () => {
    component.selectedSystem = { id: 1 } as any;
    component.deleteAasSubmodel('sm1');
    expect(mockAasSvc.deleteSubmodel).toHaveBeenCalled();
  });

  it('deleteAasElement calls service with raw ids', () => {
    component.selectedSystem = { id: 1 } as any;
    component.deleteAasElement('sm1', 'p/x');
    expect(mockAasSvc.deleteElement).toHaveBeenCalledWith(1, 'sm1', 'p/x');
  });

  it('onAasxFileSelected sets preview and selection', () => {
    component.selectedSystem = { id: 1 } as any;
    const file = new File([new Blob(["test"])], 'test.aasx');
    component.onAasxFileSelected({ files: [file] });
    expect(component.aasxSelection.submodels.length).toBeGreaterThan(0);
  });

  it('onAasNodeExpand (submodel) triggers listElements', () => {
    component.selectedSystem = { id: 1 } as any;
    const node = { data: { type: 'submodel', id: 'sm1' } };
    component['onAasNodeExpand']({ node } as any);
    expect(mockAasSvc.listElements).toHaveBeenCalled();
  });

  it('onAasNodeSelect triggers getElement for element node', () => {
    component.selectedSystem = { id: 1 } as any;
    const node = { data: { type: 'element', submodelId: 'sm1', idShortPath: 'x' } };
    component.onAasNodeSelect({ node } as any);
    expect(mockAasSvc.getElement).toHaveBeenCalled();
  });
});
