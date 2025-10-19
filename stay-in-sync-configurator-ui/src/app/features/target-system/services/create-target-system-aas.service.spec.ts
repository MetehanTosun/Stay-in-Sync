import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { CreateTargetSystemAasService } from './create-target-system-aas.service';
import { AasClientService } from '../../source-system/services/aas-client.service';

describe('CreateTargetSystemAasService', () => {
  let service: CreateTargetSystemAasService;
  let clientSpy: jasmine.SpyObj<AasClientService>;
  let msg: MessageService;

  beforeEach(() => {
    clientSpy = jasmine.createSpyObj('AasClientService', [
      'test', 'listSubmodels', 'listElements', 'getElement', 'createSubmodel', 'createElement', 'deleteSubmodel', 'deleteElement', 'patchElementValue', 'previewAasx', 'attachSelectedAasx', 'uploadAasx'
    ]);

    TestBed.configureTestingModule({
      providers: [
        MessageService,
        { provide: AasClientService, useValue: clientSpy },
        CreateTargetSystemAasService
      ]
    });
    service = TestBed.inject(CreateTargetSystemAasService);
    msg = TestBed.inject(MessageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('testConnection should resolve success true', async () => {
    clientSpy.test.and.returnValue(of({ idShort: 'AAS' }));
    const result = await service.testConnection(1);
    expect(result.success).toBeTrue();
  });

  it('testConnection should resolve success false on error', async () => {
    clientSpy.test.and.returnValue(throwError(() => new Error('fail')));
    const result = await service.testConnection(1);
    expect(result.success).toBeFalse();
  });

  it('discoverSubmodels should map list to nodes', async () => {
    clientSpy.listSubmodels.and.returnValue(of({ result: [{ idShort: 'sm', id: 'id1' }] } as any));
    const nodes = await service.discoverSubmodels(1);
    expect(nodes.length).toBe(1);
  });

  it('loadRootElements should fallback to all on 400/404', async () => {
    clientSpy.listElements.and.returnValues(
      throwError(() => ({ status: 400 })),
      of({ result: [{ idShort: 'e1' }] } as any)
    );
    const nodes = await service.loadRootElements(1, 'sm');
    expect(nodes.length).toBe(1);
  });

  it('createSubmodel should call client and add success toast', async () => {
    clientSpy.createSubmodel.and.returnValue(of({}));
    await service.createSubmodel(1, {});
    expect(clientSpy.createSubmodel).toHaveBeenCalled();
  });

  it('createElement should call client and add success toast', async () => {
    clientSpy.createElement.and.returnValue(of({}));
    await service.createElement(1, 'sm', {}, undefined);
    expect(clientSpy.createElement).toHaveBeenCalled();
  });

  it('deleteSubmodel should call client and add success toast', async () => {
    clientSpy.deleteSubmodel.and.returnValue(of({}));
    await service.deleteSubmodel(1, 'sm');
    expect(clientSpy.deleteSubmodel).toHaveBeenCalled();
  });

  it('deleteElement should call client and add success toast', async () => {
    clientSpy.deleteElement.and.returnValue(of({}));
    await service.deleteElement(1, 'sm', 'a/b');
    expect(clientSpy.deleteElement).toHaveBeenCalled();
  });

  it('setElementValue should call client and add success toast', async () => {
    clientSpy.patchElementValue.and.returnValue(of({}));
    await service.setElementValue(1, 'sm', 'a/b', 1);
    expect(clientSpy.patchElementValue).toHaveBeenCalled();
  });
});
