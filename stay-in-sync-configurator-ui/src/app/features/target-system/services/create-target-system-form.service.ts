import { Injectable } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiAuthType } from '../../source-system/models/apiAuthType';

@Injectable({
  providedIn: 'root'
})
export class CreateTargetSystemFormService {

  constructor(private fb: FormBuilder) {}

  /**
   * Create the main form for target system creation
   */
  createForm(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: [''],
      apiType: ['REST_OPENAPI', Validators.required],
      aasId: [''],
      apiAuthType: [null],
      authConfig: this.fb.group({
        username: [''],
        password: [''],
        apiKey: [''],
        headerName: ['']
      }),
      openApiSpec: [{ value: null, disabled: false }]
    });
  }

  /**
   * Setup form value change subscriptions
   */
  setupFormSubscriptions(form: FormGroup): void {
    form.get('apiType')!.valueChanges.subscribe((apiType: string) => {
      const aasIdCtrl = form.get('aasId')!;
      const openApiCtrl = form.get('openApiSpec')!;
      
      if (apiType === 'AAS') {
        aasIdCtrl.setValidators([Validators.required]);
        openApiCtrl.disable();
        openApiCtrl.clearValidators();
      } else {
        aasIdCtrl.clearValidators();
        openApiCtrl.enable();
      }
      
      aasIdCtrl.updateValueAndValidity();
      openApiCtrl.updateValueAndValidity();
    });

    form.get('apiAuthType')!.valueChanges.subscribe((authType: ApiAuthType) => {
      const grp = form.get('authConfig') as FormGroup;
      ['username', 'password', 'apiKey', 'headerName'].forEach(k => {
        grp.get(k)!.clearValidators();
        grp.get(k)!.updateValueAndValidity();
      });
      
      if (authType === ApiAuthType.Basic) {
        grp.get('username')!.setValidators([Validators.required]);
        grp.get('password')!.setValidators([Validators.required]);
      } else if (authType === ApiAuthType.ApiKey) {
        grp.get('apiKey')!.setValidators([Validators.required]);
        grp.get('headerName')!.setValidators([Validators.required]);
      } else {
      }
      
      ['username', 'password', 'apiKey', 'headerName'].forEach(k => 
        grp.get(k)!.updateValueAndValidity()
      );
    });
  }

  /**
   * Get steps configuration based on API type
   */
  getSteps(apiType: string): Array<{ label: string }> {
    if (apiType === 'AAS') {
      return [
        { label: 'Metadaten & Test' },
        { label: 'Api Header' },
        { label: 'AAS Submodels' },
      ];
    }
    return [
      { label: 'Metadaten' },
      { label: 'Api Header' },
      { label: 'Endpoints' },
    ];
  }

  /**
   * Reset form to default state
   */
  resetForm(form: FormGroup): void {
    form.reset({ apiType: 'REST_OPENAPI' });
    form.get('openApiSpec')!.enable();
  }

  /**
   * Check if form is valid for current step
   */
  isFormValidForStep(form: FormGroup, currentStep: number, apiType: string, aasTestOk?: boolean | null): boolean {
    if (currentStep === 0) {
      if (apiType === 'AAS') {
        return !form.invalid && aasTestOk === true;
      }
      return !form.invalid;
    }
    return true;
  }

  /**
   * Get form data for API submission
   */
  getFormDataForSubmission(form: FormGroup, selectedFile?: File | null): any {
    const base: any = { ...form.getRawValue() };
    
    if (selectedFile) {
      return { base, hasFile: true };
    } else {
      const val = base.openApiSpec;
      if (typeof val === 'string' && val.trim()) {
        base.openAPI = val;
      }
      delete base.openApiSpec;
      delete base.apiAuthType;
      delete base.authConfig;
      return { base, hasFile: false };
    }
  }
}
