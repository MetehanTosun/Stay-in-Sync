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
      endpointPath: ['', [Validators.required, this.pathValidator()]],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: [''],
      responseBodySchema: ['']
    });
  }

  /**
   * Create edit form for existing endpoint
   */
  createEditForm(): FormGroup {
    return this.fb.group({
      endpointPath: ['', [Validators.required, this.pathValidator()]],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: [''],
      responseBodySchema: ['']
    });
  }

  /**
   * Populate form with endpoint data
   */
  populateForm(form: FormGroup, endpoint: SourceSystemEndpointDTO): void {
    form.patchValue({
      endpointPath: endpoint.endpointPath,
      httpRequestType: endpoint.httpRequestType,
      requestBodySchema: endpoint.requestBodySchema,
      responseBodySchema: endpoint.responseBodySchema
    });

    // TODO: Handle query params and path params arrays
  }

  /**
   * Get form data for submission
   */
  getFormData(form: FormGroup): Partial<SourceSystemEndpointDTO> {
    const formValue = form.value;
    return {
      endpointPath: formValue.endpointPath,
      httpRequestType: formValue.httpRequestType,
      requestBodySchema: formValue.requestBodySchema,
      responseBodySchema: formValue.responseBodySchema
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
      { label: 'Path', value: ApiEndpointQueryParamType.Path },
      { label: 'Query', value: ApiEndpointQueryParamType.Query }
    ];
  }

  /**
   * Load endpoints for a source system
   */
  loadEndpoints(sourceSystemId: number): Observable<SourceSystemEndpointDTO[]> {
    return this.endpointService.apiConfigSourceSystemSourceSystemIdEndpointGet(sourceSystemId);
  }

  /**
   * Create new endpoint
   */
  createEndpoint(sourceSystemId: number, endpointData: Partial<SourceSystemEndpointDTO>): Observable<SourceSystemEndpointDTO> {
    const endpoint: Partial<SourceSystemEndpointDTO> = {
      ...endpointData,
      sourceSystemId: sourceSystemId
    };
    return this.endpointService.apiConfigSourceSystemSourceSystemIdEndpointPost(sourceSystemId, [endpoint as SourceSystemEndpointDTO]);
  }

  /**
   * Update existing endpoint
   */
  updateEndpoint(endpoint: SourceSystemEndpointDTO): Observable<SourceSystemEndpointDTO> {
    // Mock implementation since updateEndpoint doesn't exist in backend
    return new Observable(observer => {
      setTimeout(() => {
        observer.next(endpoint);
        observer.complete();
      }, 100);
    });
  }

  /**
   * Delete endpoint
   */
  deleteEndpoint(endpointId: number): Observable<void> {
    // Mock implementation since deleteEndpoint doesn't exist in backend
    return new Observable(observer => {
      setTimeout(() => {
        observer.next();
        observer.complete();
      }, 100);
    });
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
