import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { HttpErrorService } from '../../../../core/services/http-error.service';

import { ManageApiHeadersComponent } from './manage-api-headers.component';

describe('ManageApiHeadersComponent', () => {
  let component: ManageApiHeadersComponent;
  let fixture: ComponentFixture<ManageApiHeadersComponent>;
  let httpMock: HttpTestingController;
  let messageService: MessageService;
  let errorService: jasmine.SpyObj<HttpErrorService>;

  beforeEach(async () => {
    const errorSpy = jasmine.createSpyObj('HttpErrorService', ['handleError']);
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ManageApiHeadersComponent],
      providers: [MessageService, { provide: HttpErrorService, useValue: errorSpy }]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageApiHeadersComponent);
    component = fixture.componentInstance;
    component.syncSystemId = 1;
    httpMock = TestBed.inject(HttpTestingController);
    messageService = TestBed.inject(MessageService);
    errorService = TestBed.inject(HttpErrorService) as jasmine.SpyObj<HttpErrorService>;
    fixture.detectChanges();

    // Flush initial loadHeaders GET
    const initReq = httpMock.expectOne((req) => req.url.includes('/api/config/sync-system/1/request-header') && req.method === 'GET');
    initReq.flush([]);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should filter Accept/Content-Type when isAas=true', () => {
    component.isAas = true;
    fixture.detectChanges();
    const types = component.allowedHeaderTypes.map(t => String(t.value));
    expect(types.some(v => v.includes('Accept'))).toBeFalse();
    expect(types.some(v => v.toUpperCase().includes('CONTENT'))).toBeFalse();
  });

  it('loads headers on init', () => {
    component.loadHeaders();
    const req = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/1/request-header') && r.method === 'GET');
    req.flush([{ id: 10, headerName: 'Authorization', headerType: 'Authorization', values: [] }] as any);
    expect(component.headers.length).toBe(1);
  });

  it('creates header via addHeader()', () => {
    spyOn(messageService, 'add');
    component.allowValues = true;
    component.form.setValue({ headerName: 'X-Test', headerType: 'Custom', headerValue: 'abc' });
    component.addHeader();

    const post = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/1/request-header') && r.method === 'POST');
    expect(post.request.body).toEqual(jasmine.objectContaining({ headerName: 'X-Test', headerType: 'Custom', values: ['abc'] }));
    post.flush({});

    const reload = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/1/request-header') && r.method === 'GET');
    reload.flush([]);

    expect(messageService.add).toHaveBeenCalled();
  });

  it('updates header when editing is set', () => {
    spyOn(messageService, 'add');
    component.editHeader({ id: 5, headerName: 'User-Agent', headerType: 'UserAgent', values: ['UA'] } as any);
    component.allowValues = true;
    component.form.patchValue({ headerName: 'User-Agent', headerType: 'UserAgent', headerValue: 'UA2' });
    component.addHeader();

    const put = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/request-header/5') && r.method === 'PUT');
    expect(put.request.body).toEqual(jasmine.objectContaining({ headerName: 'User-Agent', headerType: 'UserAgent', values: ['UA2'] }));
    put.flush({});

    const reload = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/1/request-header') && r.method === 'GET');
    reload.flush([]);

    expect(component.editing).toBeNull();
    expect(messageService.add).toHaveBeenCalled();
  });

  it('deleteHeader opens confirmation dialog with correct data', () => {
    const header = { id: 9, headerName: 'Cache-Control', headerType: 'CacheControl' } as any;
    component.deleteHeader(header);
    expect(component.showConfirmationDialog).toBeTrue();
    expect(component.headerToDelete).toEqual(header);
    expect(String(component.confirmationData.message)).toContain('Cache-Control');
  });

  it('onConfirmationConfirmed deletes header and shows toast', () => {
    spyOn(messageService, 'add');
    const header = { id: 12, headerName: 'X-Del', headerType: 'Custom' } as any;
    component.headers = [header];
    component.headerToDelete = header;
    component.onConfirmationConfirmed();

    const del = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/request-header/12') && r.method === 'DELETE');
    del.flush({});

    expect(component.headers.length).toBe(0);
    expect(component.headerToDelete).toBeNull();
    expect(messageService.add).toHaveBeenCalled();
  });

  it('onConfirmationCancelled clears headerToDelete', () => {
    component.headerToDelete = { id: 1 } as any;
    component.onConfirmationCancelled();
    expect(component.headerToDelete).toBeNull();
  });

  it('does nothing when form invalid on addHeader()', () => {
    component.form.reset();
    component.addHeader();
    httpMock.expectNone((r) => r.url.includes('/api/config/sync-system/1/request-header'));
  });

  it('allowedHeaderTypes keeps Accept/Content-Type when isAas=false', () => {
    component.isAas = false;
    const labels = component.allowedHeaderTypes.map(t => t.label);
    expect(labels).toContain('Accept');
    expect(labels).toContain('Content-Type');
  });

  it('editHeader patches form and sets editing', () => {
    const h = { id: 3, headerName: 'X-Patched', headerType: 'Custom', values: ['v1'] } as any;
    component.editHeader(h);
    expect(component.editing).toEqual(h);
    expect(component.form.value.headerName).toBe('X-Patched');
  });

  it('cancelEdit resets editing and form headerType', () => {
    component.editing = { id: 5 } as any;
    component.cancelEdit();
    expect(component.editing).toBeNull();
    // Enum/string may be uppercase depending on model mapping
    expect(String(component.form.value.headerType).toUpperCase()).toBe('CUSTOM');
  });

  it('loadHeaders error calls errorService and stops loading', () => {
    errorService.handleError.calls.reset();
    component.loadHeaders();
    const req = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/1/request-header') && r.method === 'GET');
    component['loading'] = true;
    req.flush({ message: 'err' }, { status: 500, statusText: 'Server Error' });
    expect(errorService.handleError).toHaveBeenCalled();
    expect(component.loading).toBeFalse();
  });

  it('creates header without values when allowValues=false', () => {
    spyOn(messageService, 'add');
    component.allowValues = false;
    component.form.setValue({ headerName: 'X-Plain', headerType: 'Custom', headerValue: 'IGNORED' });
    component.addHeader();

    const post = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/1/request-header') && r.method === 'POST');
    expect(post.request.body.values).toBeUndefined();
    post.flush({});

    const reload = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/1/request-header') && r.method === 'GET');
    reload.flush([]);
  });

  it('update header error calls errorService', () => {
    errorService.handleError.calls.reset();
    component.editHeader({ id: 6, headerName: 'UA', headerType: 'UserAgent', values: [] } as any);
    component.form.patchValue({ headerName: 'UA', headerType: 'UserAgent', headerValue: '' });
    component.addHeader();
    const put = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/request-header/6') && r.method === 'PUT');
    put.flush({ message: 'e' }, { status: 400, statusText: 'Bad' });
    expect(errorService.handleError).toHaveBeenCalled();
  });

  it('delete error calls errorService and clears headerToDelete', () => {
    errorService.handleError.calls.reset();
    const header = { id: 13, headerName: 'X-Err', headerType: 'Custom' } as any;
    component.headerToDelete = header;
    component.onConfirmationConfirmed();
    const del = httpMock.expectOne((r) => r.url.includes('/api/config/sync-system/request-header/13') && r.method === 'DELETE');
    del.flush({ message: 'e' }, { status: 500, statusText: 'Server' });
    expect(errorService.handleError).toHaveBeenCalled();
    expect(component.headerToDelete).toBeNull();
  });
});
