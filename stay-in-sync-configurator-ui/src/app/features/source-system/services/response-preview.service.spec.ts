import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { MessageService } from 'primeng/api';
import { ResponsePreviewService } from './response-preview.service';

describe('ResponsePreviewService', () => {
  let service: ResponsePreviewService;
  let httpMock: HttpTestingController;

  const endpoint: any = { endpointPath: '/pets/{id}', httpRequestType: 'GET' };
  const sourceSystem: any = { apiUrl: 'https://api.example.com' };

  beforeEach(() => {
    TestBed.configureTestingModule({ 
      imports: [HttpClientTestingModule],
      providers: [
        { provide: HttpErrorService, useValue: { handleError: () => {} } },
        MessageService
      ]
    });
    service = TestBed.inject(ResponsePreviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('buildApiUrl replaces path params and joins with base', () => {
    const url = service.buildApiUrl(sourceSystem, endpoint, { id: 5 });
    expect(url).toBe('https://api.example.com/pets/5');
  });

  it('validateRequest requires endpoint and sourceSystem', () => {
    const res = service.validateRequest({} as any);
    expect(res.valid).toBeFalse();
    expect(res.errors.length).toBeGreaterThan(0);
  });

  it('formatResponse pretty prints JSON strings', () => {
    const formatted = service.formatResponse('{"a":1}');
    expect(formatted).toContain('\n');
  });

  it('testEndpoint updates preview data on success', () => {
    const req = { endpoint, sourceSystem } as any;
    service.testEndpoint(req);
    const http = httpMock.expectOne('https://api.example.com/pets/{id}');
    http.flush({ ok: true });
    const current = service.getCurrentPreviewData();
    expect(current?.isLoading).toBeFalse();
    expect(current?.response).toBeTruthy();
  });
});


