import { TestBed } from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import { TransformationService } from './transformation.service';
import { TransformationModelForSnapshotPanel } from '../models/transformation.model';
import {ConfigService} from './config.service';
import {provideHttpClient} from '@angular/common/http';

describe('TransformationService', () => {
  let service: TransformationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService, provideHttpClient(),
        provideHttpClientTesting() ],
    });

    service = TestBed.inject(TransformationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch transformations without syncJobId', () => {
    const mockTransformations: TransformationModelForSnapshotPanel[] = [
      { id: 1, name: 'Transformation 1' },
      { id: 2, name: 'Transformation 2' },
    ];

    service.getTransformations().subscribe(transformations => {
      expect(transformations).toEqual(mockTransformations);
    });

    const req = httpMock.expectOne('/api/transformation/undefined');
    expect(req.request.method).toBe('GET');
    req.flush(mockTransformations);
  });

  it('should fetch transformations with syncJobId', () => {
    const mockTransformations: TransformationModelForSnapshotPanel[] = [
      { id: 3, name: 'Transformation 3' },
    ];

    service.getTransformations('job123').subscribe(transformations => {
      expect(transformations).toEqual(mockTransformations);
    });

    const req = httpMock.expectOne('/api/transformation/job123');
    expect(req.request.method).toBe('GET');
    req.flush(mockTransformations);
  });
});
