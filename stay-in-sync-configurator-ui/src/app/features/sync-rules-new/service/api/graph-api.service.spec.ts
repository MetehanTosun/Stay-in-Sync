import { TestBed } from '@angular/core/testing';

import { GraphAPIService } from './graph-api.service';

describe('GraphAPIService', () => {
  let service: GraphAPIService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GraphAPIService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
// sTODO Tests
