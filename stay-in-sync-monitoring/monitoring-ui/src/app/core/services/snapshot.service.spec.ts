import { TestBed } from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import { SnapshotService } from './snapshot.service';
import { SnapshotModel } from '../models/snapshot.model';
import {ConfigService} from './config.service';
import {provideHttpClient} from '@angular/common/http';

describe('SnapshotService', () => {
  let service: SnapshotService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService, provideHttpClient(),
        provideHttpClientTesting() ],
    });

    service = TestBed.inject(SnapshotService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch latest snapshot', () => {
    const mockSnapshot: SnapshotModel = { snapshotId: '1', createdAt: '123456' };

    service.getLatestSnapshot('t1').subscribe(snapshot => {
      expect(snapshot).toEqual(mockSnapshot);
    });

    const req = httpMock.expectOne(r => r.url === '/api/snapshots/latest');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('transformationId')).toBe('t1');
    req.flush(mockSnapshot);
  });

  it('should fetch last five snapshots', () => {
    const mockSnapshots: SnapshotModel[] = [
      { snapshotId: '1', createdAt: '123' },
      { snapshotId: '2', createdAt: '124' },
    ];

    service.getLastFiveSnapshots('t2').subscribe(snapshots => {
      expect(snapshots).toEqual(mockSnapshots);
    });

    const req = httpMock.expectOne(r => r.url === '/api/snapshots/list');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('transformationId')).toBe('t2');
    req.flush(mockSnapshots);
  });
});
