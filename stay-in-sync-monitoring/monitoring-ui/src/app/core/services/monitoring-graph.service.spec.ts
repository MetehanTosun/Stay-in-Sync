import { TestBed } from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import { MonitoringGraphService } from './monitoring-graph.service';
import {ConfigService} from './config.service';
import {provideHttpClient} from '@angular/common/http';

describe('MonitoringGraphService', () => {
  let service: MonitoringGraphService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService, provideHttpClient(),
        provideHttpClientTesting() ],
    });

    service = TestBed.inject(MonitoringGraphService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch monitoring graph data successfully', () => {
    const mockData = { nodes: [], edges: [] };

    service.getMonitoringGraphData().subscribe(data => {
      expect(data).toEqual(mockData);
    });

    const req = httpMock.expectOne('/api/monitoringgraph');
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });

  it('should handle errors and return user-friendly error', () => {
    service.getMonitoringGraphData().subscribe({
      next: () => fail('Expected error, but got success'),
      error: (err) => {
        expect(err).toBeTruthy();
        expect(err.message).toBe('Monitoring graph could not be loaded.');
      }
    });

    const req = httpMock.expectOne('/api/monitoringgraph');
    req.flush('Server error', { status: 500, statusText: 'Server Error' });
  });
});
