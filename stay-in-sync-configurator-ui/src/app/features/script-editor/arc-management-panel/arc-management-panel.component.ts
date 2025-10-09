import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  OnInit,
  Output,
} from '@angular/core';
import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { ArcStateService } from '../../../core/services/arc-state.service';
import { AasArc, AnyArc, ApiRequestConfiguration, SubmodelDescription } from '../models/arc.models';
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
import { combineLatest, map, Observable, shareReplay, Subscription } from 'rxjs';
import { FilterByEndpointPipe } from '../../source-system/pipes/filter-by-endpoint.pipe';
import { AasService } from '../../source-system/services/aas.service';
import { FilterByTypePipe } from '../../source-system/pipes/filter-by-type.pipe';
import { FilterBySubmodelPipe } from '../../source-system/pipes/filter-by-submodel.pipe';

type SystemWithState = SourceSystem & {
  endpoints?: SourceSystemEndpoint[];
  submodels?: SubmodelDescription[];
  isLoading: boolean;
};

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
})
export class ArcManagementPanelComponent implements OnInit {
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

  allSourceSystems$!: Observable<SystemWithState[]>;

  activeSystems$!: Observable<SystemWithState[]>;

  arcsBySystem$: Observable<Map<string, AnyArc[]>>;

  private stateSubscription: Subscription | undefined;

  constructor(
    private scriptEditorService: ScriptEditorService,
    private arcStateService: ArcStateService,
    private aasService: AasService,
    private cdr: ChangeDetectorRef
  ) {
    this.arcsBySystem$ = this.arcStateService.arcsBySystem$ as Observable<Map<string, AnyArc[]>>;
  }

  ngOnInit(): void {
    this.allSourceSystems$ = this.scriptEditorService.getSourceSystems().pipe(
      map((systems) =>
        systems.map((s) => ({
          ...s,
          endpoints: [],
          isLoading: false,
        }))
      ),
      shareReplay(1) // API call is made only once
    );

    this.activeSystems$ = combineLatest([
      this.arcStateService.arcsBySystem$,
      this.allSourceSystems$,
    ]).pipe(
      map(([arcMap, allSystemsWithState]) => {
        const activeSystemNames = Array.from(arcMap.keys());
        if (activeSystemNames.length === 0) {
          return [];
        }

        return allSystemsWithState.filter((system) =>
          activeSystemNames.includes(system.name)
        );
      })
    );

    this.stateSubscription = combineLatest([
      this.arcStateService.arcsBySystem$,
      this.allSourceSystems$
    ]).subscribe(([arcMap, allSystems]) => {
      const systemNamesWithArcs = Array.from(arcMap.keys());
      
      systemNamesWithArcs.forEach(systemName => {
        const system = allSystems.find(s => s.name === systemName);

        if (system && system.endpoints?.length === 0 && !system.isLoading) {
          console.log(`%c[Panel] Reactively fetching endpoints for '${system.name}' because new ARCs were detected.`, 'color: #8b5cf6;');
          this.reactivelyLoadSystemDetails(system);
        }
      });
    });
  }

  private reactivelyLoadSystemDetails(system: SystemWithState): void {
    if (system.apiType === 'AAS') {
      if (!system.submodels && !system.isLoading) {
        console.log(`%c[Panel] Reactively fetching submodels for '${system.name}'...`, 'color: #8b5cf6;');
        this.ensureSubmodelsLoaded(system);
      }
    } else {
      if (system.endpoints?.length === 0 && !system.isLoading) {
        console.log(`%c[Panel] Reactively fetching endpoints for '${system.name}'...`, 'color: #8b5cf6;');
        this.ensureEndpointsLoaded(system);
      }
    }
  }

  onSystemExpand(index:number, systems: SystemWithState[]): void {
    const system = systems[index];
    if(!system) return;

    if (system.apiType === 'AAS') {
      this.ensureSubmodelsLoaded(system);
    } else {
      this.ensureEndpointsLoaded(system);
    }
    
    this.arcStateService.loadArcsForSourceSystem(system.name, system.id).subscribe();
  }

  public ensureEndpointsLoaded(system: SystemWithState): void {
    if (system && system.endpoints?.length === 0 && !system.isLoading) {
      console.log(`%c[Panel] Parent commanded to ensure endpoints are loaded for '${system.name}'. Fetching now.`, 'color: #f97316;');
      system.isLoading = true;
      this.scriptEditorService
        .getEndpointsForSourceSystem(system.id)
        .subscribe((endpoints) => {
          system.endpoints = endpoints;
          system.isLoading = false;
          this.cdr.detectChanges();
        });
    }
  }

  public ensureSubmodelsLoaded(system: SystemWithState): void {
    if (system && !system.submodels && !system.isLoading) {
      system.isLoading = true;
      this.aasService.listSubmodels(system.id).subscribe(submodels => {
        system.submodels = submodels;
        system.isLoading = false;
        this.cdr.detectChanges();
      });
    }
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

  ngOnDestroy(): void {
    if (this.stateSubscription) {
      this.stateSubscription.unsubscribe();
    }
  }
}
