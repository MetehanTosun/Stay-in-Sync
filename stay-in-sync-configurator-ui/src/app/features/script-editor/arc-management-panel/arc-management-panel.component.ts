import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  NgZone,
  Output,
} from '@angular/core';
import {
  ArcStateService,
  SystemState,
} from '../../../core/services/arc-state.service';
import {
  AasArc,
  AnyArc,
  ApiRequestConfiguration,
  SubmodelDescription,
} from '../models/arc.models';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../source-system/models/source-system.models';
import { CommonModule } from '@angular/common';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { TabViewModule } from 'primeng/tabview';
import { Observable, tap } from 'rxjs';
import { FilterByEndpointPipe } from '../../source-system/pipes/filter-by-endpoint.pipe';
import { FilterByTypePipe } from '../../source-system/pipes/filter-by-type.pipe';
import { FilterBySubmodelPipe } from '../../source-system/pipes/filter-by-submodel.pipe';

@Component({
  selector: 'app-arc-management-panel',
  standalone: true,
  imports: [
    CommonModule,
    PanelModule,
    AccordionModule,
    ButtonModule,
    ProgressSpinnerModule,
    TooltipModule,
    TabViewModule,
    FilterByEndpointPipe,
    FilterByTypePipe,
    FilterBySubmodelPipe,
  ],
  templateUrl: './arc-management-panel.component.html',
  styleUrl: './arc-management-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArcManagementPanelComponent {
  // --- Event Outputs for REST ARCs ---
  /** Emits when the user requests to create a new REST-based ARC. */
  @Output() createArc = new EventEmitter<{
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
  }>();
  /** Emits when the user requests to edit an existing REST-based ARC. */
  @Output() editArc = new EventEmitter<{
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arc: ApiRequestConfiguration;
  }>();
  /** Emits when the user requests to delete an existing REST-based ARC. */
  @Output() deleteArc = new EventEmitter<{ arc: ApiRequestConfiguration }>();
  /** Emits when the user requests to clone an existing REST-based ARC. */
  @Output() cloneArc = new EventEmitter<{
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arc: ApiRequestConfiguration;
  }>();

  // --- Event Outputs for AAS ARCs ---
  /** Emits when the user requests to create a new AAS-based ARC. */
  @Output() createAasArc = new EventEmitter<{
    system: SourceSystem;
    submodel: SubmodelDescription;
  }>();
  /** Emits when the user requests to edit an existing AAS-based ARC. */
  @Output() editAasArc = new EventEmitter<{
    system: SourceSystem;
    submodel: SubmodelDescription;
    arc: AasArc;
  }>();
  /** Emits when the user requests to delete an existing AAS-based ARC. */
  @Output() deleteAasArc = new EventEmitter<{ arc: AasArc }>();

  /**
   * @description An observable stream of all source systems, used to populate the "All Data Sources" tab.
   * This stream is also tapped to maintain a local snapshot of the system list for direct access.
   */
  public readonly allSourceSystems$: Observable<SystemState[]>;

  /**
   * @description An observable stream of only the systems that have active ARCs, used to populate the "Active in Script" tab.
   */
  public readonly activeSystems$: Observable<SystemState[]>;

  /**
   * @description An observable stream that provides a Map of ARCs keyed by their source system name.
   * @deprecated Consider using `allSourceSystems$` and deriving ARC data from the `SystemState` objects directly.
   */
  public readonly arcsBySystem$: Observable<Map<string, AnyArc[]>>;

  /**
   * @private
   * @description A local, non-observable copy of the latest `SystemState` array. This is used for
   * direct, synchronous access to a system object when an event (like `onOpen`) provides an index.
   */
  private systemsSnapshot: SystemState[] = [];

  constructor(
    private arcStateService: ArcStateService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {
    this.allSourceSystems$ = this.arcStateService.systems$.pipe(
      tap((systems) => {
        this.systemsSnapshot = systems;
      })
    );
    this.activeSystems$ = this.arcStateService.activeSystems$;
    this.arcsBySystem$ = this.arcStateService.arcsBySystem$;
  }

  /**
   * @description Handles the `onOpen` event from the PrimeNG accordion. It identifies the system
   * that was expanded and triggers the `ArcStateService` to fetch its details (endpoints, ARCs, etc.)
   * if they haven't been loaded already.
   *
   * The logic is wrapped in `queueMicrotask` and `ngZone.onStable` to work around complexities
   * with `OnPush` change detection and asynchronous data loading initiated from a template event.
   * This ensures that Angular's change detection cycle is stable before we mark the component for a re-render.
   * @param index The index of the accordion tab that was opened, corresponding to the system's position in the `systemsSnapshot`.
   */
  public onSystemExpand(index: number): void {
    const system = this.systemsSnapshot[index];
    if (!system) return;

    // Use queueMicrotask to defer the action until after the current browser task (including rendering) is complete.
    // This prevents "Expression Changed After It Was Checked" errors with OnPush strategy.
    queueMicrotask(() => {
      this.arcStateService.ensureSystemIsLoaded(system.name).subscribe({
        next: () => {
          // Once the async data loading is complete, we wait for Angular's zone to become stable
          // before telling the ChangeDetectorRef to schedule a new check of this component and its children.
          this.ngZone.onStable.pipe().subscribe(() => {
            this.cdr.markForCheck();
          });
        },
        error: (err) => {
          // Also mark for check on error to ensure any loading spinners are hidden.
          this.ngZone.onStable.pipe().subscribe(() => {
            this.cdr.markForCheck();
          });
        },
      });
    });
  }

  // --- Public Event Emitter Methods ---

  onCreateArc(system: SourceSystem, endpoint: SourceSystemEndpoint): void {
    this.createArc.emit({ system, endpoint });
  }

  onCloneArc(
    system: SourceSystem,
    endpoint: SourceSystemEndpoint,
    arc: ApiRequestConfiguration
  ): void {
    this.cloneArc.emit({ system, endpoint, arc });
  }

  onEditArc(
    system: SourceSystem,
    endpoint: SourceSystemEndpoint,
    arc: ApiRequestConfiguration
  ): void {
    this.editArc.emit({ system, endpoint, arc });
  }

  onDeleteArc(arc: ApiRequestConfiguration): void {
    this.deleteArc.emit({ arc });
  }

  onCreateAasArc(system: SourceSystem, submodel: SubmodelDescription): void {
    this.createAasArc.emit({ system, submodel });
  }

  onEditAasArc(
    system: SourceSystem,
    submodel: SubmodelDescription,
    arc: AasArc
  ): void {
    this.editAasArc.emit({ system, submodel, arc });
  }

  onDeleteAasArc(arc: AasArc): void {
    this.deleteAasArc.emit({ arc });
  }
}
