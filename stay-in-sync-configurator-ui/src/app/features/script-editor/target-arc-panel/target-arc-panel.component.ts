import {
  ChangeDetectorRef,
  Component,
  inject,
  Input,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {finalize} from 'rxjs/operators';

import { AasTargetArcConfiguration, AnyTargetArc, TargetArcConfiguration, UpdateTransformationRequestConfigurationDTO } from '../models/target-system.models';
import { TargetSystem } from '../models/target-system.models';

import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { MonacoEditorService } from '../../../core/services/monaco-editor.service';
import { MenuItem, MessageService } from 'primeng/api';

import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { TreeNode } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { AccordionModule } from 'primeng/accordion';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';

import { TargetArcWizardComponent } from '../target-arc-wizard/target-arc-wizard.component';
import { TreeTableModule } from 'primeng/treetable';
import { MenuModule } from 'primeng/menu';
import { AasService } from '../../source-system/services/aas.service';
import { TargetArcWizardAasComponent } from '../target-arc-wizard-aas/target-arc-wizard-aas.component';

type LibrarySystem = TargetSystem & {
  arcs: AnyTargetArc[];
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
    MenuModule,
    TargetArcWizardComponent,
    TargetArcWizardAasComponent
  ],
  templateUrl: './target-arc-panel.component.html',
  styleUrl: './target-arc-panel.component.css',
})
export class TargetArcPanelComponent implements OnInit {
  @Input() transformationId!: string;

  activeArcs: AnyTargetArc[] = [];
  activeArcsTree: TreeNode[] = [];
  librarySystems: LibrarySystem[] = [];
  createMenuItems: MenuItem[];

  public activeArcIds = new Set<number>();

  isLoading = false;
  displayLibrary = false;
  isRestWizardVisible = false;
  isAasWizardVisible = false;

  private scriptEditorService = inject(ScriptEditorService);
  private monacoEditorService = inject(MonacoEditorService);
  private messageService = inject(MessageService);
  private aasService = inject(AasService);

  private cdr = inject(ChangeDetectorRef);

  constructor() {
        this.createMenuItems = [
            { label: 'New REST ARC', icon: 'pi pi-globe', command: () => this.showRestArcWizard() },
            { label: 'New AAS ARC', icon: 'pi pi-database', command: () => this.showAasArcWizard() }
        ];
    }

  ngOnInit(): void {
    if (this.transformationId) {
      this.loadActiveArcs();
    }
  }

  loadActiveArcs(): void {
    this.isLoading = true;

    this.scriptEditorService
      .getActiveAnyArcsForTransformation(Number(this.transformationId))
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (loadedArcs) => {
          const uniqueArcMap = new Map<string, AnyTargetArc>();
          for (const arc of loadedArcs) {
            const uniqueKey = `${arc.arcType}-${arc.id}`;
            if (!uniqueArcMap.has(uniqueKey)) {
              uniqueArcMap.set(uniqueKey, arc);
            }
          }
          this.activeArcs = Array.from(uniqueArcMap.values());
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

  addArc(arcToAdd: AnyTargetArc): void {
    if (this.activeArcIds.has(arcToAdd.id)) {
      this.messageService.add({ severity: 'info', summary: 'Already Added', detail: 'This ARC is already active in the transformation.' });
      return;
    }

    this.activeArcs = [...this.activeArcs, arcToAdd];
    this.activeArcIds.add(arcToAdd.id);
    this.buildTreeNodes();
    
    this.sendArcUpdates();
  }

  removeArc(arcToRemove: AnyTargetArc): void {
    this.activeArcs = this.activeArcs.filter(arc => arc.id !== arcToRemove.id || arc.arcType !== arcToRemove.arcType);
    this.activeArcIds.delete(arcToRemove.id);
    this.buildTreeNodes();
    this.sendArcUpdates();
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
    const groupedBySystem = new Map<string, AnyTargetArc[]>();
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
            type: 'arc',
            arc: arc
          }
        }))
      };
      tree.push(systemNode);
    });

    this.activeArcsTree = tree;
  }

  private sendArcUpdates(): void {
    const dto: UpdateTransformationRequestConfigurationDTO = {
      restTargetArcIds: this.activeArcs.filter(arc => arc.arcType === 'REST').map(arc => arc.id),
      aasTargetArcIds: this.activeArcs.filter(arc => arc.arcType === 'AAS').map(arc => arc.id)
    };

    console.log('%c[TargetPanel] Sending update DTO to backend:', 'color: #f97116;', dto);

    this.scriptEditorService.updateTransformationTargetArcs(Number(this.transformationId), dto)
    .subscribe({
      next: (typeDefinitionsResponse) => {
        // The response from the PUT request itself contains the new, correct types.
        this.messageService.add({ summary: 'Directives Updated', severity: 'success' });
        console.log('%c[TargetPanel] Received types from update response. Requesting editor update.', 'color: #10b981;', typeDefinitionsResponse);
        this.monacoEditorService.requestTypeUpdate(typeDefinitionsResponse);
      },
      error: (err) => { 
        this.messageService.add({ severity: 'error', summary: 'Update Failed', detail: 'Could not update the directive configuration.' });
        console.error("Error during update flow:", err);
        this.loadActiveArcs(); // Re-sync on failure.
      }
    });
  }

  showRestArcWizard(): void { this.isRestWizardVisible = true; }
  showAasArcWizard(): void { this.isAasWizardVisible = true; }

  onRestWizardSaveSuccess(savedArc: TargetArcConfiguration): void {
    this.isRestWizardVisible = false;
    this.messageService.add({ summary: 'Saved', detail: 'REST ARC created.' });
    this.addArc(savedArc);
  }

  onAasWizardSaveSuccess(savedArc: AasTargetArcConfiguration): void {
    this.isAasWizardVisible = false;
    this.messageService.add({ summary: 'Saved', detail: 'AAS ARC created.' });
    this.addArc(savedArc);
  }

  isRestArc(arc: AnyTargetArc | null | undefined): arc is TargetArcConfiguration {
    return !!arc && arc.arcType === 'REST';
  }
  
  isAasArc(arc: AnyTargetArc | null | undefined): arc is AasTargetArcConfiguration {
    return !!arc && arc.arcType === 'AAS';
  }

}
