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
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Observable, of } from 'rxjs';
import { filter, switchMap, tap } from 'rxjs/operators';

import {
  EndpointSuggestion,
  TargetSystem,
  TargetArcConfiguration,
  CreateTargetArcDTO,
} from '../models/target-system.models';

import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { MessageService } from 'primeng/api';

import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';

/**
 * @description
 * A wizard component for creating new REST-based Target ARCs. It is presented within a PrimeNG dialog and guides the
 * user through selecting a target system, choosing an ARC pattern (e.g., List Upsert), and mapping specific API
 * endpoints to the required actions (Check, Create, Update). The component handles form validation, dynamic
 * loading of endpoint suggestions based on the selected system, and communication with the backend to create
 * the new ARC configuration.
 */
@Component({
  selector: 'app-target-arc-wizard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    ProgressSpinnerModule,
  ],
  templateUrl: './target-arc-wizard.component.html',
  styleUrls: ['./target-arc-wizard.component.css'],
})
export class TargetArcWizardComponent implements OnChanges {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saveSuccess = new EventEmitter<TargetArcConfiguration>();

  private fb = inject(FormBuilder);
  private scriptEditorService = inject(ScriptEditorService);
  private messageService = inject(MessageService);

  wizardForm: FormGroup;
  targetSystems$: Observable<TargetSystem[]>;
  endpointSuggestions$: Observable<EndpointSuggestion[]> = of([]);

  isLoadingEndpoints = false;
  isSaving = false;

  arcPatternTypes = [
    {
      label: 'List Upsert (Check, Create, Update für Listen)',
      value: 'LIST_UPSERT',
    },
    {
      label: 'Object Upsert (Check, Create, Update für Objekte)',
      value: 'OBJECT_UPSERT',
    },
    // { label: 'Basic API Call', value: 'BASIC_API' } // TODO: Implement
  ];

  /**
   * @description
   * Initializes the component by setting up the main reactive form (`wizardForm`) with its controls,
   * default values, and validators. It also establishes a reactive pipeline that listens for changes to
   * the `targetSystemId` form control, automatically fetching and updating the list of endpoint suggestions
   * for the selected system.
   */
  constructor() {
    const defaultListUpsertSelection = 'LIST_UPSERT';
    this.wizardForm = this.fb.group({
      alias: ['', Validators.required],
      targetSystemId: [null, Validators.required],
      arcPatternType: [defaultListUpsertSelection, Validators.required],
      checkEndpointId: [null, Validators.required],
      createEndpointId: [null, Validators.required],
      updateEndpointId: [null, Validators.required],
    });

    this.targetSystems$ = this.scriptEditorService.getTargetSystems();

    this.wizardForm
      .get('targetSystemId')!
      .valueChanges.pipe(
        tap(() => {
          this.isLoadingEndpoints = true;
          this.endpointSuggestions$ = of([]);
        }),
        filter((systemId) => systemId !== null),
        switchMap((systemId) =>
          this.scriptEditorService.getEndpointSuggestionsForTargetSystem(
            systemId
          )
        )
      )
      .subscribe((suggestions) => {
        this.endpointSuggestions$ = of(suggestions);
        this.isLoadingEndpoints = false;
      });
  }

  /**
   * @description
   * Angular lifecycle hook that responds to changes in data-bound input properties.
   * It resets the form to its default state whenever the wizard is made visible, ensuring a clean
   * slate for each new ARC creation.
   * @param {SimpleChanges} changes - An object representing the changes to the input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    const visibilityConfig = 'visible';
    if (
      changes[visibilityConfig] &&
      changes[visibilityConfig].currentValue === true
    ) {
      if (!this.isSaving) {
        const defaultListUpsertSelection = 'LIST_UPSERT';
        this.wizardForm.reset({ arcPatternType: defaultListUpsertSelection });
      }
    }
  }

  /**
   * @description
   * Handles the save action triggered by the user. It first validates the form to ensure all required
   * fields are filled. If valid, it constructs the `CreateTargetArcDTO` payload from the form values.
   * It then calls the `ScriptEditorService` to create the ARC on the backend and handles the success
   * or error response by emitting an event or showing a message.
   */
  onSave(): void {
    if (this.wizardForm.invalid) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Missing Inputs',
        detail: 'Please fill out all required fields.',
      });
      return;
    }

    this.isSaving = true;
    const formValue = this.wizardForm.value;

    const checkActionString = 'CHECK';
    const createActionString = 'CREATE';
    const updateActionString = 'UPDATE';

    const dto: CreateTargetArcDTO = {
      alias: formValue.alias,
      targetSystemId: formValue.targetSystemId,
      arcPatternType: formValue.arcPatternType,
      actions: [
        {
          endpointId: formValue.checkEndpointId,
          actionRole: checkActionString,
          executionOrder: 1,
        },
        {
          endpointId: formValue.createEndpointId,
          actionRole: createActionString,
          executionOrder: 2,
        },
        {
          endpointId: formValue.updateEndpointId,
          actionRole: updateActionString,
          executionOrder: 3,
        },
      ],
    };

    this.scriptEditorService
      .createArc(dto)
      .subscribe({
        next: (savedArc) => {
          this.saveSuccess.emit(savedArc);
          this.closeDialog();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error during Save.',
            detail: err.error?.message || 'The ARC could not be created.',
          });
        },
      })
      .add(() => (this.isSaving = false));
  }

  /**
   * @description
   * Closes the wizard dialog. It sets the `visible` property to false and emits the `visibleChange`
   * event to notify the parent component of the change, allowing for two-way binding.
   */
  closeDialog(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
  }
}
