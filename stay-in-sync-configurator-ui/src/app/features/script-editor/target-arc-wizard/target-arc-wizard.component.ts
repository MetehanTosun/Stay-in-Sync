import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Observable, of } from 'rxjs';
import { filter, switchMap, tap } from 'rxjs/operators';

import { EndpointSuggestion, TargetSystem, TargetArcConfiguration, CreateTargetArcDTO } from '../models/target-system.models';

import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { MessageService } from 'primeng/api';

import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { FilterByHttpMethodPipe } from '../../../pipes/filter-by-http-method.pipe';
import { SelectModule } from 'primeng/select';

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
    FilterByHttpMethodPipe
  ],
  templateUrl: './target-arc-wizard.component.html',
  styleUrls: ['./target-arc-wizard.component.css']
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
    { label: 'List Upsert (Check, Create, Update für Listen)', value: 'LIST_UPSERT' },
    { label: 'Object Upsert (Check, Create, Update für Objekte)', value: 'OBJECT_UPSERT' },
    // { label: 'Basic API Call', value: 'BASIC_API' } // TODO: Implement
  ];

  constructor() {
    this.wizardForm = this.fb.group({
      alias: ['', Validators.required],
      targetSystemId: [null, Validators.required],
      arcPatternType: ['LIST_UPSERT', Validators.required],
      checkEndpointId: [null, Validators.required],
      createEndpointId: [null, Validators.required],
      updateEndpointId: [null, Validators.required],
    });

    this.targetSystems$ = this.scriptEditorService.getTargetSystems();

    this.wizardForm.get('targetSystemId')!.valueChanges.pipe(
      tap(() => {
        this.isLoadingEndpoints = true;
        this.endpointSuggestions$ = of([]);
      }),
      filter(systemId => systemId !== null),
      switchMap(systemId => this.scriptEditorService.getEndpointSuggestionsForTargetSystem(systemId))
    ).subscribe(suggestions => {
      this.endpointSuggestions$ = of(suggestions);
      this.isLoadingEndpoints = false;
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
      if (changes['visible'] && changes['visible'].currentValue === true) {
          if (!this.isSaving) {
              this.wizardForm.reset({ arcPatternType: 'LIST_UPSERT' });
          }
      }
  }

  onSave(): void {
    if (this.wizardForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Fehlende Eingaben', detail: 'Bitte füllen Sie alle erforderlichen Felder aus.' });
      return;
    }

    this.isSaving = true;
    const formValue = this.wizardForm.value;

    const dto: CreateTargetArcDTO = {
      alias: formValue.alias,
      targetSystemId: formValue.targetSystemId,
      arcPatternType: formValue.arcPatternType,
      actions: [
        { endpointId: formValue.checkEndpointId, actionRole: 'CHECK', executionOrder: 1 },
        { endpointId: formValue.createEndpointId, actionRole: 'CREATE', executionOrder: 2 },
        { endpointId: formValue.updateEndpointId, actionRole: 'UPDATE', executionOrder: 3 },
      ]
    };

    this.scriptEditorService.createArc(dto).subscribe({
      next: (savedArc) => {
        this.messageService.add({ severity: 'success', summary: 'Gespeichert', detail: `Der ARC '${savedArc.alias}' wurde erstellt.` });
        this.saveSuccess.emit(savedArc);
        this.closeDialog();
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Fehler beim Speichern', detail: 'Der ARC konnte nicht erstellt werden.' });
        console.error(err);
      }
    }).add(() => this.isSaving = false);
  }

  closeDialog(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
  }
}
