/** Unit tests for `SourceSystemResourceService` HTTP methods. */
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SourceSystemResourceService } from './sourceSystemResource.service';

/** Verifies the HTTP contract of `SourceSystemResourceService`. */
describe('SourceSystemResourceService', () => {
  let service: SourceSystemResourceService;
  let httpMock: HttpTestingController;

  /** Configure testing module and inject dependencies. */
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(SourceSystemResourceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  /** Ensure no unexpected HTTP requests remain. */
  afterEach(() => {
    httpMock.verify();
  });

  /** Should issue GET request to list all source systems. */
  it('apiConfigSourceSystemGet should GET list', () => {
    service.apiConfigSourceSystemGet().subscribe((resp) => {
      expect(Array.isArray(resp)).toBeTrue();
    });
    const req = httpMock.expectOne('/api/config/source-system');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  /** Should issue GET request to fetch a single source system by id. */
  it('apiConfigSourceSystemIdGet should GET by id', () => {
    service.apiConfigSourceSystemIdGet(1).subscribe((resp) => {
      expect(resp).toBeTruthy();
    });
    const req = httpMock.expectOne('/api/config/source-system/1');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1 });
  });

  /** Should issue POST request to create a new source system. */
  it('apiConfigSourceSystemPost should POST create', () => {
    const body: any = { name: 'sys' };
    service.apiConfigSourceSystemPost(body).subscribe((resp) => {
      expect(resp).toBeTruthy();
    });
    const req = httpMock.expectOne('/api/config/source-system');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 5, ...body });
  });

  /** Should issue PUT request to update a source system. */
  it('apiConfigSourceSystemIdPut should PUT update', () => {
    const body: any = { name: 'updated' };
    service.apiConfigSourceSystemIdPut(2, body).subscribe((resp) => {
      expect(resp).toBeTruthy();
    });
    const req = httpMock.expectOne('/api/config/source-system/2');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({});
  });

  /** Should issue DELETE request to remove a source system by id. */
  it('apiConfigSourceSystemIdDelete should DELETE id', () => {
    service.apiConfigSourceSystemIdDelete(9).subscribe((resp) => {
      expect(resp).toBeDefined();
    });
    const req = httpMock.expectOne('/api/config/source-system/9');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});


