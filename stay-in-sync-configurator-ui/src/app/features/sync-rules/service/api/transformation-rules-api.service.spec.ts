import { TestBed } from '@angular/core/testing';
import { TransformationRulesApiService } from './transformation-rules-api.service';


describe('TransformationRulesApiService', () => {
  let service: TransformationRulesApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TransformationRulesApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
// sTODO Tests
