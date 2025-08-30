import { TestBed } from '@angular/core/testing';

import { OperatorNodesApiService } from './operator-nodes-api.service';

describe('OperatorNodesService', () => {
  let service: OperatorNodesApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(OperatorNodesApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
