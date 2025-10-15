import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ManageEndpointsFormService } from './manage-endpoints-form.service';
import { SourceSystemEndpointResourceService } from '../service/sourceSystemEndpointResource.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { MessageService } from 'primeng/api';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';

describe('ManageEndpointsFormService', () => {
  let service: ManageEndpointsFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, HttpClientTestingModule],
      providers: [
        ManageEndpointsFormService,
        { provide: SourceSystemEndpointResourceService, useValue: {
          apiConfigSourceSystemSourceSystemIdEndpointGet: () => of([]),
          apiConfigSourceSystemSourceSystemIdEndpointPost: () => of([])
        } },
        { provide: HttpErrorService, useValue: { handleError: () => {} } },
        MessageService
      ]
    });
    service = TestBed.inject(ManageEndpointsFormService);
  });

  it('creates endpoint form with validators', () => {
    const form = service.createEndpointForm();
    form.patchValue({ endpointPath: '/pets', httpRequestType: 'GET' });
    expect(form.valid).toBeTrue();
  });

  it('pathValidator requires leading slash', () => {
    const form = service.createEndpointForm();
    form.patchValue({ endpointPath: 'pets' });
    expect(form.get('endpointPath')?.errors).toBeTruthy();
  });

  it('jsonValidator allows empty and valid JSON', () => {
    const form = service.createEndpointForm();
    form.patchValue({ requestBodySchema: '' });
    expect(form.get('requestBodySchema')?.errors).toBeNull();
    form.patchValue({ requestBodySchema: '{"a":1}' });
    expect(form.get('requestBodySchema')?.errors).toBeNull();
  });

  it('extractPathParams extracts names', () => {
    const params = service.extractPathParams('/pets/{id}/{name}');
    expect(params).toEqual(['id', 'name']);
  });
});


