import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from "@angular/core";
import { AasTargetArcConfiguration, CreateAasTargetArcDTO, SubmodelDescription, TargetSystem } from "../models/target-system.models";
import { CommonModule } from "@angular/common";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { DialogModule } from "primeng/dialog";
import { ButtonModule } from "primeng/button";
import { InputTextModule } from "primeng/inputtext";
import { ProgressSpinnerModule } from "primeng/progressspinner";
import { DropdownModule } from "primeng/dropdown";
import { ScriptEditorService } from "../../../core/services/script-editor.service";
import { MessageService } from "primeng/api";
import { filter, finalize, map, Observable, of, switchMap, tap } from "rxjs";
import { SourceSystem } from "../../source-system/models/source-system.models";

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
  styleUrls: ['./target-arc-wizard-aas.component.css']
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

  constructor() {
    this.wizardForm = this.fb.group({
      alias: ['', Validators.required],
      targetSystemId: [null, Validators.required],
      submodelId: [null, Validators.required],
    });

    this.aasTargetSystems$ = this.scriptEditorService.getSourceSystems().pipe(
      map(systems => systems.filter(s => s.apiType === 'AAS'))
    );

    this.wizardForm.get('targetSystemId')!.valueChanges.pipe(
      tap(() => {
        this.isLoadingSubmodels = true;
        this.submodels$ = of([]);
        this.wizardForm.get('submodelId')?.reset();
      }),
      filter((systemId): systemId is number => !!systemId),
      switchMap(systemId => 
        this.scriptEditorService.getSubmodelsForTargetSystem(systemId).pipe(
          finalize(() => this.isLoadingSubmodels = false)
        )
      )
    ).subscribe(submodels => {
      this.submodels$ = of(submodels);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
      if (changes['visible'] && this.visible) {
          this.isEditMode = !!this.context?.arcToEdit;
          if (this.isEditMode && this.context?.arcToEdit) {
              // Fülle das Formular für den Edit-Modus
              this.wizardForm.patchValue({
                  alias: this.context.arcToEdit.alias,
                  targetSystemId: this.context.arcToEdit.targetSystemId,
                  submodelId: this.context.arcToEdit.id,
              });
          } else if (!this.isSaving) {
              // Setze das Formular für den Create-Modus zurück
              this.wizardForm.reset();
          }
      }
  }

  onSave(): void {
    if (this.wizardForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Missing Fields', detail: 'Please fill out all required fields.' });
      return;
    }

    this.isSaving = true;
    const formValue = this.wizardForm.getRawValue();

    const dto: CreateAasTargetArcDTO = {
      alias: formValue.alias,
      targetSystemId: formValue.targetSystemId,
      submodelId: formValue.submodelId,
    };
    
    // TODO: Erweitere den Service um eine updateAasTargetArc-Methode
    const saveOperation = this.isEditMode
      ? this.scriptEditorService.updateAasTargetArc(this.context!.arcToEdit!.id, dto)
      : this.scriptEditorService.createAasTargetArc(dto);

    saveOperation
      .pipe(finalize(() => this.isSaving = false))
      .subscribe({
        next: (savedArc) => {
          this.messageService.add({ 
              severity: 'success', 
              summary: 'Success', 
              detail: `AAS Target ARC '${savedArc.alias}' has been saved.` 
          });
          this.saveSuccess.emit(savedArc);
          this.closeDialog();
        },
        error: (err) => {
          this.messageService.add({ 
              severity: 'error', 
              summary: 'Save Failed', 
              detail: err.error?.message || 'The AAS Target ARC could not be saved.' 
          });
          console.error(err);
        }
      });
  }

  closeDialog(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
  }
}
