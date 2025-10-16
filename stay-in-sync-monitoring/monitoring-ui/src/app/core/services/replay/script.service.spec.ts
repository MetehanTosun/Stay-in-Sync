import { TestBed } from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import { ScriptService } from './script.service';
import { ConfigService } from '../config.service';
import { TransformationScriptDTO } from '../../models/transformation-script.model';
import {provideHttpClient} from '@angular/common/http';

describe('ScriptService', () => {
  let service: ScriptService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService, provideHttpClient(),
        provideHttpClientTesting() ],
    });

    service = TestBed.inject(ScriptService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch transformation script by ID', () => {
    const transformationId = 42;
    const mockResponse: TransformationScriptDTO = {
      id: transformationId,
      name: 'Test Transformation',
      javascriptCode: 'console.log("Hello World")',
      targetArcIds: [1, 2, 3],
      typescriptCode: 'let x: number = 42;',
      requiredArcAliases: ['alias1', 'alias2'],
      status: 'active',
    };

    service.getByTransformationId(transformationId).subscribe((result) => {
      expect(result).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/replay/' + transformationId);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should handle HTTP error response', () => {
    const transformationId = '99';

    service.getByTransformationId(transformationId).subscribe({
      next: () => fail('Expected error response'),
      error: (error) => {
        expect(error.status).toBe(404);
        expect(error.statusText).toBe('Not Found');
      },
    });

    const req = httpMock.expectOne('/api/replay/' + transformationId);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });
});
