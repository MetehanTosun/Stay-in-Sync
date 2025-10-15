/** Unit tests for `ManageEndpointParamsComponent`. */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';

import { ManageEndpointParamsComponent } from './manage-endpoint-params.component';
import { ApiEndpointQueryParamResourceService } from '../../service/apiEndpointQueryParamResource.service';
import { ApiEndpointQueryParamDTO } from '../../models/apiEndpointQueryParamDTO';


/** Verifies listing, creation, deletion and error handling for endpoint params. */
describe('ManageEndpointParamsComponent', () => {
  let component: ManageEndpointParamsComponent;
  let fixture: ComponentFixture<ManageEndpointParamsComponent>;
  let svc: jasmine.SpyObj<ApiEndpointQueryParamResourceService>;

  /** Configure TestBed and initialize the component and service spies. */
  beforeEach(async () => {
    svc = jasmine.createSpyObj('ApiEndpointQueryParamResourceService', [
      'apiConfigEndpointEndpointIdQueryParamGet',
      'apiConfigEndpointEndpointIdQueryParamPost',
      'apiConfigEndpointQueryParamIdDelete'
    ]);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ManageEndpointParamsComponent],
      providers: [MessageService, { provide: ApiEndpointQueryParamResourceService, useValue: svc }]
    }).compileComponents();

    fixture = TestBed.createComponent(ManageEndpointParamsComponent);
    component = fixture.componentInstance;
    component.endpointId = 5;
  });

  /** Should instantiate and render with empty params. */
  it('should create', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of([] as any));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  /** Loads parameters on initialization. */
  it('loads params on init', () => {
    const params: ApiEndpointQueryParamDTO[] = [{ id: 1, paramName: 'limit', queryParamType: 'QUERY' } as any];
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of(params as any));
    fixture.detectChanges();
    expect(component.queryParams).toEqual(params);
    expect(component.queryParamsLoading).toBeFalse();
  });

  /** Adds a parameter and emits onCreated. */
  it('adds param and emits onCreated', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of([] as any));
    svc.apiConfigEndpointEndpointIdQueryParamPost.and.returnValue(of({} as any));
    fixture.detectChanges();
    spyOn(component.onCreated, 'emit');

    component.queryParamForm.patchValue({ paramName: 'id', queryParamType: 'QUERY' });
    component.addQueryParam();

    expect(component.onCreated.emit).toHaveBeenCalled();
  });

  /** Deletes a parameter and emits onDeleted. */
  it('deletes param and emits onDeleted', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of([{ id: 7, paramName: 'id', queryParamType: 'PATH' } as any] as any));
    svc.apiConfigEndpointQueryParamIdDelete.and.returnValue(of({} as any));
    fixture.detectChanges();
    spyOn(component.onDeleted, 'emit');

    component.deleteQueryParam(7);

    expect(component.onDeleted.emit).toHaveBeenCalled();
  });

  /** Gracefully handles load errors and clears loading state. */
  it('handles load error gracefully', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(throwError(() => new Error('fail')));
    fixture.detectChanges();
    expect(component.queryParamsLoading).toBeFalse();
  });
});
