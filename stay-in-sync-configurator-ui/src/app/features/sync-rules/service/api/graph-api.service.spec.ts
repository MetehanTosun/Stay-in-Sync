import { TestBed } from '@angular/core/testing';

import { GraphAPIService } from './graph-api.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

describe('GraphAPIService', () => {
  let service: GraphAPIService;
  let httpTestingController: HttpTestingController;
  const apiUrl = '/api/config/transformation-rule';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        GraphAPIService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(GraphAPIService);
    httpTestingController = TestBed.inject(HttpTestingController)
  });

  afterEach(() => {
    httpTestingController.verify();
  })

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
