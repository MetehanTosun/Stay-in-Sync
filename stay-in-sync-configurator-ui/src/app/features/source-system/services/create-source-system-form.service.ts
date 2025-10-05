import { Injectable } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { ApiAuthType } from '../models/apiAuthType';
import { CreateSourceSystemDTO } from '../models/createSourceSystemDTO';
import { SourceSystemDTO } from '../models/sourceSystemDTO';
import { BasicAuthDTO } from '../models/basicAuthDTO';
import { ApiKeyAuthDTO } from '../models/apiKeyAuthDTO';

@Injectable({
  providedIn: 'root'
})
export class CreateSourceSystemFormService {

  constructor(private fb: FormBuilder) {}

  /**
   * Create the reactive form with all necessary fields and validators
   */
  createForm(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: [''],
      apiType: ['REST_OPENAPI', Validators.required],
      apiAuthType: [null],
      aasId: [''],
      authConfig: this.fb.group({
        username: [''],
        password: [''],
        apiKey: [''],
        headerName: ['']
      }),
      openApiSpec: [{value: null, disabled: false}]
    });
  }

  /**
   * Setup form subscriptions for dynamic validation
   */
  setupFormSubscriptions(form: FormGroup): void {
    // Subscribe to API type changes
    form.get('apiType')?.valueChanges.subscribe((apiType: string) => {
      const aasIdCtrl = form.get('aasId');
      const openApiCtrl = form.get('openApiSpec');
      
      if (apiType === 'AAS') {
        aasIdCtrl?.setValidators([Validators.required]);
        openApiCtrl?.disable();
        openApiCtrl?.clearValidators();
      } else {
        aasIdCtrl?.clearValidators();
        openApiCtrl?.enable();
      }
      aasIdCtrl?.updateValueAndValidity();
      openApiCtrl?.updateValueAndValidity();
    });

    // Subscribe to authentication type changes
    form.get('apiAuthType')?.valueChanges.subscribe((authType: ApiAuthType) => {
      const authConfigGroup = form.get('authConfig') as FormGroup;
      
      // Reset all validators first
      ['username', 'password', 'apiKey', 'headerName'].forEach(key => {
        authConfigGroup.get(key)?.clearValidators();
        authConfigGroup.get(key)?.updateValueAndValidity();
      });

      // Set validators based on auth type
      if (authType === ApiAuthType.Basic) {
        authConfigGroup.get('username')?.setValidators([Validators.required]);
        authConfigGroup.get('password')?.setValidators([Validators.required]);
      } else if (authType === ApiAuthType.ApiKey) {
        authConfigGroup.get('apiKey')?.setValidators([Validators.required]);
        authConfigGroup.get('headerName')?.setValidators([Validators.required]);
      }

      // Update validity
      ['username', 'password', 'apiKey', 'headerName'].forEach(key => {
        authConfigGroup.get(key)?.updateValueAndValidity();
      });
    });
  }

  /**
   * Get steps configuration
   */
  getSteps(): Array<{label: string}> {
    return [
      {label: 'Metadaten'},
      {label: 'Api Header'},
      {label: 'Endpoints'},
    ];
  }

  /**
   * Reset form to initial state
   */
  resetForm(form: FormGroup): void {
    form.reset({
      name: '',
      description: '',
      apiType: 'REST_OPENAPI',
      apiUrl: '',
      authType: ApiAuthType.Basic,
      basicAuth: {
        username: '',
        password: ''
      },
      apiKeyAuth: {
        keyName: '',
        keyValue: '',
        location: 'header'
      },
      aasId: ''
    });
  }

  /**
   * Get form data formatted for submission
   */
  getFormDataForSubmission(form: FormGroup): CreateSourceSystemDTO {
    const formValue = form.value;
    const dto: CreateSourceSystemDTO = {
      name: formValue.name,
      description: formValue.description,
      apiType: formValue.apiType,
      apiUrl: formValue.apiUrl,
      aasId: formValue.aasId,
      apiAuthType: formValue.authType,
      authConfig: {
        authType: formValue.authType
      }
    };

    return dto;
  }

  /**
   * Check if form is valid for current step
   */
  isFormValidForStep(form: FormGroup, step: number): boolean {
    switch (step) {
      case 0: // Metadata step
        return !!(form.get('name')?.valid && 
               form.get('apiType')?.valid && 
               form.get('apiUrl')?.valid &&
               (form.get('apiType')?.value !== 'AAS' || form.get('aasId')?.valid));
      case 1: // API Headers step
        const authType = form.get('authType')?.value;
        if (authType === ApiAuthType.Basic) {
          return !!(form.get('basicAuth.username')?.valid && 
                 form.get('basicAuth.password')?.valid);
        } else if (authType === ApiAuthType.ApiKey) {
          return !!(form.get('apiKeyAuth.keyName')?.valid && 
                 form.get('apiKeyAuth.keyValue')?.valid);
        }
        return true;
      case 2: // Endpoints step
        return true; // No specific validation for endpoints step
      default:
        return form.valid;
    }
  }

  /**
   * Get type options for dropdown
   */
  getTypeOptions(): Array<{label: string, value: string}> {
    return [
      {label: 'REST-OpenAPI', value: 'REST_OPENAPI'},
      {label: 'AAS', value: 'AAS'}
    ];
  }

  /**
   * Get auth type options for dropdown
   */
  getAuthTypeOptions(): Array<{label: string, value: ApiAuthType}> {
    return [
      {label: 'Basic', value: ApiAuthType.Basic},
      {label: 'API Key', value: ApiAuthType.ApiKey}
    ];
  }

  /**
   * Populate form with existing source system data for editing
   */
  populateForm(form: FormGroup, sourceSystem: SourceSystemDTO): void {
    form.patchValue({
      name: sourceSystem.name,
      apiUrl: sourceSystem.apiUrl,
      description: sourceSystem.description,
      apiType: sourceSystem.apiType,
      aasId: sourceSystem.aasId
    });

    // Set auth data if available
    // Note: SourceSystemDTO doesn't have basicAuth/apiKeyAuth properties
    // These would need to be loaded separately if needed
  }
}
