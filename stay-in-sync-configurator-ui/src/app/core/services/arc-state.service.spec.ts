import { TestBed } from '@angular/core/testing';

import { ArcStateService } from './arc-state.service';

describe('ArcStateService', () => {
  let service: ArcStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ArcStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
