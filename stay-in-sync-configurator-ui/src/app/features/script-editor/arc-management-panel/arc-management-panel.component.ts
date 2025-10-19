import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  NgZone,
  Output,
} from '@angular/core';
import { ArcStateService, SystemState } from '../../../core/services/arc-state.service';
import { AasArc, AnyArc, ApiRequestConfiguration, SubmodelDescription } from '../models/arc.models';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../source-system/models/source-system.models';
import { CommonModule } from '@angular/common';
import { AccordionModule} from 'primeng/accordion';
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
    FilterBySubmodelPipe
  ],
  templateUrl: './arc-management-panel.component.html',
  styleUrl: './arc-management-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArcManagementPanelComponent {
  @Output() createArc = new EventEmitter<{ 
    system: SourceSystem; 
    endpoint: SourceSystemEndpoint; 
  }>();
  @Output() editArc = new EventEmitter<{
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arc: ApiRequestConfiguration;
  }>();
  @Output() deleteArc = new EventEmitter<{ arc: ApiRequestConfiguration }>();
  @Output() cloneArc = new EventEmitter<{
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arc: ApiRequestConfiguration;
  }>();

  @Output() createAasArc = new EventEmitter<{ system: SourceSystem; submodel: SubmodelDescription }>();
  @Output() editAasArc = new EventEmitter<{ system: SourceSystem; submodel: SubmodelDescription; arc: AasArc }>();
  @Output() deleteAasArc = new EventEmitter<{ arc: AasArc }>();

  allSourceSystems$: Observable<SystemState[]>;
  activeSystems$: Observable<SystemState[]>;

  arcsBySystem$: Observable<Map<string, AnyArc[]>>;

  private systemsSnapshot: SystemState[] = [];

  constructor(
    private arcStateService: ArcStateService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {
    this.allSourceSystems$ = this.arcStateService.systems$.pipe(
      tap(systems => {
        this.systemsSnapshot = systems;
        console.log('%c[DEBUG 3] Panel received systems update:', 'color: orange; font-weight: bold;', JSON.parse(JSON.stringify(systems)));
      }));
    this.activeSystems$ = this.arcStateService.activeSystems$;
    this.arcsBySystem$ = this.arcStateService.arcsBySystem$;
  }

onSystemExpand(index: number): void {
  const system = this.systemsSnapshot[index];
  if (!system) return;

  // Don’t change anything directly in this tick
  queueMicrotask(() => {
    this.arcStateService.ensureSystemIsLoaded(system.name).subscribe({
      next: () => {
        // defer again after Angular stabilizes
        this.ngZone.onStable.pipe().subscribe(() => {
          this.cdr.markForCheck();
        });
      },
      error: (err) => {
        console.error(`Failed to load system '${system.name}'`, err);
        this.ngZone.onStable.pipe().subscribe(() => {
          this.cdr.markForCheck();
        });
      },
    });
  });
}

  onCreateArc(system: SourceSystem, endpoint: SourceSystemEndpoint): void {
    this.createArc.emit({ system, endpoint });
  }

  onCloneArc(system: SourceSystem, endpoint: SourceSystemEndpoint, arc: ApiRequestConfiguration): void {
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

  onEditAasArc(system: SourceSystem, submodel: SubmodelDescription, arc: AasArc): void {
    this.editAasArc.emit({ system, submodel, arc });
  }

  onDeleteAasArc(arc: AasArc): void {
    this.deleteAasArc.emit({ arc });
  }
}
