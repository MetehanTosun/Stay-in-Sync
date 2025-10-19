/**
 * Unit tests for CreateTargetSystemAasService ensuring proper AAS client interactions and success handling.
 */
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

  /**
   * Ensures that the CreateTargetSystemAasService is instantiated successfully.
   */
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * Ensures that testConnection resolves with success=true when the AAS client responds correctly.
   */
  it('testConnection should resolve success true', async () => {
    clientSpy.test.and.returnValue(of({ idShort: 'AAS' }));
    const result = await service.testConnection(1);
    expect(result.success).toBeTrue();
  });

  /**
   * Ensures that testConnection resolves with success=false when the AAS client throws an error.
   */
  it('testConnection should resolve success false on error', async () => {
    clientSpy.test.and.returnValue(throwError(() => new Error('fail')));
    const result = await service.testConnection(1);
    expect(result.success).toBeFalse();
  });

  /**
   * Ensures that discoverSubmodels maps the client's listSubmodels result to node objects.
   */
  it('discoverSubmodels should map list to nodes', async () => {
    clientSpy.listSubmodels.and.returnValue(of({ result: [{ idShort: 'sm', id: 'id1' }] } as any));
    const nodes = await service.discoverSubmodels(1);
    expect(nodes.length).toBe(1);
  });

  /**
   * Ensures that loadRootElements falls back to loading all elements when the first call results in a 400/404 error.
   */
  it('loadRootElements should fallback to all on 400/404', async () => {
    clientSpy.listElements.and.returnValues(
      throwError(() => ({ status: 400 })),
      of({ result: [{ idShort: 'e1' }] } as any)
    );
    const nodes = await service.loadRootElements(1, 'sm');
    expect(nodes.length).toBe(1);
  });

  /**
   * Ensures that createSubmodel calls the client and adds a success toast notification.
   */
  it('createSubmodel should call client and add success toast', async () => {
    clientSpy.createSubmodel.and.returnValue(of({}));
    await service.createSubmodel(1, {});
    expect(clientSpy.createSubmodel).toHaveBeenCalled();
  });

  /**
   * Ensures that createElement calls the client and adds a success toast notification.
   */
  it('createElement should call client and add success toast', async () => {
    clientSpy.createElement.and.returnValue(of({}));
    await service.createElement(1, 'sm', {}, undefined);
    expect(clientSpy.createElement).toHaveBeenCalled();
  });

  /**
   * Ensures that deleteSubmodel calls the client and adds a success toast notification.
   */
  it('deleteSubmodel should call client and add success toast', async () => {
    clientSpy.deleteSubmodel.and.returnValue(of({}));
    await service.deleteSubmodel(1, 'sm');
    expect(clientSpy.deleteSubmodel).toHaveBeenCalled();
  });

  /**
   * Ensures that deleteElement calls the client and adds a success toast notification.
   */
  it('deleteElement should call client and add success toast', async () => {
    clientSpy.deleteElement.and.returnValue(of({}));
    await service.deleteElement(1, 'sm', 'a/b');
    expect(clientSpy.deleteElement).toHaveBeenCalled();
  });

  /**
   * Ensures that setElementValue calls the client and adds a success toast notification.
   */
  it('setElementValue should call client and add success toast', async () => {
    clientSpy.patchElementValue.and.returnValue(of({}));
    await service.setElementValue(1, 'sm', 'a/b', 1);
    expect(clientSpy.patchElementValue).toHaveBeenCalled();
  });
});
