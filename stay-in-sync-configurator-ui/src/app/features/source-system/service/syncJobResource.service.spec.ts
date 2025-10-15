import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SyncJobResourceService } from './syncJobResource.service';

describe('SyncJobResourceService', () => {
  let service: SyncJobResourceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(SyncJobResourceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('apiConfigSyncJobGet should GET /api/config/sync-job', () => {
    service.apiConfigSyncJobGet().subscribe((resp) => {
      expect(Array.isArray(resp)).toBeTrue();
    });
    const req = httpMock.expectOne('/api/config/sync-job');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('apiConfigSyncJobIdGet should GET by id', () => {
    service.apiConfigSyncJobIdGet(1).subscribe((resp) => {
      expect(resp).toBeTruthy();
    });
    const req = httpMock.expectOne('/api/config/sync-job/1');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1 });
  });

  it('apiConfigSyncJobPost should POST body', () => {
    const body: any = { name: 'job' };
    service.apiConfigSyncJobPost(body).subscribe((resp) => {
      expect(resp).toBeTruthy();
    });
    const req = httpMock.expectOne('/api/config/sync-job');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 10, ...body });
  });

  it('apiConfigSyncJobIdPut should PUT body to id', () => {
    const body: any = { id: 2, name: 'job2' };
    service.apiConfigSyncJobIdPut(2, body).subscribe((resp) => {
      expect(resp).toBeTruthy();
    });
    const req = httpMock.expectOne('/api/config/sync-job/2');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  it('apiConfigSyncJobIdDelete should DELETE id', () => {
    service.apiConfigSyncJobIdDelete(3).subscribe((resp) => {
      expect(resp).toBeDefined();
    });
    const req = httpMock.expectOne('/api/config/sync-job/3');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});


