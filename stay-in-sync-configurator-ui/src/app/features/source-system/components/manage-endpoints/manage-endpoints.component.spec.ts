import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ManageEndpointsComponent } from './manage-endpoints.component';
import { SourceSystemEndpointResourceService } from '../../../../generated/api/sourceSystemEndpointResource.service';
import { CreateSourceSystemEndpointDTO } from '../../../../generated/model/createSourceSystemEndpointDTO';
import { SourceSystemEndpointDTO } from '../../../../generated';
import { HttpEvent } from '@angular/common/http';

describe('ManageEndpointsComponent', () => {
  let component: ManageEndpointsComponent;
  let fixture: ComponentFixture<ManageEndpointsComponent>;
  let mockService: jasmine.SpyObj<SourceSystemEndpointResourceService>;

  beforeEach(waitForAsync(() => {
    mockService = jasmine.createSpyObj('SourceSystemEndpointResourceService', [
      'apiConfigSourceSystemSourceSystemIdEndpointGet',
      'apiConfigSourceSystemSourceSystemIdEndpointPost',
      'apiConfigSourceSystemEndpointIdDelete'
    ]);

    mockService.apiConfigSourceSystemSourceSystemIdEndpointGet.and.returnValue(of({ type: 0, body: [] } as HttpEvent<SourceSystemEndpointDTO[]>));
    mockService.apiConfigSourceSystemSourceSystemIdEndpointPost.and.returnValue(of({ type: 0 }));
    mockService.apiConfigSourceSystemEndpointIdDelete.and.returnValue(of({ type: 0 }));

    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule,
        RouterTestingModule,
        ManageEndpointsComponent // standalone
      ],
      providers: [
        { provide: SourceSystemEndpointResourceService, useValue: mockService }
      ]
    })
    .compileComponents()
    .then(() => {
      fixture = TestBed.createComponent(ManageEndpointsComponent);
      component = fixture.componentInstance;
      component.sourceSystemId = 42;
      fixture.detectChanges();
    });
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load endpoints on init', () => {
    expect(mockService.apiConfigSourceSystemSourceSystemIdEndpointGet).toHaveBeenCalledWith(42);
    expect(component.endpoints).toEqual([]);
  });

  it('addEndpoint() should post new endpoint', () => {
    component.endpointForm.patchValue({ endpointPath: '/foo', httpRequestType: 'GET' });
    component.addEndpoint();
    expect(mockService.apiConfigSourceSystemSourceSystemIdEndpointPost)
      .toHaveBeenCalledWith(42, [ jasmine.objectContaining({ endpointPath: '/foo' }) ]);
  });
});