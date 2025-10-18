import { TestBed } from '@angular/core/testing';
import { ArcAPIService } from './arc-api.service';


describe('ArcAPIService', () => {
  let service: ArcAPIService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ArcAPIService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
