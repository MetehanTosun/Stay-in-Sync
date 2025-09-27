import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SourceSystemAasManagementComponent } from './source-system-aas-management.component';
import { SourceSystemAasManagementService } from '../../services/source-system-aas-management.service';
import { of } from 'rxjs';

describe('SourceSystemAasManagementComponent', () => {
  let component: SourceSystemAasManagementComponent;
  let fixture: ComponentFixture<SourceSystemAasManagementComponent>;
  let mockAasManagementService: jasmine.SpyObj<SourceSystemAasManagementService>;

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

    await TestBed.configureTestingModule({
      imports: [SourceSystemAasManagementComponent],
      providers: [
        { provide: SourceSystemAasManagementService, useValue: spy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SourceSystemAasManagementComponent);
    component = fixture.componentInstance;
    mockAasManagementService = TestBed.inject(SourceSystemAasManagementService) as jasmine.SpyObj<SourceSystemAasManagementService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should check if AAS is selected', () => {
    component.system = { apiType: 'AAS' } as any;
    expect(component.isAasSelected()).toBe(true);

    component.system = { apiType: 'REST' } as any;
    expect(component.isAasSelected()).toBe(false);
  });

  it('should get AAS ID', () => {
    component.system = { aasId: 'test-id' } as any;
    expect(component.getAasId()).toBe('test-id');

    component.system = { apiUrl: 'http://localhost:8080/aas/12345' } as any;
    expect(component.getAasId()).toBe('12345');
  });

  it('should test AAS connection', () => {
    component.system = { id: 1 } as any;
    mockAasManagementService.testConnection.and.returnValue(of({}));

    component.testAasConnection();

    expect(mockAasManagementService.testConnection).toHaveBeenCalledWith(1);
    expect(component.aasTestLoading).toBe(false);
  });

  it('should discover AAS snapshot', () => {
    component.system = { id: 1 } as any;
    const mockNodes = [{ key: '1', label: 'Test' }];
    mockAasManagementService.discoverSnapshot.and.returnValue(of(mockNodes));

    component.discoverAasSnapshot();

    expect(mockAasManagementService.discoverSnapshot).toHaveBeenCalledWith(1);
    expect(component.aasTreeNodes).toEqual(mockNodes);
    expect(component.aasTreeLoading).toBe(false);
  });

  it('should handle AAS node expand', () => {
    component.system = { id: 1 } as any;
    const mockNode = { data: { type: 'submodel', id: 'sm1' } };
    const mockEvent = { node: mockNode };
    mockAasManagementService.loadChildren.and.returnValue(of(undefined));

    component.onAasNodeExpand(mockEvent);

    expect(mockAasManagementService.loadChildren).toHaveBeenCalledWith(1, 'sm1', undefined, mockNode);
  });

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

  it('should open AAS create submodel dialog', () => {
    component.openAasCreateSubmodel();
    expect(component.showAasSubmodelDialog).toBe(true);
  });

  it('should set AAS submodel template', () => {
    component.setAasSubmodelTemplate('minimal');
    expect(component.aasNewSubmodelJson).toBe(component.aasMinimalSubmodelTemplate);

    component.setAasSubmodelTemplate('property');
    expect(component.aasNewSubmodelJson).toBe(component.aasPropertySubmodelTemplate);

    component.setAasSubmodelTemplate('collection');
    expect(component.aasNewSubmodelJson).toBe(component.aasCollectionSubmodelTemplate);
  });

  it('should create AAS submodel', () => {
    component.system = { id: 1 } as any;
    component.aasNewSubmodelJson = '{"id": "test", "idShort": "Test"}';
    mockAasManagementService.createSubmodel.and.returnValue(of({}));
    mockAasManagementService.discoverSnapshot.and.returnValue(of([]));

    component.aasCreateSubmodel();

    expect(mockAasManagementService.createSubmodel).toHaveBeenCalledWith(1, { id: 'test', idShort: 'Test' });
    expect(component.showAasSubmodelDialog).toBe(false);
  });

  it('should open AAS create element dialog', () => {
    component.openAasCreateElement('sm1', 'parent/path');
    expect(component.aasTargetSubmodelId).toBe('sm1');
    expect(component.aasParentPath).toBe('parent/path');
    expect(component.showAasElementDialog).toBe(true);
  });

  it('should set AAS element template', () => {
    component.setAasElementTemplate('property');
    expect(component.aasNewElementJson).toBe(component.aasElementTemplateProperty);

    component.setAasElementTemplate('range');
    expect(component.aasNewElementJson).toBe(component.aasElementTemplateRange);

    component.setAasElementTemplate('unknown');
    expect(component.aasNewElementJson).toBe('{}');
  });

  it('should create AAS element', () => {
    component.system = { id: 1 } as any;
    component.aasTargetSubmodelId = 'sm1';
    component.aasParentPath = 'parent';
    component.aasNewElementJson = '{"modelType": "Property", "idShort": "Test"}';
    mockAasManagementService.createElement.and.returnValue(of({}));
    mockAasManagementService.loadChildren.and.returnValue(of(undefined));

    component.aasCreateElement();

    expect(mockAasManagementService.createElement).toHaveBeenCalledWith(1, 'sm1', { modelType: 'Property', idShort: 'Test' }, 'parent');
    expect(component.showAasElementDialog).toBe(false);
  });

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

  it('should delete AAS submodel', () => {
    component.system = { id: 1 } as any;
    mockAasManagementService.deleteSubmodel.and.returnValue(of({}));
    mockAasManagementService.discoverSnapshot.and.returnValue(of([]));

    component.deleteAasSubmodel('sm1');

    expect(mockAasManagementService.deleteSubmodel).toHaveBeenCalledWith(1, 'sm1');
  });

  it('should delete AAS element', () => {
    component.system = { id: 1 } as any;
    mockAasManagementService.deleteElement.and.returnValue(of({}));
    mockAasManagementService.loadChildren.and.returnValue(of(undefined));
    mockAasManagementService.findNodeByKey.and.returnValue({} as any);

    component.deleteAasElement('sm1', 'path1');

    expect(mockAasManagementService.deleteElement).toHaveBeenCalledWith(1, 'sm1', 'path1');
  });
});
