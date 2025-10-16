import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SnapshotService } from './snapshot.service';
import { ConfigService } from './config.service';
import { provideHttpClient } from '@angular/common/http';
import { SnapshotDTO } from '../models/snapshot.model';

describe('SnapshotService', () => {
  let service: SnapshotService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ConfigService,
        provideHttpClient(),
        provideHttpClientTesting()
      ],
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
    const mockSnapshot: SnapshotDTO = { snapshotId: '1', createdAt: '123456' };

    service.getLatestSnapshot('t1').subscribe(snapshot => {
      expect(snapshot).toEqual(mockSnapshot);
    });

    const req = httpMock.expectOne(r => r.url === '/api/snapshots/latest');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('transformationId')).toBe('t1');
    req.flush(mockSnapshot);
  });

  it('should fetch last five snapshots', () => {
    const mockSnapshots: SnapshotDTO[] = [
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

  it('should fetch snapshot by ID', () => {
    const mockSnapshot: SnapshotDTO = { snapshotId: '42', createdAt: '999999' };

    service.getById('42').subscribe(snapshot => {
      expect(snapshot).toEqual(mockSnapshot);
    });

    const req = httpMock.expectOne('/api/snapshots/42');
    expect(req.request.method).toBe('GET');
    req.flush(mockSnapshot);
  });
});
