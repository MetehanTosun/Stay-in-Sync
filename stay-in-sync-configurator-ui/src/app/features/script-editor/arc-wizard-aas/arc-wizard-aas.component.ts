import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';

import {
  AasArc,
  AasArcSaveRequest,
  SubmodelDescription,
} from '../models/arc.models';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
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

/**
 * @description
 * A wizard component displayed in a dialog for creating and editing Asset Administration Shell (AAS)
 * based API Request Configurations (ARCs). It handles form validation, data population for edit mode,
 * and communication with the backend service to save the ARC.
 */
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
    TooltipModule,
  ],
  templateUrl: './arc-wizard-aas.component.html',
  styleUrls: ['./arc-wizard-aas.component.css'],
})
export class ArcWizardAasComponent implements OnChanges {
  /**
   * @description Controls the visibility of the wizard dialog.
   */
  @Input() visible = false;
  /**
   * @description The context object providing necessary data for the wizard.
   * Includes the parent source system, the target submodel, and an optional ARC object for editing.
   */
  @Input() context!: {
    system: SourceSystem;
    submodel: SubmodelDescription;
    arcToEdit?: AasArc;
  };
  /**
   * @description Emits an event when the dialog is closed, either by the user or after a successful save.
   */
  @Output() onHide = new EventEmitter<void>();
  /**
   * @description Emits the successfully saved ARC configuration.
   */
  @Output() onSaveSuccess = new EventEmitter<AasArc>();

  /**
   * @description The reactive form group that manages the state of the wizard's input fields.
   */
  arcForm: FormGroup;
  /**
   * @description A flag to indicate when a save operation is in progress, used to show loading indicators.
   */
  isSaving = false;
  /**
   * @description A flag to determine if the wizard is in 'edit' or 'create' mode.
   */
  isEditMode = false;

  private fb = inject(FormBuilder);
  private aasService = inject(AasService);
  private arcStateService = inject(ArcStateService);
  private messageService = inject(MessageService);

  /**
   * @description Initializes the component and sets up the reactive form with its controls and validators.
   */
  constructor() {
    this.arcForm = this.fb.group({
      alias: ['', Validators.required],
      pollingRate: [20000, [Validators.required, Validators.min(1)]],
      active: [false, Validators.required],
    });
  }

  /**
   * @description Angular lifecycle hook that responds to changes in data-bound input properties.
   * It initializes the wizard's state when it becomes visible.
   * @param {SimpleChanges} changes - An object representing the changes to the input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    const visibilityConfig = 'visible';
    if (changes[visibilityConfig] && this.visible && this.context) {
      this.resetWizard();
      this.isEditMode = !!this.context.arcToEdit;

      if (this.isEditMode && this.context.arcToEdit) {
        this.populateForm(this.context.arcToEdit);
      }
    }
  }

  /**
   * @private
   * @description Populates the form fields with data from an existing ARC when in 'edit' mode.
   * @param {AasArc} arc - The ARC object to load into the form.
   */
  private populateForm(arc: AasArc): void {
    this.arcForm.patchValue({
      alias: arc.alias,
      pollingRate: arc.pollingIntervallTimeInMs,
      active: arc.active,
    });
  }

  /**
   * @description Handles the save action. It validates the form, constructs the save request DTO,
   * calls the appropriate service method (create or update), and handles the response.
   */
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

    saveOperation.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: (savedArc) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `AAS ARC '${savedArc.alias}' has been saved.`,
        });
        const arcForState: AasArc = {
          ...savedArc,
          sourceSystemName: this.context.system.name,
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

  /**
   * @description Emits the onHide event to signal that the dialog should be closed.
   */
  closeDialog(): void {
    this.onHide.emit();
  }

  /**
   * @private
   * @description Resets the wizard to its initial state, clearing the form and any state flags.
   */
  private resetWizard(): void {
    this.isSaving = false;
    this.isEditMode = false;
    const defaultPollingRate = 1000;
    this.arcForm.reset({
      pollingRate: defaultPollingRate,
      active: true,
    });
  }
}
