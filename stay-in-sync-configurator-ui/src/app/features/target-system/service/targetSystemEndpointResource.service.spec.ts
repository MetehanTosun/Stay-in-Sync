import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TargetSystemEndpointResourceService } from './targetSystemEndpointResource.service';
import { TargetSystemEndpointDTO } from '../models/targetSystemEndpointDTO';
import { CreateTargetSystemEndpointDTO } from '../models/createTargetSystemEndpointDTO';

describe('TargetSystemEndpointResourceService', () => {
  let service: TargetSystemEndpointResourceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(TargetSystemEndpointResourceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('list should GET endpoints', () => {
    const mock: TargetSystemEndpointDTO[] = [] as any;
    service.list(1).subscribe(res => expect(res).toEqual(mock));
    const req = httpMock.expectOne('/api/config/target-systems/1/endpoints');
    expect(req.request.method).toBe('GET');
    req.flush(mock);
  });

  it('create should POST endpoint array', () => {
    const payload: CreateTargetSystemEndpointDTO[] = [] as any;
    const mock: TargetSystemEndpointDTO[] = [] as any;
    service.create(1, payload).subscribe(res => expect(res).toEqual(mock));
    const req = httpMock.expectOne('/api/config/target-systems/1/endpoints');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mock);
  });

  it('getById should GET endpoint', () => {
    const dto = { id: 2 } as TargetSystemEndpointDTO;
    service.getById(2).subscribe(res => expect(res).toEqual(dto));
    const req = httpMock.expectOne('/api/config/target-systems/endpoints/2');
    expect(req.request.method).toBe('GET');
    req.flush(dto);
  });

  it('replace should PUT endpoint', () => {
    const dto = { id: 3 } as TargetSystemEndpointDTO;
    service.replace(3, dto).subscribe(res => expect(res).toBeDefined());
    const req = httpMock.expectOne('/api/config/target-systems/endpoints/3');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(dto);
    req.flush({});
  });

  it('delete should DELETE endpoint', () => {
    service.delete(4).subscribe(res => expect(res).toBeDefined());
    const req = httpMock.expectOne('/api/config/target-systems/endpoints/4');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('generateTypeScript should POST generation request', () => {
    const request = { jsonSchema: '{}' } as any;
    service.generateTypeScript(5, request).subscribe(res => expect(res).toBeTruthy());
    const req = httpMock.expectOne('/api/config/target-systems/endpoints/5/generate-typescript');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ generatedTypeScript: 'interface X {}' });
  });
});
