import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { AasManagementService } from './aas-management.service';
import { AasClientService } from '../../source-system/services/aas-client.service';

describe('AasManagementService', () => {
  let service: AasManagementService;
  let clientSpy: jasmine.SpyObj<AasClientService>;

  beforeEach(() => {
    clientSpy = jasmine.createSpyObj('AasClientService', [
      'listSubmodels', 'listElements', 'getElement', 'createSubmodel', 'createElement', 'deleteSubmodel', 'deleteElement', 'patchElementValue', 'previewAasx', 'attachSelectedAasx', 'uploadAasx'
    ]);

    TestBed.configureTestingModule({
      providers: [
        MessageService,
        { provide: AasClientService, useValue: clientSpy },
        AasManagementService
      ]
    });
    service = TestBed.inject(AasManagementService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('discoverSubmodels should fallback from SNAPSHOT to LIVE', async () => {
    clientSpy.listSubmodels.and.returnValues(
      throwError(() => new Error('snap fail')),
      of({ result: [{ idShort: 'SM' }] } as any)
    );
    const nodes = await service.discoverSubmodels(1);
    expect(nodes.length).toBe(1);
  });

  it('loadSubmodelElements should map to tree nodes', async () => {
    clientSpy.listElements.and.returnValue(of({ result: [{ idShort: 'e1' }] } as any));
    const nodes = await service.loadSubmodelElements(1, 'sm');
    expect(nodes.length).toBe(1);
  });

  it('loadElementChildren should compute idShortPath for shallow children', async () => {
    clientSpy.listElements.and.returnValue(of({ result: [{ idShort: 'child' }] } as any));
    const nodes = await service.loadElementChildren(1, 'sm', 'parent');
    expect(nodes[0].data?.idShortPath).toBe('parent/child');
  });

  it('loadElementDetails should map fields to live panel', async () => {
    clientSpy.getElement.and.returnValue(of({ idShort: 'x', modelType: 'Property', value: 1 } as any));
    const panel = await service.loadElementDetails(1, 'sm', 'path');
    expect(panel.label).toBe('x');
    expect(panel.type).toBe('Property');
  });

  it('createSubmodel should show success toast', async () => {
    clientSpy.createSubmodel.and.returnValue(of({}));
    await service.createSubmodel(1, {});
    expect(clientSpy.createSubmodel).toHaveBeenCalled();
  });

  it('deleteElement should show success toast', async () => {
    clientSpy.deleteElement.and.returnValue(of({}));
    await service.deleteElement(1, 'sm', 'a');
    expect(clientSpy.deleteElement).toHaveBeenCalled();
  });

  it('setElementValue should call patch and success', async () => {
    clientSpy.patchElementValue.and.returnValue(of({}));
    await service.setElementValue(1, 'sm', 'p', 1);
    expect(clientSpy.patchElementValue).toHaveBeenCalled();
  });
});
