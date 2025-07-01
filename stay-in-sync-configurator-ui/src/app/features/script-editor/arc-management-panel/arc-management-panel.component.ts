import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ScriptEditorService } from '../../../core/services/script-editor.service';
import { ArcStateService } from '../../../core/services/arc-state.service';
import { ApiRequestConfiguration } from '../models/arc.models';
import { SourceSystem, SourceSystemEndpoint } from '../../source-system/models/source-system.models';
import { CommonModule } from '@angular/common';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { Observable } from 'rxjs';
import { FilterByEndpointPipe } from '../../../pipes/filter-by-endpoint.pipe';

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
    FilterByEndpointPipe,
  ],
  templateUrl: './arc-management-panel.component.html',
  styleUrl: './arc-management-panel.component.css'
})
export class ArcManagementPanelComponent implements OnInit {

  @Output() createArc = new EventEmitter<{ system: SourceSystem; endpoint: SourceSystemEndpoint}>();
  @Output() cloneArc = new EventEmitter<{ arc: ApiRequestConfiguration }>();

  sourceSystems: any[] = []; // Will hold {..., endpoints: [], arcs: [], isLoadingEndpoints: false }

  arcsBySystem$: Observable<Map<number, ApiRequestConfiguration[]>>;
  
  constructor(
    private scriptEditorService: ScriptEditorService,
    private arcStateService: ArcStateService
  ){
    this.arcsBySystem$ = this.arcStateService.arcsBySystem$;
  }
  
  ngOnInit(): void {
      this.scriptEditorService.getSourceSystems().subscribe(systems => {
        this.sourceSystems = systems.map(s => ({ ...s, endpoints: [], isLoadingEndpoints: false }));
      });
  }

  onSystemExpand(system: any): void {
    if (system.endpoints.length > 0 || system.isLoadingEndpoints) return;

    system.isLoadingEndpoints = true;
    this.scriptEditorService.getEndpointsForSourceSystem(system.id).subscribe(endpoints => {
      system.endpoints = endpoints;
      system.isLoadingEndpoints = false;
    });
  }

  onCreateArc(system: SourceSystem, endpoint: SourceSystemEndpoint): void {
    this.createArc.emit({system, endpoint});
  }

  onCloneArc(arc: ApiRequestConfiguration): void {
    this.cloneArc.emit({ arc });
  }
}
