import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { ArcStateService } from '../../../core/services/arc-state.service';
import { ApiRequestConfiguration } from '../models/arc.models';
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
import { combineLatest, map, Observable, shareReplay } from 'rxjs';
import { FilterByEndpointPipe } from '../../../pipes/filter-by-endpoint.pipe';

type SystemWithState = SourceSystem & {
  endpoints: SourceSystemEndpoint[];
  isLoadingEndpoints: boolean;
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
  ],
  templateUrl: './arc-management-panel.component.html',
  styleUrl: './arc-management-panel.component.css',
})
export class ArcManagementPanelComponent implements OnInit {
  @Output() createArc = new EventEmitter<{
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
  }>();
  @Output() cloneArc = new EventEmitter<{ arc: ApiRequestConfiguration }>();

  allSourceSystems$!: Observable<SystemWithState[]>;

  activeSystems$!: Observable<SystemWithState[]>;

  arcsBySystem$: Observable<Map<string, ApiRequestConfiguration[]>>;

  constructor(
    private scriptEditorService: ScriptEditorService,
    private arcStateService: ArcStateService,
    private cdr: ChangeDetectorRef
  ) {
    this.arcsBySystem$ = this.arcStateService.arcsBySystem$;
  }

  ngOnInit(): void {
    this.allSourceSystems$ = this.scriptEditorService.getSourceSystems().pipe(
      map(systems => 
        systems.map(s => ({
          ...s,
          endpoints: [],
          isLoadingEndpoints: false,
        }))
      ),
      shareReplay(1) // API call is made only once
    );

    this.activeSystems$ = combineLatest([
      this.arcStateService.arcsBySystem$,
      this.allSourceSystems$
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
  }

  onSystemExpand(system: SystemWithState): void {
    if (system.endpoints.length > 0 || system.isLoadingEndpoints) return;

    console.log("Expanding system:", system.name);

    system.isLoadingEndpoints = true;

    this.scriptEditorService
      .getEndpointsForSourceSystem(system.id)
      .subscribe((endpoints) => {
        system.endpoints = endpoints;
        system.isLoadingEndpoints = false;
        this.cdr.detectChanges();
      });
  }

  onCreateArc(system: SourceSystem, endpoint: SourceSystemEndpoint): void {
    this.createArc.emit({ system, endpoint });
  }

  onCloneArc(arc: ApiRequestConfiguration): void {
    this.cloneArc.emit({ arc });
  }
}
