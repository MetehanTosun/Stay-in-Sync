import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import { ReplayService } from './replay.service';
import {
  ReplayExecuteRequestDTO,
  ReplayExecuteResponseDTO,
} from '../../models/replay.model';
import {ConfigService} from '../config.service';
import {provideHttpClient} from '@angular/common/http';

describe('ReplayService', () => {
  let service: ReplayService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService, provideHttpClient(),
        provideHttpClientTesting() ],
    });

    service = TestBed.inject(ReplayService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should execute replay and return response', () => {
    const dto: ReplayExecuteRequestDTO = { scriptName: 'abc123' , javascriptCode: 'code', sourceData: { key: 'value' } };
    const mockResponse: ReplayExecuteResponseDTO = { errorInfo: 'OK', outputData: 'abc123', variables: { data: 'data' } };

    service.executeReplay(dto).subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('api/replay/execute');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);
    req.flush(mockResponse);
  });

  it('should handle backend error properly', () => {
    const dto: ReplayExecuteRequestDTO = { scriptName: 'abc123', javascriptCode: 'code', sourceData: { key: 'value' } };

    service.executeReplay(dto).subscribe({
      next: () => fail('Expected request to fail'),
      error: (err) => {
        expect(err.status).toBe(500);
      }
    });

    const req = httpMock.expectOne('api/replay/execute');
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });
});
