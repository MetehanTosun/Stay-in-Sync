import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { AasManagementComponent } from './aas-management.component';
import { AasManagementService } from '../../services/aas-management.service';
import { AasUtilityService } from '../../services/aas-utility.service';
import { MessageService } from 'primeng/api';
import { TreeNode } from 'primeng/api';
import { HttpClientTestingModule } from '@angular/common/http/testing';

/**
 * Unit tests for {@link AasManagementComponent} in the target system context.
 * Verifies behavior for discovering, expanding, and managing AAS submodels and elements.
 * Includes tests for AASX upload, submodel creation, and error handling.
 */
describe('AasManagementComponent (target-system)', () => {
  let component: AasManagementComponent;
  let fixture: ComponentFixture<AasManagementComponent>;
  let mgmtSpy: jasmine.SpyObj<AasManagementService>;
  let utilSpy: jasmine.SpyObj<AasUtilityService>;

  /**
   * Sets up the test environment for AasManagementComponent.
   * Configures spies for dependent services and initializes the testing module.
   */
  beforeEach(async () => {
    mgmtSpy = jasmine.createSpyObj('AasManagementService', [
      'discoverSubmodels', 'loadSubmodelElements', 'loadElementChildren', 'loadElementDetails',
      'createSubmodel', 'createElement', 'deleteSubmodel', 'deleteElement', 'setElementValue',
      'previewAasx', 'attachSelectedAasx', 'uploadAasx'
    ]);
    utilSpy = jasmine.createSpyObj('AasUtilityService', ['getAasId', 'getParentPath']);
    utilSpy.getAasId.and.returnValue('aas-id');

    await TestBed.configureTestingModule({
      imports: [AasManagementComponent, HttpClientTestingModule],
      providers: [
        provideNoopAnimations(),
        MessageService,
        { provide: AasManagementService, useValue: mgmtSpy },
        { provide: AasUtilityService, useValue: utilSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AasManagementComponent);
    component = fixture.componentInstance;
    component.system = { id: 1, apiUrl: 'http://localhost:8080' } as any;
  });

  /**
   * Verifies that the AasManagementComponent is successfully created.
   */
  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  /**
   * Tests the discoverSnapshot method to ensure that submodels are fetched
   * and the component's treeNodes are populated correctly.
   */
  it('discoverSnapshot should populate treeNodes', fakeAsync(async () => {
    const nodes: TreeNode[] = [{ key: 'sm1', label: 'SM', data: { type: 'submodel', id: 'sm1' }, children: [] } as any];
    mgmtSpy.discoverSubmodels.and.returnValue(Promise.resolve(nodes));

    await component.discoverSnapshot();
    tick();

    expect(mgmtSpy.discoverSubmodels).toHaveBeenCalledWith(1);
    expect(component.treeNodes.length).toBe(1);
  }));

  /**
   * Verifies that expanding a submodel node triggers loading of its submodel elements.
   */
  it('onNodeExpand should load submodel children', fakeAsync(async () => {
    mgmtSpy.loadSubmodelElements.and.returnValue(Promise.resolve([{ key: 'k', label: 'el', data: { type: 'element' } } as any]));
    const node = { data: { type: 'submodel', id: 'sm1' }, children: [] } as any;

    await component.onNodeExpand({ node });
    tick();

    expect(mgmtSpy.loadSubmodelElements).toHaveBeenCalledWith(1, 'sm1');
  }));

  /**
   * Verifies that expanding an element node triggers loading of its child elements.
   */
  it('onNodeExpand should load element children for element node', fakeAsync(async () => {
    mgmtSpy.loadElementChildren.and.returnValue(Promise.resolve([{ key: 'k2', label: 'child', data: { type: 'element' } } as any]));
    const node = { data: { type: 'element', submodelId: 'sm1', idShortPath: 'parent' }, children: [] } as any;

    await component.onNodeExpand({ node });
    tick();

    expect(mgmtSpy.loadElementChildren).toHaveBeenCalledWith(1, 'sm1', 'parent');
  }));

  /**
   * Tests that selecting a node triggers loading of element details
   * and updates the selectedNode property accordingly.
   */
  it('onNodeSelect should load element details', fakeAsync(async () => {
    mgmtSpy.loadElementDetails.and.returnValue(Promise.resolve({ label: 'x', type: 'Property' } as any));
    const node = { data: { type: 'element', submodelId: 'sm1', idShortPath: 'p' } } as any;

    await component.onNodeSelect({ node });
    tick();

    expect(component.selectedNode).toBe(node);
    expect(mgmtSpy.loadElementDetails).toHaveBeenCalledWith(1, 'sm1', 'p');
  }));

  /**
   * Verifies that createSubmodel calls the service and closes the dialog
   * after successfully creating a new submodel.
   */
  it('createSubmodel should call service and close dialog', fakeAsync(async () => {
    mgmtSpy.createSubmodel.and.returnValue(Promise.resolve());
    component.showSubmodelDialog = true;
    component.newSubmodelJson = '{"id":"x","idShort":"y"}';

    await component.createSubmodel();
    tick();

    expect(mgmtSpy.createSubmodel).toHaveBeenCalledWith(1, jasmine.any(Object));
    expect(component.showSubmodelDialog).toBeFalse();
  }));

  /**
   * Tests uploadAasx behavior when no file is selected.
   * Ensures that a warning message is displayed and no service call is made.
   */
  it('uploadAasx with no file should warn and return', () => {
    component.aasxSelectedFile = null;
    spyOn(TestBed.inject(MessageService), 'add');
    component.uploadAasx();
    expect(TestBed.inject(MessageService).add).toHaveBeenCalled();
  });

  /**
   * Verifies that uploading an AASX file with full selection triggers the
   * attachSelectedAasx service method.
   */
  it('uploadAasx with full selection calls attachSelectedAasx', fakeAsync(async () => {
    const file = new File(['x'], 'a.aasx');
    component.aasxSelectedFile = file as any;
    component.aasxSelection = { submodels: [{ id: 'sm1', full: true, elements: [] }] };
    mgmtSpy.attachSelectedAasx.and.returnValue(Promise.resolve());

    component.uploadAasx();
    tick();

    expect(mgmtSpy.attachSelectedAasx).toHaveBeenCalled();
  }));

  /**
   * Tests that uploading an AASX file without selection triggers the
   * uploadAasx service method.
   */
  it('uploadAasx without selection calls uploadAasx', fakeAsync(async () => {
    const file = new File(['x'], 'a.aasx');
    component.aasxSelectedFile = file as any;
    component.aasxSelection = { submodels: [] };
    mgmtSpy.uploadAasx.and.returnValue(Promise.resolve());

    component.uploadAasx();
    tick();

    expect(mgmtSpy.uploadAasx).toHaveBeenCalled();
  }));

  /**
   * Tests deleteElement behavior when the service throws an error.
   * Ensures that an error message is displayed through the MessageService.
   */
  it('deleteElement should show error toast on failure', fakeAsync(async () => {
    mgmtSpy.deleteElement.and.returnValue(Promise.reject(new Error('Not found')));
    spyOn(TestBed.inject(MessageService), 'add');

    await component.deleteElement('sm1', 'x');
    tick();

    expect(TestBed.inject(MessageService).add).toHaveBeenCalled();
  }));
});
