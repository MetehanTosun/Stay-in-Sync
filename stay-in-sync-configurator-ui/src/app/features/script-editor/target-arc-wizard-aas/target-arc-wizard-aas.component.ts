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
  AasTargetArcConfiguration,
  CreateAasTargetArcDTO,
  SubmodelDescription,
} from '../models/target-system.models';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DropdownModule } from 'primeng/dropdown';
import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { MessageService } from 'primeng/api';
import { filter, finalize, map, Observable, of, switchMap, tap } from 'rxjs';
import { SourceSystem } from '../../source-system/models/source-system.models';

/**
 * @description
 * A wizard component displayed in a dialog for creating and editing Asset Administration Shell (AAS) based Target ARCs.
 * It provides a user interface for selecting an AAS-enabled target system and a specific submodel within that system.
 * The component supports both creating a new ARC and editing an existing one by populating the form with pre-existing
 * data. It handles all form validation and communication with the backend services for saving the configuration.
 */
@Component({
  selector: 'app-target-arc-wizard-aas',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    ProgressSpinnerModule,
    DropdownModule,
  ],
  templateUrl: './target-arc-wizard-aas.component.html',
  styleUrls: ['./target-arc-wizard-aas.component.css'],
})
export class TargetArcWizardAasComponent implements OnChanges {
  @Input() visible = false;
  @Input() context?: { arcToEdit?: AasTargetArcConfiguration };
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saveSuccess = new EventEmitter<AasTargetArcConfiguration>();

  private fb = inject(FormBuilder);
  private scriptEditorService = inject(ScriptEditorService);
  private messageService = inject(MessageService);

  wizardForm: FormGroup;
  aasTargetSystems$: Observable<SourceSystem[]>;
  submodels$: Observable<SubmodelDescription[]> = of([]);

  isLoadingSubmodels = false;
  isSaving = false;
  isEditMode = false;

  /**
   * @description
   * Initializes the component. It sets up the main reactive form (`wizardForm`) with its controls and validators.
   * It also establishes a reactive data flow that listens for changes on the `targetSystemId` form control
   * to dynamically fetch and populate the list of available submodels for the selected system.
   */
  constructor() {
    this.wizardForm = this.fb.group({
      alias: ['', Validators.required],
      targetSystemId: [null, Validators.required],
      submodelId: [null, Validators.required],
    });

    this.aasTargetSystems$ = this.scriptEditorService
      .getSourceSystems()
      .pipe(map((systems) => systems.filter((s) => s.apiType === 'AAS')));

    this.wizardForm
      .get('targetSystemId')!
      .valueChanges.pipe(
        tap(() => {
          this.isLoadingSubmodels = true;
          this.submodels$ = of([]);
          this.wizardForm.get('submodelId')?.reset();
        }),
        filter((systemId): systemId is number => !!systemId),
        switchMap((systemId) =>
          this.scriptEditorService
            .getSubmodelsForTargetSystem(systemId)
            .pipe(finalize(() => (this.isLoadingSubmodels = false)))
        )
      )
      .subscribe((submodels) => {
        this.submodels$ = of(submodels);
      });
  }

  /**
   * @description
   * Angular lifecycle hook that responds when data-bound input properties change.
   * When the wizard becomes visible (i.e., the `visible` input changes to `true`), this method initializes the form's state.
   * In 'edit' mode (when an `arcToEdit` is provided in the context), it populates the form with the existing ARC's data.
   * In 'create' mode, it resets the form to a clean state.
   * @param {SimpleChanges} changes - An object of key-value pairs mapping property names to SimpleChange objects.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.isEditMode = !!this.context?.arcToEdit;
      if (this.isEditMode && this.context?.arcToEdit) {
        this.wizardForm.patchValue({
          alias: this.context.arcToEdit.alias,
          targetSystemId: this.context.arcToEdit.targetSystemId,
          submodelId: this.context.arcToEdit.id,
        });
      } else if (!this.isSaving) {
        this.wizardForm.reset();
      }
    }
  }

  /**
   * @description
   * Handles the primary save action triggered by the user. It performs the following steps:
   * 1. Validates the `wizardForm`. If invalid, it displays a warning message.
   * 2. If valid, it sets a saving indicator and constructs the `CreateAasTargetArcDTO` from the form values.
   * 3. It determines whether to call the `createAasTargetArc` or `updateAasTargetArc` service method based on the `isEditMode` flag.
   * 4. It subscribes to the save operation, handling success (showing a message, emitting the result, and closing the dialog)
   *    or error (showing an error message) outcomes.
   */
  onSave(): void {
    if (this.wizardForm.invalid) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Missing Fields',
        detail: 'Please fill out all required fields.',
      });
      return;
    }

    this.isSaving = true;
    const formValue = this.wizardForm.getRawValue();

    const dto: CreateAasTargetArcDTO = {
      alias: formValue.alias,
      targetSystemId: formValue.targetSystemId,
      submodelId: formValue.submodelId,
    };

    // TODO: Add an update Button for Target ARCs
    const saveOperation = this.isEditMode
      ? this.scriptEditorService.updateAasTargetArc(
          this.context!.arcToEdit!.id,
          dto
        )
      : this.scriptEditorService.createAasTargetArc(dto);

    saveOperation.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: (savedArc) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `AAS Target ARC '${savedArc.alias}' has been saved.`,
        });
        this.saveSuccess.emit(savedArc);
        this.closeDialog();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Save Failed',
          detail:
            err.error?.message || 'The AAS Target ARC could not be saved.',
        });
      },
    });
  }

  /**
   * @description
   * Closes the wizard dialog. It sets the `visible` property to `false` and emits the `visibleChange` event to notify
   * the parent component of the change, allowing for two-way data binding on the `visible` property.
   */
  closeDialog(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
  }
}
