import { TestBed } from '@angular/core/testing';

import { SyncJobService } from './sync-job.service';

describe('SyncJobService', () => {
  let service: SyncJobService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SyncJobService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
