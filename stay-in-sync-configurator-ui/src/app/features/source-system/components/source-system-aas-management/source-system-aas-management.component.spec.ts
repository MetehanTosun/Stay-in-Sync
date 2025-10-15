/**
 * Unit tests for `SourceSystemAasManagementComponent`.
 * Covers AAS selection, ID derivation, connection tests, snapshot discovery,
 * node interactions (expand/select), and CRUD actions for submodels/elements,
 * including value setting flows.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SourceSystemAasManagementComponent } from './source-system-aas-management.component';
import { SourceSystemAasManagementService } from '../../services/source-system-aas-management.service';
import { of } from 'rxjs';
import { AasService } from '../../services/aas.service';
import { AasUtilityService } from '../../../target-system/services/aas-utility.service';

/** Test suite for core AAS management behaviors on the source system page. */
describe('SourceSystemAasManagementComponent', () => {
  let component: SourceSystemAasManagementComponent;
  let fixture: ComponentFixture<SourceSystemAasManagementComponent>;
  let mockAasManagementService: jasmine.SpyObj<SourceSystemAasManagementService>;
  let mockAasService: jasmine.SpyObj<AasService>;
  let mockAasUtility: jasmine.SpyObj<AasUtilityService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('SourceSystemAasManagementService', [
      'testConnection',
      'discoverSnapshot',
      'loadChildren',
      'loadElementDetails',
      'createSubmodel',
      'createElement',
      'deleteSubmodel',
      'deleteElement',
      'setPropertyValue',
      'parseValueForType',
      'findNodeByKey'
    ]);
    mockAasService = jasmine.createSpyObj('AasService', [
      'encodeIdToBase64Url', 'createElement', 'listElements', 'previewAasx', 'attachSelectedAasx', 'uploadAasx'
    ]);
    mockAasService.encodeIdToBase64Url.and.callFake((id: string) => btoa(id).replace(/=+$/,'').replace(/\+/g,'-').replace(/\//g,'_'));
    mockAasService.createElement.and.returnValue(of({}) as any);
    mockAasService.listElements.and.returnValue(of([]) as any);
    mockAasUtility = jasmine.createSpyObj('AasUtilityService', ['getAasId']);
    mockAasUtility.getAasId.and.callFake((sys: any) => sys?.aasId || (sys?.apiUrl ? String(sys.apiUrl).split('/').pop() : ''));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, SourceSystemAasManagementComponent],
      providers: [
        { provide: SourceSystemAasManagementService, useValue: spy },
        { provide: AasService, useValue: mockAasService },
        { provide: AasUtilityService, useValue: mockAasUtility },
        provideNoopAnimations()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SourceSystemAasManagementComponent);
    component = fixture.componentInstance;
    mockAasManagementService = TestBed.inject(SourceSystemAasManagementService) as jasmine.SpyObj<SourceSystemAasManagementService>;
  });

  /** Component should be created successfully. */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /** Verifies detection of whether an AAS system is selected. */
  it('should check if AAS is selected', () => {
    component.system = { apiType: 'AAS' } as any;
    expect(component.isAasSelected()).toBe(true);

    component.system = { apiType: 'REST' } as any;
    expect(component.isAasSelected()).toBe(false);
  });

  /** Derives the AAS identifier from explicit `aasId` or from `apiUrl`. */
  it('should get AAS ID', () => {
    component.system = { aasId: 'test-id' } as any;
    expect(component.getAasId()).toBe('test-id');

    component.system = { apiUrl: 'http://localhost:8080/aas/12345' } as any;
    expect(component.getAasId()).toBe('12345');
  });

  /** Tests connection to AAS backend service. */
  it('should test AAS connection', () => {
    component.system = { id: 1 } as any;
    mockAasManagementService.testConnection.and.returnValue(of({}));

    component.testAasConnection();

    expect(mockAasManagementService.testConnection).toHaveBeenCalledWith(1);
    expect(component.aasTestLoading).toBe(false);
  });

  /** Discovers and loads the AAS snapshot tree. */
  it('should discover AAS snapshot', () => {
    component.system = { id: 1 } as any;
    const mockNodes = [{ key: '1', label: 'Test' }];
    mockAasManagementService.discoverSnapshot.and.returnValue(of(mockNodes));

    component.discoverAasSnapshot();

    expect(mockAasManagementService.discoverSnapshot).toHaveBeenCalledWith(1);
    expect(component.aasTreeNodes).toEqual(mockNodes);
    expect(component.aasTreeLoading).toBe(false);
  });

  /** Expands a node to load its children for a given submodel. */
  it('should handle AAS node expand', () => {
    component.system = { id: 1 } as any;
    const mockNode = { data: { type: 'submodel', id: 'sm1' } };
    const mockEvent = { node: mockNode };
    mockAasManagementService.loadChildren.and.returnValue(of(undefined));

    component.onAasNodeExpand(mockEvent);

    expect(mockAasManagementService.loadChildren).toHaveBeenCalledWith(1, 'sm1', undefined, mockNode);
  });

  /** Selects a node to load and display live element details. */
  it('should handle AAS node select', () => {
    component.system = { id: 1 } as any;
    const mockNode = { data: { type: 'element', submodelId: 'sm1', idShortPath: 'path1' } };
    const mockEvent = { node: mockNode };
    const mockLivePanel = { label: 'Test', type: 'Property' };
    mockAasManagementService.loadElementDetails.and.returnValue(of(mockLivePanel));

    component.onAasNodeSelect(mockEvent);

    expect(component.selectedAasNode).toBe(mockNode);
    expect(mockAasManagementService.loadElementDetails).toHaveBeenCalledWith(1, 'sm1', 'path1', mockNode);
  });

  /** Opens the dialog for creating a new submodel. */
  it('should open AAS create submodel dialog', () => {
    component.openAasCreateSubmodel();
    expect(component.showAasSubmodelDialog).toBe(true);
  });

  /** Sets the creation template for a new submodel. */
  it('should set AAS submodel template', () => {
    component.setAasSubmodelTemplate('minimal');
    expect(component.aasNewSubmodelJson).toBe(component.aasMinimalSubmodelTemplate);

    component.setAasSubmodelTemplate('property');
    expect(component.aasNewSubmodelJson).toBe(component.aasPropertySubmodelTemplate);

    component.setAasSubmodelTemplate('collection');
    expect(component.aasNewSubmodelJson).toBe(component.aasCollectionSubmodelTemplate);
  });

  /** Creates a new submodel and closes the dialog. */
  it('should create AAS submodel', () => {
    component.system = { id: 1 } as any;
    component.aasNewSubmodelJson = '{"id": "test", "idShort": "Test"}';
    mockAasManagementService.createSubmodel.and.returnValue(of({}));
    mockAasManagementService.discoverSnapshot.and.returnValue(of([]));

    component.aasCreateSubmodel();

    expect(mockAasManagementService.createSubmodel).toHaveBeenCalledWith(1, { id: 'test', idShort: 'Test' });
    expect(component.showAasSubmodelDialog).toBe(false);
  });

  /** Opens the dialog for creating a new element under the given parent. */
  it('should open AAS create element dialog', () => {
    component.system = { id: 1 } as any;
    component.openAasCreateElement('sm1', 'parent/path');
    expect(component.elementDialogData).toEqual(jasmine.objectContaining({
      submodelId: 'sm1',
      parentPath: 'parent/path',
      systemId: 1,
      systemType: 'source'
    }));
    expect(component.showElementDialog).toBe(true);
  });

  /** Opens the dialog to set a value for a selected element. */
  it('should open AAS set value dialog', () => {
    const mockElement = { idShortPath: 'path1', valueType: 'xs:string' };
    component.aasSelectedLivePanel = { value: 'test value' } as any;
    component.selectedAasNode = { data: { idShortPath: 'path1' } } as any;

    component.openAasSetValue('sm1', mockElement);

    expect(component.aasValueSubmodelId).toBe('sm1');
    expect(component.aasValueElementPath).toBe('path1');
    expect(component.aasValueTypeHint).toBe('xs:string');
    expect(component.aasValueNew).toBe('test value');
    expect(component.showAasValueDialog).toBe(true);
  });

  /** Sets a property value and refreshes element details. */
  it('should set AAS value', () => {
    component.system = { id: 1 } as any;
    component.aasValueSubmodelId = 'sm1';
    component.aasValueElementPath = 'path1';
    component.aasValueNew = '42';
    component.aasValueTypeHint = 'xs:integer';
    mockAasManagementService.parseValueForType.and.returnValue(42);
    mockAasManagementService.setPropertyValue.and.returnValue(of({}));
    mockAasManagementService.loadElementDetails.and.returnValue(of({} as any));

    component.aasSetValue();

    expect(mockAasManagementService.parseValueForType).toHaveBeenCalledWith('42', 'xs:integer');
    expect(mockAasManagementService.setPropertyValue).toHaveBeenCalledWith(1, 'sm1', 'path1', 42);
    expect(component.showAasValueDialog).toBe(false);
  });

  /** Deletes a submodel. */
  it('should delete AAS submodel', () => {
    component.system = { id: 1 } as any;
    mockAasManagementService.deleteSubmodel.and.returnValue(of({}));
    mockAasManagementService.discoverSnapshot.and.returnValue(of([]));

    component.deleteAasSubmodel('sm1');

    expect(mockAasManagementService.deleteSubmodel).toHaveBeenCalledWith(1, 'sm1');
  });

  /** Deletes an element and refreshes children. */
  it('should delete AAS element', () => {
    component.system = { id: 1 } as any;
    mockAasManagementService.deleteElement.and.returnValue(of({}));
    mockAasManagementService.loadChildren.and.returnValue(of(undefined));
    mockAasManagementService.findNodeByKey.and.returnValue({} as any);

    component.deleteAasElement('sm1', 'path1');

    expect(mockAasManagementService.deleteElement).toHaveBeenCalledWith(1, 'sm1', 'path1');
  });
});
