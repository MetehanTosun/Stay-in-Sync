import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

import { AasArc, AasArcSaveRequest, SubmodelDescription } from '../models/arc.models';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ArcStateService } from '../../../core/services/arc-state.service';
import { AasService } from '../../source-system/services/aas.service';
import { MessageService } from 'primeng/api';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputSwitchModule } from 'primeng/inputswitch';
import { finalize } from 'rxjs';
import { TooltipModule } from 'primeng/tooltip';
import { SourceSystem } from '../../source-system/models/source-system.models';

@Component({
  selector: 'app-arc-wizard-aas',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    InputSwitchModule,
    TooltipModule
  ],
  templateUrl: './arc-wizard-aas.component.html',
  styleUrls: ['./arc-wizard-aas.component.css'],
})
export class ArcWizardAasComponent implements OnChanges {
  @Input() visible = false;
  @Input() context!: {
    system: SourceSystem;
    submodel: SubmodelDescription;
    arcToEdit?: AasArc;
  };
  @Output() onHide = new EventEmitter<void>();
  @Output() onSaveSuccess = new EventEmitter<AasArc>();

  arcForm: FormGroup;
  isSaving = false;
  isEditMode = false;

  private fb = inject(FormBuilder);
  private aasService = inject(AasService);
  private arcStateService = inject(ArcStateService);
  private messageService = inject(MessageService);

  constructor() {
    this.arcForm = this.fb.group({
      alias: ['', Validators.required],
      pollingRate: [20000, [Validators.required, Validators.min(1)]],
      active: [false, Validators.required],
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible && this.context) {
      this.resetWizard();
      this.isEditMode = !!this.context.arcToEdit;

      if (this.isEditMode && this.context.arcToEdit) {
        this.populateForm(this.context.arcToEdit);
      }
    }
  }

  private populateForm(arc: AasArc): void {
    this.arcForm.patchValue({
      alias: arc.alias,
      pollingRate: arc.pollingIntervallTimeInMs,
      active: arc.active,
    });
  }

  onSaveArc(): void {
    if (this.arcForm.invalid) {
      this.arcForm.markAllAsTouched();
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'Please fill out all required fields.',
      });
      return;
    }

    this.isSaving = true;
    const formValue = this.arcForm.getRawValue();
    
    const saveRequest: AasArcSaveRequest = {
      id: this.context.arcToEdit?.id,
      sourceSystemId: this.context.system.id!,
      submodelId: this.context.submodel.coreEntityId,
      alias: formValue.alias,
      pollingIntervallTimeInMs: formValue.pollingRate,
      active: formValue.active,
    };

    const saveOperation = this.isEditMode
      ? this.aasService.updateAasArc(saveRequest.id!, saveRequest)
      : this.aasService.createAasArc(saveRequest);

    saveOperation
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (savedArc) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: `AAS ARC '${savedArc.alias}' has been saved.`,
          });
          const arcForState : AasArc = {
            ...savedArc,
            sourceSystemName: this.context.system.name
          };
            this.arcStateService.addOrUpdateArc(arcForState);
            this.onSaveSuccess.emit(savedArc);
            this.closeDialog();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Save Failed',
            detail: err.error?.message || 'Could not save the AAS ARC.',
          });
          console.error('Save AAS ARC error:', err);
        },
      });
  }

  closeDialog(): void {
    this.onHide.emit();
  }

  private resetWizard(): void {
    this.isSaving = false;
    this.isEditMode = false;
    this.arcForm.reset({
      pollingRate: 20000,
      active: true,
    });
  }
}
