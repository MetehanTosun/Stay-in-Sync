import { TestBed } from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import { ConfigService } from './config.service';
import {provideHttpClient} from '@angular/common/http';

describe('ConfigService', () => {
  let service: ConfigService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService, provideHttpClient(),
        provideHttpClientTesting() ],
    });

    service = TestBed.inject(ConfigService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch Grafana base URL', async () => {
    const mockUrl = 'http://localhost:3000/grafana';

    const promise = service.getGrafanaBaseUrl();

    const req = httpMock.expectOne('/api/config/grafanaUrl');
    expect(req.request.method).toBe('GET');
    req.flush(mockUrl);

    const result = await promise;
    expect(result).toBe(mockUrl);
  });

  it('should throw an error if request fails', async () => {
    const promise = service.getGrafanaBaseUrl();

    const req = httpMock.expectOne('/api/config/grafanaUrl');
    req.flush('Error fetching URL', { status: 500, statusText: 'Server Error' });

    await expectAsync(promise).toBeRejected();
  });
});
