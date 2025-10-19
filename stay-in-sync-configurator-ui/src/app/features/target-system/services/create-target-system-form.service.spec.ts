import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { CreateTargetSystemFormService } from './create-target-system-form.service';

/** Unit tests for CreateTargetSystemFormService ensuring correct form initialization, validation, and data handling for target system creation. */
describe('CreateTargetSystemFormService', () => {
  let service: CreateTargetSystemFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ReactiveFormsModule] });
    service = TestBed.inject(CreateTargetSystemFormService);
  });

  /**
   * Verifies that the service is created successfully.
   */
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * Verifies that the form is created with all expected controls and default values.
   */
  it('createForm should build expected controls', () => {
    const form = service.createForm();
    expect(form.get('name')).toBeTruthy();
    expect(form.get('apiUrl')).toBeTruthy();
    expect(form.get('apiType')?.value).toBe('REST_OPENAPI');
    expect(form.get('authConfig')).toBeTruthy();
  });

  /**
   * Checks that setupFormSubscriptions toggles validators and control states based on apiType.
   */
  it('setupFormSubscriptions should toggle validators for AAS and REST', () => {
    const form = service.createForm();
    service.setupFormSubscriptions(form);

    form.get('apiType')?.setValue('AAS');
    expect(form.get('aasId')?.validator).toBeTruthy();
    expect(form.get('openApiSpec')?.disabled).toBeTrue();

    form.get('apiType')?.setValue('REST_OPENAPI');
    expect(form.get('openApiSpec')?.disabled).toBeFalse();
  });

  /**
   * Ensures getSteps returns non-empty steps arrays for both AAS and REST apiTypes.
   */
  it('getSteps should return steps for AAS and REST', () => {
    expect(service.getSteps('AAS').length).toBeGreaterThan(0);
    expect(service.getSteps('REST_OPENAPI').length).toBeGreaterThan(0);
  });

  /**
   * Verifies resetForm resets apiType to default and enables openApiSpec control.
   */
  it('resetForm should reset apiType and enable openApiSpec', () => {
    const form = service.createForm();
    form.get('openApiSpec')?.disable();
    service.resetForm(form);
    expect(form.get('apiType')?.value).toBe('REST_OPENAPI');
    expect(form.get('openApiSpec')?.disabled).toBeFalse();
  });

  /**
   * Tests isFormValidForStep validation logic for different apiTypes and aasTestOk flags.
   */
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

  /**
   * Checks that getFormDataForSubmission correctly handles openAPI spec string without file upload.
   */
  it('getFormDataForSubmission should include openAPI when string provided and no file', () => {
    const form = service.createForm();
    form.patchValue({ name: 'n', apiUrl: 'https://x', openApiSpec: '{"openapi":"3.0.0"}' });
    const result = service.getFormDataForSubmission(form, null);
    expect(result.hasFile).toBeFalse();
    expect(result.base.openAPI).toContain('openapi');
    expect(result.base.openApiSpec).toBeUndefined();
  });

  /**
   * Validates that getFormDataForSubmission returns hasFile=true when a file is provided.
   */
  it('getFormDataForSubmission should return hasFile=true when file provided', () => {
    const form = service.createForm();
    const file = new File(['{}'], 'openapi.json', { type: 'application/json' });
    const result = service.getFormDataForSubmission(form, file);
    expect(result.hasFile).toBeTrue();
  });
});
