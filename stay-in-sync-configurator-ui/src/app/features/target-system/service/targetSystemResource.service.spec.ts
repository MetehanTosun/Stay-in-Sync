import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TargetSystemResourceService } from './targetSystemResource.service';
import { TargetSystemDTO } from '../models/targetSystemDTO';

/**
 * Unit tests for {@link TargetSystemResourceService}.
 * Verifies CRUD operations for Target Systems using the Angular HttpClientTestingModule.
 * Ensures that correct HTTP methods, URLs, and request bodies are used for each operation.
 */
describe('TargetSystemResourceService', () => {
  let service: TargetSystemResourceService;
  let httpMock: HttpTestingController;

  /**
   * Sets up the testing environment before each test case.
   * Initializes the service and the HttpTestingController for HTTP request mocking.
   */
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(TargetSystemResourceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  /**
   * Verifies that no unexpected HTTP requests remain after each test.
   */
  afterEach(() => {
    httpMock.verify();
  });

  /**
   * Verifies that the TargetSystemResourceService is successfully created and injected.
   */
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * Tests that the getAll method performs a GET request to retrieve all target systems.
   * Verifies that the returned response matches the expected mock data.
   */
  it('getAll should GET target systems', () => {
    const mock: TargetSystemDTO[] = [] as any;
    service.getAll().subscribe(res => expect(res).toEqual(mock));
    const req = httpMock.expectOne('/api/config/target-systems');
    expect(req.request.method).toBe('GET');
    req.flush(mock);
  });

  /**
   * Tests that the getById method performs a GET request with the correct ID parameter.
   * Verifies that the response body contains the expected TargetSystemDTO object.
   */
  it('getById should GET with id', () => {
    const dto = { id: 1 } as TargetSystemDTO;
    service.getById(1).subscribe(res => expect(res).toEqual(dto));
    const req = httpMock.expectOne('/api/config/target-systems/1');
    expect(req.request.method).toBe('GET');
    req.flush(dto);
  });

  /**
   * Tests that the create method performs a POST request with the provided DTO in the body.
   * Verifies that the response matches the expected object.
   */
  it('create should POST dto', () => {
    const dto = { id: 1 } as TargetSystemDTO;
    service.create(dto).subscribe(res => expect(res).toEqual(dto));
    const req = httpMock.expectOne('/api/config/target-systems');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);
    req.flush(dto);
  });

  /**
   * Tests that the update method performs a PUT request with the correct ID and payload.
   * Ensures that the response body contains the updated DTO.
   */
  it('update should PUT dto with id', () => {
    const dto = { id: 2 } as TargetSystemDTO;
    service.update(2, dto).subscribe(res => expect(res).toEqual(dto));
    const req = httpMock.expectOne('/api/config/target-systems/2');
    expect(req.request.method).toBe('PUT');
    req.flush(dto);
  });

  /**
   * Tests that the delete method performs a DELETE request with the specified ID.
   * Verifies that a valid response is received upon successful deletion.
   */
  it('delete should DELETE by id', () => {
    service.delete(3).subscribe(res => expect(res).toBeDefined());
    const req = httpMock.expectOne('/api/config/target-systems/3');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
