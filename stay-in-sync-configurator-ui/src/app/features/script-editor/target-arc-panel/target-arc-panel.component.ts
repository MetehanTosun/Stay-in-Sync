import { Component, inject, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';

import { TargetArcConfiguration } from '../models/target-system.models';
import { TargetSystem } from '../models/target-system.models';

import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { MonacoEditorService } from '../../../core/services/monaco-editor.service';
import { MessageService } from 'primeng/api';

import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { AccordionModule } from 'primeng/accordion';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';

import { TargetArcWizardComponent } from '../target-arc-wizard/target-arc-wizard.component';

type LibrarySystem = TargetSystem & {
  arcs: TargetArcConfiguration[];
  isLoading: boolean;
};

@Component({
  selector: 'app-target-arc-panel',
  standalone: true,
  imports: [
    CommonModule,
    PanelModule,
    ButtonModule,
    TableModule,
    DialogModule,
    AccordionModule,
    ProgressSpinnerModule,
    TooltipModule,
    TargetArcWizardComponent
  ],
  templateUrl: './target-arc-panel.component.html',
  styleUrl: './target-arc-panel.component.css'
})
export class TargetArcPanelComponent implements OnInit {
  @Input() transformationId!: string;

  activeArcs: TargetArcConfiguration[] = [];
  librarySystems: LibrarySystem[] = [];
  
  isLoading = false;
  displayLibrary = false;
  isWizardVisible = false;

  private scriptEditorService = inject(ScriptEditorService);
  private monacoEditorService = inject(MonacoEditorService);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    if (this.transformationId) {
      this.loadActiveArcs();
    }
  }

  loadActiveArcs(): void {
    this.isLoading = true;

    this.scriptEditorService.getActiveArcsForTransformation(this.transformationId)
      .pipe(
        finalize(() => this.isLoading = false)
      )
      .subscribe({
        next: (loadedArcs) => {
          this.activeArcs = loadedArcs;
          this.updateMonacoTypes();
        },
        error: (err) => {
          console.error('Failed to load active ARCs', err);
          this.messageService.add({
            severity: 'error',
            summary: 'Loading Error',
            detail: 'The configuration of the directives could not be loaded.'
          });
        }
      });
  }

  updateMonacoTypes(): void {
    this.scriptEditorService.getTargetTypeDefinitions(Number(this.transformationId))
      .subscribe(response => {
        console.log('%c[TargetArcPanel] Received types from backend. Requesting update.', 'color: #0ea5e9;', response);
        this.monacoEditorService.requestTypeUpdate(response);
      });
  }

  addArc(arcId: number): void {
    const newArcIds = [...this.activeArcs.map(a => a.id), arcId];
    this.scriptEditorService.updateTransformationTargetArcs(Number(this.transformationId), newArcIds)
      .subscribe(() => {
        this.messageService.add({ severity: 'success', summary: 'ARC added' });
        this.loadActiveArcs();
        this.displayLibrary = false;
      });
  }

  removeArc(arcId: number): void {
    const newArcIds = this.activeArcs.map(a => a.id).filter(id => id !== arcId);
    this.scriptEditorService.updateTransformationTargetArcs(Number(this.transformationId), newArcIds)
      .subscribe(() => {
        this.messageService.add({ severity: 'info', summary: 'ARC removed' });
        this.loadActiveArcs();
      });
  }

  showLibrary(): void {
    this.scriptEditorService.getTargetSystems().subscribe(systems => {
      this.librarySystems = systems.map(s => ({ ...s, arcs: [], isLoading: false }));
      this.displayLibrary = true;
    });
  }

  onSystemExpand(system: LibrarySystem): void {
    if (system.arcs.length > 0 || system.isLoading) return;

    system.isLoading = true;
    this.scriptEditorService.getArcsByTargetSystem(system.id)
        .pipe(finalize(() => system.isLoading = false))
        .subscribe(arcs => {
            system.arcs = arcs;
        });
  }
  
  showArcWizard(): void {
    this.isWizardVisible = true;
  }
  
  onWizardSaveSuccess(): void {
      this.isWizardVisible = false;
      this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'The new ARC has been successfully created.' });
      this.loadActiveArcs();
  }
}
