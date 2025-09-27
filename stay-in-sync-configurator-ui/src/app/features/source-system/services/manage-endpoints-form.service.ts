import { Injectable } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Observable } from 'rxjs';
import { SourceSystemEndpointDTO } from '../models/sourceSystemEndpointDTO';
import { ApiEndpointQueryParamType } from '../models/apiEndpointQueryParamType';
import { SourceSystemEndpointResourceService } from '../service/sourceSystemEndpointResource.service';
import { HttpErrorService } from '../../../core/services/http-error.service';

@Injectable({
  providedIn: 'root'
})
export class ManageEndpointsFormService {

  constructor(
    private fb: FormBuilder,
    private endpointService: SourceSystemEndpointResourceService,
    private errorService: HttpErrorService
  ) {}

  /**
   * Create form for endpoint creation/editing
   */
  createEndpointForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(1)]],
      path: ['', [Validators.required, this.pathValidator()]],
      method: ['GET', Validators.required],
      description: [''],
      requestBodyRequired: [false],
      requestBodySchema: [''],
      responseSchema: [''],
      queryParams: this.fb.array([]),
      pathParams: this.fb.array([])
    });
  }

  /**
   * Create edit form for existing endpoint
   */
  createEditForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(1)]],
      path: ['', [Validators.required, this.pathValidator()]],
      method: ['GET', Validators.required],
      description: [''],
      requestBodyRequired: [false],
      requestBodySchema: [''],
      responseSchema: [''],
      queryParams: this.fb.array([]),
      pathParams: this.fb.array([])
    });
  }

  /**
   * Populate form with endpoint data
   */
  populateForm(form: FormGroup, endpoint: SourceSystemEndpointDTO): void {
    form.patchValue({
      name: endpoint.name,
      path: endpoint.path,
      method: endpoint.method,
      description: endpoint.description,
      requestBodyRequired: endpoint.requestBodyRequired,
      requestBodySchema: endpoint.requestBodySchema,
      responseSchema: endpoint.responseSchema
    });

    // TODO: Handle query params and path params arrays
  }

  /**
   * Get form data for submission
   */
  getFormData(form: FormGroup): Partial<SourceSystemEndpointDTO> {
    const formValue = form.value;
    return {
      name: formValue.name,
      path: formValue.path,
      method: formValue.method,
      description: formValue.description,
      requestBodyRequired: formValue.requestBodyRequired,
      requestBodySchema: formValue.requestBodySchema,
      responseSchema: formValue.responseSchema,
      queryParams: formValue.queryParams || [],
      pathParams: formValue.pathParams || []
    };
  }

  /**
   * Get available HTTP methods
   */
  getHttpMethods(): Array<{label: string, value: string}> {
    return [
      { label: 'GET', value: 'GET' },
      { label: 'POST', value: 'POST' },
      { label: 'PUT', value: 'PUT' },
      { label: 'DELETE', value: 'DELETE' },
      { label: 'PATCH', value: 'PATCH' },
      { label: 'HEAD', value: 'HEAD' },
      { label: 'OPTIONS', value: 'OPTIONS' }
    ];
  }

  /**
   * Get available query param types
   */
  getQueryParamTypes(): Array<{label: string, value: ApiEndpointQueryParamType}> {
    return [
      { label: 'String', value: ApiEndpointQueryParamType.STRING },
      { label: 'Number', value: ApiEndpointQueryParamType.NUMBER },
      { label: 'Boolean', value: ApiEndpointQueryParamType.BOOLEAN },
      { label: 'Array', value: ApiEndpointQueryParamType.ARRAY }
    ];
  }

  /**
   * Load endpoints for a source system
   */
  loadEndpoints(sourceSystemId: number): Observable<SourceSystemEndpointDTO[]> {
    return this.endpointService.getEndpointsBySourceSystemId(sourceSystemId);
  }

  /**
   * Create new endpoint
   */
  createEndpoint(sourceSystemId: number, endpointData: Partial<SourceSystemEndpointDTO>): Observable<SourceSystemEndpointDTO> {
    const endpoint: Partial<SourceSystemEndpointDTO> = {
      ...endpointData,
      sourceSystemId: sourceSystemId
    };
    return this.endpointService.createEndpoint(endpoint as SourceSystemEndpointDTO);
  }

  /**
   * Update existing endpoint
   */
  updateEndpoint(endpoint: SourceSystemEndpointDTO): Observable<SourceSystemEndpointDTO> {
    return this.endpointService.updateEndpoint(endpoint);
  }

  /**
   * Delete endpoint
   */
  deleteEndpoint(endpointId: number): Observable<void> {
    return this.endpointService.deleteEndpoint(endpointId);
  }

  /**
   * Custom validator for path format
   */
  private pathValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!value) return null;

      // Path should start with /
      if (!value.startsWith('/')) {
        return { pathFormat: { message: 'Path must start with /' } };
      }

      // Check for valid path parameter format {param}
      const pathParamRegex = /\{[a-zA-Z_][a-zA-Z0-9_]*\}/g;
      const invalidParams = value.match(/\{[^}]*\}/g)?.filter((param: string) => 
        !pathParamRegex.test(param)
      );

      if (invalidParams && invalidParams.length > 0) {
        return { pathFormat: { message: 'Invalid path parameter format' } };
      }

      return null;
    };
  }

  /**
   * Reset form to initial state
   */
  resetForm(form: FormGroup): void {
    form.reset({
      name: '',
      path: '',
      method: 'GET',
      description: '',
      requestBodyRequired: false,
      requestBodySchema: '',
      responseSchema: '',
      queryParams: [],
      pathParams: []
    });
  }

  /**
   * Validate JSON schema string
   */
  validateJsonSchema(schema: string): boolean {
    if (!schema || schema.trim() === '') return true;

    try {
      JSON.parse(schema);
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Get validation error message for a form control
   */
  getValidationError(control: AbstractControl | null): string | null {
    if (!control || !control.errors) return null;

    if (control.errors['required']) return 'This field is required';
    if (control.errors['minlength']) return `Minimum length is ${control.errors['minlength'].requiredLength}`;
    if (control.errors['pathFormat']) return control.errors['pathFormat'].message;

    return 'Invalid input';
  }

  /**
   * Check if form is valid for submission
   */
  isFormValid(form: FormGroup): boolean {
    return form.valid && this.validateRequiredSchemas(form);
  }

  /**
   * Validate that required schemas are provided and valid
   */
  private validateRequiredSchemas(form: FormGroup): boolean {
    const requestBodyRequired = form.get('requestBodyRequired')?.value;
    const requestBodySchema = form.get('requestBodySchema')?.value;
    const responseSchema = form.get('responseSchema')?.value;

    // If request body is required, schema must be valid
    if (requestBodyRequired && !this.validateJsonSchema(requestBodySchema)) {
      return false;
    }

    // Response schema should be valid if provided
    if (responseSchema && !this.validateJsonSchema(responseSchema)) {
      return false;
    }

    return true;
  }

  /**
   * Extract path parameters from path string
   */
  extractPathParams(path: string): string[] {
    const pathParamRegex = /\{([a-zA-Z_][a-zA-Z0-9_]*)\}/g;
    const params: string[] = [];
    let match;

    while ((match = pathParamRegex.exec(path)) !== null) {
      params.push(match[1]);
    }

    return params;
  }

  /**
   * Generate example request body from schema
   */
  generateExampleFromSchema(schema: string): any {
    try {
      const parsedSchema = JSON.parse(schema);
      return this.generateExampleFromParsedSchema(parsedSchema);
    } catch (error) {
      return {};
    }
  }

  /**
   * Generate example from parsed schema object
   */
  private generateExampleFromParsedSchema(schema: any): any {
    if (!schema || typeof schema !== 'object') return {};

    if (schema.type === 'object' && schema.properties) {
      const example: any = {};
      for (const [key, prop] of Object.entries(schema.properties)) {
        example[key] = this.generateExampleFromParsedSchema(prop);
      }
      return example;
    }

    if (schema.type === 'array' && schema.items) {
      return [this.generateExampleFromParsedSchema(schema.items)];
    }

    // Generate example values based on type
    switch (schema.type) {
      case 'string':
        return schema.example || 'string';
      case 'number':
      case 'integer':
        return schema.example || 0;
      case 'boolean':
        return schema.example || false;
      default:
        return schema.example || null;
    }
  }
}
