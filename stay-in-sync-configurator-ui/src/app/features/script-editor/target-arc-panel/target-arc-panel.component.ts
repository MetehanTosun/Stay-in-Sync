import {
  ChangeDetectorRef,
  Component,
  inject,
  Input,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';

import {
  AasTargetArcConfiguration,
  AnyTargetArc,
  TargetArcConfiguration,
  UpdateTransformationRequestConfigurationDTO,
} from '../models/target-system.models';
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
import { TargetArcWizardAasComponent } from '../target-arc-wizard-aas/target-arc-wizard-aas.component';

/**
 * @description Represents a Target System specifically within the context of the 'From Library' dialog.
 * It extends the base `TargetSystem` model with local UI state for managing on-demand loading of its associated ARCs.
 */
type LibrarySystem = TargetSystem & {
  arcs: AnyTargetArc[];
  isLoading: boolean;
};

/**
 * @description
 * This component serves as the central UI panel for managing Target Directives (ARCs) within the script editor.
 * Its primary responsibilities include:
 * 1.  Displaying the list of ARCs currently active for a given transformation.
 * 2.  Providing UI controls to add new ARCs, either by creating them from scratch (via wizards) or by selecting them from a global library.
 * 3.  Enabling users to remove or edit active ARCs.
 * 4.  Communicating with the backend via `ScriptEditorService` to synchronize any changes to the active ARC list.
 * 5.  Triggering updates to the Monaco editor's type definitions via `MonacoEditorService` to ensure IntelliSense reflects the current configuration.
 */
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
    TargetArcWizardAasComponent,
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

  private cdr = inject(ChangeDetectorRef);

  /**
   * @description Initializes the component by setting up the static menu items for the 'Create New' button.
   */
  constructor() {
    this.createMenuItems = [
      {
        label: 'New REST ARC',
        icon: 'pi pi-globe',
        command: () => this.showRestArcWizard(),
      },
      {
        label: 'New AAS ARC',
        icon: 'pi pi-database',
        command: () => this.showAasArcWizard(),
      },
    ];
  }

  /**
   * @description Angular lifecycle hook that executes when the component is initialized.
   * It triggers the initial loading of active ARCs if a `transformationId` is present.
   */
  ngOnInit(): void {
    if (this.transformationId) {
      this.loadActiveArcs();
    }
  }

  /**
   * @description Fetches the list of all Target ARCs currently active for the given transformation from the backend.
   * It processes the loaded ARCs to ensure uniqueness, updates the local state (`activeArcs`, `activeArcIds`),
   * and rebuilds the tree structure for display in the UI. It also manages the `isLoading` state.
   * @sideEffects Sets the `isLoading` flag, and upon success, populates `activeArcs`, `activeArcIds`, and `activeArcsTree`.
   */
  loadActiveArcs(): void {
    this.isLoading = true;

    this.scriptEditorService
      .getActiveAnyArcsForTransformation(Number(this.transformationId))
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        })
      )
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
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Loading Error',
            detail: 'The configuration of the directives could not be loaded.',
          });
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * @description Adds a new ARC to the list of active directives. It first checks for duplicates.
   * If the ARC is not already active, it constructs a new desired state and calls the synchronization
   * method to update the backend and the editor types.
   * @param {AnyTargetArc} arcToAdd - The ARC object to add to the active list.
   */
  addArc(arcToAdd: AnyTargetArc): void {
    if (this.activeArcIds.has(arcToAdd.id)) {
      this.messageService.add({
        severity: 'info',
        summary: 'Already Added',
        detail: 'This ARC is already active.',
      });
      return;
    }

    const newArcList = [...this.activeArcs, arcToAdd];
    this._synchronizeAndRefreshTypes(newArcList);
  }

  /**
   * @description Removes an ARC from the list of active directives. It constructs a new desired state
   * by filtering out the specified ARC and then calls the synchronization method to update the backend
   * and editor types.
   * @param {AnyTargetArc} arcToRemove - The ARC object to remove from the active list.
   */
  removeArc(arcToRemove: AnyTargetArc): void {
    const newArcList = this.activeArcs.filter(
      (arc) => arc.id !== arcToRemove.id || arc.arcType !== arcToRemove.arcType
    );
    this._synchronizeAndRefreshTypes(newArcList);
  }

  /**
   * @private
   * @description
   * This is the single source of truth for synchronizing the active ARCs with the backend
   * and fetching the updated Monaco type definitions. It is called after any operation
   * that changes the state of the active ARCs (add, remove, or edit).
   *
   * It uses the `updateTransformationTargetArcs` endpoint because it performs an atomic
   * operation: updating the transformation's configuration AND returning the newly
   * generated types in a single request, which prevents race conditions and ensures consistency.
   */
  private _synchronizeAndRefreshTypes(desiredArcState: AnyTargetArc[]): void {
    const dto: UpdateTransformationRequestConfigurationDTO = {
      restTargetArcIds: desiredArcState
        .filter((arc) => this.isRestArc(arc))
        .map((arc) => arc.id),
      aasTargetArcIds: desiredArcState
        .filter((arc) => this.isAasArc(arc))
        .map((arc) => arc.id),
    };

    this.scriptEditorService
      .updateTransformationTargetArcs(Number(this.transformationId), dto)
      .subscribe({
        next: (typeDefinitionsResponse) => {
          this.messageService.add({
            summary: 'Directives Synchronized',
            severity: 'success',
          });

          // Step 1: Tell Monaco about the new types.
          this.monacoEditorService.requestTypeUpdate(typeDefinitionsResponse);

          // Step 2: Update this component's local state with the confirmed new list.
          this.activeArcs = desiredArcState;
          this.activeArcIds = new Set(this.activeArcs.map((a) => a.id));
          this.buildTreeNodes();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Sync Failed',
            detail: 'Could not update directives.',
          });
          // On failure, reload the last known good state from the server to recover.
          this.loadActiveArcs();
        },
      });
  }

  /**
   * @description
   * Opens the 'From Library' dialog. It fetches all available target systems from the backend
   * and initializes them with local state for the library view (e.g., an empty `arcs` array
   * and a loading flag). This allows for on-demand loading of ARCs for each system.
   * @sideEffects Updates `this.librarySystems` with the fetched systems and sets `this.displayLibrary` to `true` to show the dialog.
   */
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

  /**
   * @description
   * An event handler for the PrimeNG Accordion in the 'From Library' dialog. It implements a
   * lazy-loading pattern. When a user expands a target system's tab for the first time, this
   * method is triggered to fetch all associated ARCs for that specific system. It includes a guard
   * to prevent redundant API calls if the ARCs have already been loaded or are in the process of being loaded.
   * @param {any} event - The event object emitted by the PrimeNG Accordion component, expected to have an `index` property corresponding to the opened tab.
   * @sideEffects Sets the `isLoading` flag on the specific system, fetches ARCs, populates the `system.arcs` array, and triggers change detection.
   */
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

  /**
   * @private
   * @description
   * Transforms the flat list of `activeArcs` into a hierarchical `TreeNode[]` structure required
   * by the PrimeNG Accordion component in the template. It groups the active ARCs by their `targetSystemName`,
   * creating a parent node for each system and child nodes for each ARC within that system.
   * @sideEffects The generated tree structure is assigned to the `this.activeArcsTree` property, which triggers a UI update.
   */
  private buildTreeNodes(): void {
    const groupedBySystem = new Map<string, AnyTargetArc[]>();
    for (const arc of this.activeArcs) {
      if (!groupedBySystem.has(arc.targetSystemName)) {
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
          type: 'system',
        },
        children: arcs.map((arc) => ({
          key: `arc-${arc.id}`,
          data: {
            name: arc.alias,
            id: arc.id,
            type: 'arc',
            arc: arc,
          },
        })),
      };
      tree.push(systemNode);
    });

    this.activeArcsTree = tree;
  }

  /**
   * @description
   * Shows the wizard for creating a new REST ARC.
   * Clears any previous edit context to ensure the wizard opens in 'create' mode.
   */
  showRestArcWizard(): void {
    this.isRestWizardVisible = true;
  }

  /**
   * @description
   * Shows the wizard for creating a new AAS ARC.
   * Clears any previous edit context to ensure the wizard opens in 'create' mode.
   */
  showAasArcWizard(): void {
    this.isAasWizardVisible = true;
  }

  /**
   * @description
   * Shows the appropriate wizard to edit an existing ARC.
   * @param arc The ARC object to be edited.
   */
  showEditArcWizard(arc: AnyTargetArc): void {
    if (this.isAasArc(arc)) {
      this.isAasWizardVisible = true;
    } else if (this.isRestArc(arc)) {
      this.isRestWizardVisible = true;
    }
  }

  /**
   * @description
   * Handles the successful save from the REST ARC wizard.
   * @param savedArc The ARC data returned from the wizard.
   */
  onRestWizardSaveSuccess(savedArc: TargetArcConfiguration): void {
    this.isRestWizardVisible = false;
    this.messageService.add({ summary: 'Saved', detail: 'REST ARC created.' });
    this.addArc(savedArc);
  }

  /**
   * @description
   * Handles the successful save from the AAS ARC wizard.
   * @param savedArc The ARC data returned from the wizard.
   */
  onAasWizardSaveSuccess(savedArc: AasTargetArcConfiguration): void {
    this.isAasWizardVisible = false;
    this.messageService.add({ summary: 'Saved', detail: 'AAS ARC created.' });
    this.addArc(savedArc);
  }

  isRestArc(
    arc: AnyTargetArc | null | undefined
  ): arc is TargetArcConfiguration {
    return !!arc && arc.arcType === 'REST';
  }

  isAasArc(
    arc: AnyTargetArc | null | undefined
  ): arc is AasTargetArcConfiguration {
    return !!arc && arc.arcType === 'AAS';
  }
}
