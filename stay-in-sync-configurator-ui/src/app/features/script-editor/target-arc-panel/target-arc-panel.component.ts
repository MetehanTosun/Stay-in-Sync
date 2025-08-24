import {
  ChangeDetectorRef,
  Component,
  inject,
  Input,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';

import { TargetArcConfiguration } from '../models/target-system.models';
import { TargetSystem } from '../models/target-system.models';

import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { MonacoEditorService } from '../../../core/services/monaco-editor.service';
import { MessageService } from 'primeng/api';

import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { TreeNode } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { AccordionModule } from 'primeng/accordion';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';

import { TargetArcWizardComponent } from '../target-arc-wizard/target-arc-wizard.component';
import { TreeTableModule } from 'primeng/treetable';

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
    TreeTableModule,
    DialogModule,
    AccordionModule,
    ProgressSpinnerModule,
    TooltipModule,
    TargetArcWizardComponent,
  ],
  templateUrl: './target-arc-panel.component.html',
  styleUrl: './target-arc-panel.component.css',
})
export class TargetArcPanelComponent implements OnInit {
  @Input() transformationId!: string;

  activeArcs: TargetArcConfiguration[] = [];
  activeArcsTree: TreeNode[] = [];
  librarySystems: LibrarySystem[] = [];

  public activeArcIds = new Set<number>();

  isLoading = false;
  displayLibrary = false;
  isWizardVisible = false;

  private scriptEditorService = inject(ScriptEditorService);
  private monacoEditorService = inject(MonacoEditorService);
  private messageService = inject(MessageService);

  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    if (this.transformationId) {
      this.loadActiveArcs();
    }
  }

  loadActiveArcs(): void {
    this.isLoading = true;

    this.scriptEditorService
      .getActiveArcsForTransformation(this.transformationId)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (loadedArcs) => {
          this.activeArcs = loadedArcs;
          this.activeArcIds = new Set(this.activeArcs.map((a) => a.id));
          this.buildTreeNodes();
          this.updateMonacoTypes();
        },
        error: (err) => {
          console.error('Failed to load active ARCs', err);
          this.messageService.add({
            severity: 'error',
            summary: 'Loading Error',
            detail: 'The configuration of the directives could not be loaded.',
          });
        },
      });
  }

  updateMonacoTypes(): void {
    this.scriptEditorService
      .getTargetTypeDefinitions(Number(this.transformationId))
      .subscribe((response) => {
        console.log(
          '%c[TargetArcPanel] Received types from backend. Requesting update.',
          'color: #0ea5e9;',
          response
        );
        this.monacoEditorService.requestTypeUpdate(response);
      });
  }

  addArc(arcIdToAdd: number): void {
    const currentArcIds = new Set(this.activeArcs.map((a) => a.id));
    if (currentArcIds.has(arcIdToAdd)) {
      return;
    }

    const newArcIds = [...currentArcIds, arcIdToAdd];

    this.scriptEditorService
      .updateTransformationTargetArcs(Number(this.transformationId), newArcIds)
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Directive Added',
          });
          this.loadActiveArcs();
          this.displayLibrary = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Could not add the directive.',
          });
          console.error(err);
        },
      });
  }

  removeArc(arcId: number): void {
    console.log('Arc id is ' + arcId);
    const newArcIds = this.activeArcs
      .map((a) => a.id)
      .filter((id) => id !== arcId);
    console.log(newArcIds);
    this.scriptEditorService
      .updateTransformationTargetArcs(Number(this.transformationId), newArcIds)
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'info',
            summary: 'Directive Removed',
          });
          this.loadActiveArcs();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Could not remove the directive.',
          });
        },
      });
  }

  showLibrary(): void {
    this.scriptEditorService.getTargetSystems().subscribe((systems) => {
      this.librarySystems = systems.map((s) => ({
        ...s,
        arcs: [],
        isLoading: false,
      }));
      this.displayLibrary = true;
    });
  }

  onTabOpen(event: any): void {
    const systemIndex = event.index;
    const system = this.librarySystems[systemIndex];
    if (!system || system.arcs.length > 0 || system.isLoading) return;

    system.isLoading = true;
    this.scriptEditorService
      .getArcsByTargetSystem(system.id)
      .pipe(finalize(() => (system.isLoading = false)))
      .subscribe((arcs) => {
        system.arcs = arcs;
        this.cdr.detectChanges();
      });
  }

  private buildTreeNodes(): void {
    const groupedBySystem = new Map<string, TargetArcConfiguration[]>();
    for (const arc of this.activeArcs){
      if (!groupedBySystem.has(arc.targetSystemName)){
        groupedBySystem.set(arc.targetSystemName, []);
      }
      groupedBySystem.get(arc.targetSystemName)!.push(arc);
    }

    const tree: TreeNode[] = [];
    groupedBySystem.forEach((arcs, systemName) => {
      const systemNode: TreeNode = {
        key: `system-${systemName}`,
        data: {
          name: systemName,
          type: 'system'
        },
        children: arcs.map(arc => ({
          key: `arc-${arc.id}`,
          data: {
            name: arc.alias,
            id: arc.id,
            type: 'arc'
          }
        }))
      };
      tree.push(systemNode);
    });

    this.activeArcsTree = tree;
  }

  showArcWizard(): void {
    this.isWizardVisible = true;
  }

  onWizardSaveSuccess(savedArc: TargetArcConfiguration): void {
    this.isWizardVisible = false;
    this.messageService.add({
      severity: 'success',
      summary: 'Saved',
      detail: 'The new ARC has been successfully created.',
    });
    this.addArc(savedArc.id);
  }
}
