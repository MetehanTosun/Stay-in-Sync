import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';

import { ManageEndpointParamsComponent } from './manage-endpoint-params.component';
import { ApiEndpointQueryParamResourceService } from '../../service/apiEndpointQueryParamResource.service';
import { ApiEndpointQueryParamDTO } from '../../models/apiEndpointQueryParamDTO';


describe('ManageEndpointParamsComponent', () => {
  let component: ManageEndpointParamsComponent;
  let fixture: ComponentFixture<ManageEndpointParamsComponent>;
  let svc: jasmine.SpyObj<ApiEndpointQueryParamResourceService>;

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

  it('should create', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of([]));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('loads params on init', () => {
    const params: ApiEndpointQueryParamDTO[] = [{ id: 1, paramName: 'limit', queryParamType: 'QUERY' } as any];
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of(params));
    fixture.detectChanges();
    expect(component.queryParams).toEqual(params);
    expect(component.queryParamsLoading).toBeFalse();
  });

  it('adds param and emits onCreated', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of([]));
    svc.apiConfigEndpointEndpointIdQueryParamPost.and.returnValue(of({} as any));
    fixture.detectChanges();
    spyOn(component.onCreated, 'emit');

    component.queryParamForm.patchValue({ paramName: 'id', queryParamType: 'QUERY' });
    component.addQueryParam();

    expect(component.onCreated.emit).toHaveBeenCalled();
  });

  it('deletes param and emits onDeleted', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(of([{ id: 7, paramName: 'id', queryParamType: 'PATH' } as any]));
    svc.apiConfigEndpointQueryParamIdDelete.and.returnValue(of({} as any));
    fixture.detectChanges();
    spyOn(component.onDeleted, 'emit');

    component.deleteQueryParam(7);

    expect(component.onDeleted.emit).toHaveBeenCalled();
  });

  it('handles load error gracefully', () => {
    svc.apiConfigEndpointEndpointIdQueryParamGet.and.returnValue(throwError(() => new Error('fail')));
    fixture.detectChanges();
    expect(component.queryParamsLoading).toBeFalse();
  });
});
