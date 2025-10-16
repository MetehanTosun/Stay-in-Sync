import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { CreateTargetSystemFormService } from './create-target-system-form.service';

describe('CreateTargetSystemFormService', () => {
  let service: CreateTargetSystemFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ReactiveFormsModule] });
    service = TestBed.inject(CreateTargetSystemFormService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('createForm should build expected controls', () => {
    const form = service.createForm();
    expect(form.get('name')).toBeTruthy();
    expect(form.get('apiUrl')).toBeTruthy();
    expect(form.get('apiType')?.value).toBe('REST_OPENAPI');
    expect(form.get('authConfig')).toBeTruthy();
  });

  it('setupFormSubscriptions should toggle validators for AAS and REST', () => {
    const form = service.createForm();
    service.setupFormSubscriptions(form);

    form.get('apiType')?.setValue('AAS');
    expect(form.get('aasId')?.validator).toBeTruthy();
    expect(form.get('openApiSpec')?.disabled).toBeTrue();

    form.get('apiType')?.setValue('REST_OPENAPI');
    expect(form.get('openApiSpec')?.disabled).toBeFalse();
  });

  it('getSteps should return steps for AAS and REST', () => {
    expect(service.getSteps('AAS').length).toBeGreaterThan(0);
    expect(service.getSteps('REST_OPENAPI').length).toBeGreaterThan(0);
  });

  it('resetForm should reset apiType and enable openApiSpec', () => {
    const form = service.createForm();
    form.get('openApiSpec')?.disable();
    service.resetForm(form);
    expect(form.get('apiType')?.value).toBe('REST_OPENAPI');
    expect(form.get('openApiSpec')?.disabled).toBeFalse();
  });

  it('isFormValidForStep should validate based on apiType and aasTestOk', () => {
    const form = service.createForm();
    form.get('name')?.setValue('x');
    form.get('apiUrl')?.setValue('https://x');

    expect(service.isFormValidForStep(form, 0, 'REST_OPENAPI')).toBeTrue();
    expect(service.isFormValidForStep(form, 1, 'REST_OPENAPI')).toBeTrue();

    form.get('apiType')?.setValue('AAS');
    form.get('aasId')?.setValue('id');
    expect(service.isFormValidForStep(form, 0, 'AAS', true)).toBeTrue();
    expect(service.isFormValidForStep(form, 0, 'AAS', false)).toBeFalse();
  });

  it('getFormDataForSubmission should include openAPI when string provided and no file', () => {
    const form = service.createForm();
    form.patchValue({ name: 'n', apiUrl: 'https://x', openApiSpec: '{"openapi":"3.0.0"}' });
    const result = service.getFormDataForSubmission(form, null);
    expect(result.hasFile).toBeFalse();
    expect(result.base.openAPI).toContain('openapi');
    expect(result.base.openApiSpec).toBeUndefined();
  });

  it('getFormDataForSubmission should return hasFile=true when file provided', () => {
    const form = service.createForm();
    const file = new File(['{}'], 'openapi.json', { type: 'application/json' });
    const result = service.getFormDataForSubmission(form, file);
    expect(result.hasFile).toBeTrue();
  });
});
