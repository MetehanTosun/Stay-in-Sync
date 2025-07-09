import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { CreateSourceSystemComponent } from './create-source-system.component';
import { ReactiveFormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { StepsModule } from 'primeng/steps';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';
import { of, throwError } from 'rxjs';
import { SourceSystemResourceService } from '../../generated/api/sourceSystemResource.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

describe('CreateSourceSystemComponent', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let mockService: jasmine.SpyObj<SourceSystemResourceService>;

  beforeEach(async () => {
    mockService = jasmine.createSpyObj('SourceSystemResourceService', ['apiConfigSourceSystemPost']);

    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        DialogModule,
        StepsModule,
        DropdownModule,
        InputTextModule,
        ButtonModule,
        TextareaModule,
        CreateSourceSystemComponent
      ],
      providers: [
        { provide: SourceSystemResourceService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create form with default values', () => {
    const f = component.form;
    expect(f).toBeTruthy();
    expect(f.get('apiType')!.value).toBe('REST_OPENAPI');
    expect(f.get('apiAuthType')!.value).toBe('NONE');
  });

  it('should not call API if form invalid', () => {
    component.form.patchValue({ name: '', apiUrl: '' });
    component.save();
    expect(mockService.apiConfigSourceSystemPost).not.toHaveBeenCalled();
  });

  it('should extract ID from Location header and advance step', fakeAsync(() => {
    // valid form
    component.form.patchValue({ name: 'X', apiUrl: 'http://foo', apiType: 'REST_OPENAPI', apiAuthType: 'NONE' });
    const headers = new HttpHeaders({ 'Location': 'http://localhost/api/config/source-system/123' });
    const resp = new HttpResponse<void>({ headers, status: 201 });
    mockService.apiConfigSourceSystemPost.and.returnValue(of(resp));
    component.save();
    // file handling async disabled, so directly tick
    tick();
    expect(component['createdSourceSystemId']).toBe(123);
    expect(component['currentStep']).toBe(1);
  }));

  it('should log error if no Location header', fakeAsync(() => {
    spyOn(console, 'error');
    component.form.patchValue({ name: 'X', apiUrl: 'http://foo', apiType: 'REST_OPENAPI', apiAuthType: 'NONE' });
    const resp = new HttpResponse<void>({ headers: new HttpHeaders(), status: 201 });
    mockService.apiConfigSourceSystemPost.and.returnValue(of(resp));
    component.save();
    tick();
    expect(console.error).toHaveBeenCalledWith('No Location header returned');
  }));
});