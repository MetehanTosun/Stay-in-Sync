import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { ApiRequestConfiguration, ArcSaveRequest, ArcTestCallRequest, ArcTestCallResponse } from '../models/arc.models';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { SourceSystem, SourceSystemEndpoint } from '../../source-system/models/source-system.models';
import { DialogModule } from 'primeng/dialog';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { SchemaViewerComponent } from '../schema-viewer/schema-viewer.component';

@Component({
  selector: 'app-arc-wizard',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    DialogModule,
    InputTextModule,
    ButtonModule,
    DividerModule,
    SchemaViewerComponent
  ],
  templateUrl: './arc-wizard.component.html',
  styleUrl: './arc-wizard.component.css'
})
export class ArcWizardComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() context!: { system: SourceSystem; endpoint: SourceSystemEndpoint; arcToClone?: ApiRequestConfiguration};
  @Output() onHide =  new EventEmitter<void>();
  @Output() onSaveSuccess = new EventEmitter<ApiRequestConfiguration>();

  arcForm!: FormGroup;
  isTesting = false;
  isSaving = false;
  testResult: ArcTestCallResponse | null = null;

  get alias(){
    return this.arcForm.get('alias');
  }

  //TODO: add more getters for fields in Form

  constructor(private fb: FormBuilder, private scriptEditorService: ScriptEditorService){
    this.arcForm = this.fb.group({});
  }
  
  ngOnInit(): void {
      this.arcForm = this.fb.group({
        alias: ['', Validators.required],
        pathParameters: [''],
        queryParameterValues: [''],
        headerValues: ['']
        // TODO: add pathparams, queryparams etc. TENTATIVE !!!
      })
  }


  ngOnChanges(changes: SimpleChanges): void {
      if (changes['context'] && this.context){
        this.testResult = null;
        if (this.context.arcToClone){
          this.arcForm.patchValue({ 
            alias: `${this.context.arcToClone.alias}_copy`,
            // add all maps for cloning
          }
        );
        } else {
          this.arcForm.reset({
            alias: '',
            pathParameters: '',
            queryParameterValues: '',
            headerValues: ''
          });
        }
      }
  }

  onTestCall(): void {
    if (this.arcForm.invalid){
      this.arcForm.markAllAsTouched();
      return;
    }

    this.isTesting = true;
    this.testResult = null;

    const request: ArcTestCallRequest = {
      sourceSystemId: this.context.system.id,
      endpointId: this.context.endpoint.id,
      pathParameters: this.arcForm.value.pathparams || {},
      queryParameterValues: this.arcForm.value.queryparams || {},
      headerValues: this.arcForm.value.headers || {}
    }; // TODO: Build request from form

    this.scriptEditorService.testArcConfiguration(request). subscribe(result => {
      this.testResult = result;
      this.isTesting = false;
    })
  }

  onSaveArc(): void {
    if (this.arcForm.invalid || !this.testResult?.isSuccess || !this.testResult.generatedDts){
      // TODO: handle error state
      return;
    }
    this.isSaving = true;
    const request: ArcSaveRequest = {
      alias: this.arcForm.value.alias,
      sourceSystemId: this.context.system.id,
      endpointId: this.context.endpoint.id,
      responseDts: this.testResult.generatedDts,
      pathParameters: this.arcForm.value.pathParameters,
      queryParameterValues: this.arcForm.value.queryParameterValues,
      headerValues: this.arcForm.value.headerValues
      // TODO: map full params from form
    };
    this.scriptEditorService.saveArcConfiguration(request).subscribe(savedArc => {
      this.onSaveSuccess.emit(savedArc);
      this.closeDialog();
      this.isSaving = false;
    },
    (error) => {
        console.error("Failed to save ARC", error);
        // TODO: Show user-facing error
    },
    () => {
        this.isSaving = false;
    });
  }

  closeDialog(): void {
    this.onHide.emit();
  }
}
