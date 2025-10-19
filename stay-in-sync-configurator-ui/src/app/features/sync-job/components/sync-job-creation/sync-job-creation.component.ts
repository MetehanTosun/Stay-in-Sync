import {Component, EventEmitter, inject, Input, OnInit, Output} from '@angular/core';
import {Dialog} from 'primeng/dialog';
import {Button} from 'primeng/button';
import {Step, StepList, StepPanel, StepPanels, Stepper} from 'primeng/stepper';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {SourceSystem} from '../../../source-system/models/source-system.model';
import {ToggleSwitch} from "primeng/toggleswitch";
import {FloatLabel} from "primeng/floatlabel";
import {Textarea} from "primeng/textarea";
import {
  TransformationBaseComponent
} from "../../../transformation/components/transformation-base/transformation-base.component";
import {SyncJobOverviewComponent} from '../sync-job-overview/sync-job-overview.component';
import {Transformation} from '../../../transformation/models/transformation.model';
import {SyncJobService} from '../../services/sync-job.service';
import {SyncJob} from '../../models/sync-job.model';
import {SourceSystemService} from '../../../source-system/services/source-system.service';
import {MultiSelect} from 'primeng/multiselect';
import {TransformationTempStoreService} from '../../../transformation/services/transformation.tempstore.service';
import {TransformationService} from '../../../transformation/services/transformation.service';
import {MessageService} from 'primeng/api';

/**
 * @class SyncJobCreationComponent
 * @description Angular component for creating and editing Sync Jobs.
 */
@Component({
  selector: 'app-sync-job-creation',
  standalone: true,
  imports: [
    Dialog,
    Button,
    StepPanel,
    Step,
    Stepper,
    StepList,
    StepPanels,
    FormsModule,
    InputText,
    FloatLabel,
    Textarea,
    TransformationBaseComponent,
    SyncJobOverviewComponent,
    NgSwitch,
    NgSwitchCase,
    NgIf
  ],
  templateUrl: './sync-job-creation.component.html',
  styleUrl: './sync-job-creation.component.css'
})
export class SyncJobCreationComponent implements OnInit {
  /**
   * @property {boolean} visible - Controls the visibility of the component.
   */
  @Input() visible = false;

  /**
   * @property {number | undefined} selectedSyncJobId - ID of the Sync Job being edited.
   */
  @Input() selectedSyncJobId: number | undefined;

  /**
   * @event visibleChange - Emits visibility changes of the component.
   */
  @Output() visibleChange = new EventEmitter<boolean>();

  /**
   * @event selectedSyncJobIdChange - Emits changes to the selected Sync Job ID.
   */
  @Output() selectedSyncJobIdChange = new EventEmitter<SyncJob>();

  private messageService = inject(MessageService);

  /**
   * @property {SyncJob} mySyncJob - Repräsentiert den aktuellen Sync Job, der erstellt oder bearbeitet wird.
   */
  mySyncJob: SyncJob = {
    name: ''
  };

  private _syncJobName: string = '';
  private deployed: boolean = false;

  /**
   * @property {string} syncJobName - Name des Sync Jobs.
   */
  get syncJobName(): string {
    return this._syncJobName;
  }

  set syncJobName(value: string) {
    this._syncJobName = value;
    this.updateMySyncJob();
  }

  private _syncJobDescription: string = '';

  /**
   * @property {string} syncJobDescription - Beschreibung des Sync Jobs.
   */
  get syncJobDescription(): string {
    return this._syncJobDescription;
  }

  set syncJobDescription(value: string) {
    this._syncJobDescription = value;
    this.updateMySyncJob();
  }

  private _selectedSourceSystems: SourceSystem[] = [];

  /**
   * @property {SourceSystem[]} selectedSourceSystems - Liste der ausgewählten Quellsysteme für den Sync Job.
   */
  get selectedSourceSystems(): SourceSystem[] {
    return this._selectedSourceSystems;
  }

  set selectedSourceSystems(value: SourceSystem[]) {
    this._selectedSourceSystems = value;
    this.updateMySyncJob();
  }

  /**
   * @property {SourceSystem[]} sourceSystems - Liste aller verfügbaren Quellsysteme.
   */
  sourceSystems: SourceSystem[] = [];
  private _isSimulation: boolean = false;

  /**
   * @property {boolean} isSimulation - Gibt an, ob der Sync Job im Simulationsmodus ist.
   */
  get isSimulation(): boolean {
    return this._isSimulation;
  }

  set isSimulation(value: boolean) {
    this._isSimulation = value;
    this.updateMySyncJob();
  }

  private _transformations: Transformation[] = [];

  /**
   * @property {Transformation[]} transformations - Liste der Transformationen für den Sync Job.
   */
  get transformations(): Transformation[] {
    return this._transformations;
  }

  set transformations(value: Transformation[]) {
    this._transformations = value;
    this.updateMySyncJob();
  }

// NEU: arcs und variables
  private _arcs: any[] = [];
  private _variables: any[] = [];

  /**
   * @property {any[]} arcs - Liste der Arcs für den Sync Job.
   */
  get arcs(): any[] {
    return this._arcs;
  }

  set arcs(value: any[]) {
    this._arcs = value;
    this.updateMySyncJob();
  }

  /**
   * @property {any[]} variables - Liste der Variablen für den Sync Job.
   */
  get variables(): any[] {
    return this._variables;
  }

  set variables(value: any[]) {
    this._variables = value;
    this.updateMySyncJob();
  }

  /**
   * Aktualisiert das `mySyncJob`-Objekt mit den aktuellen Werten der Komponenten-Properties.
   */
  private updateMySyncJob() {
    this.mySyncJob = {
      name: this._syncJobName,
      description: this._syncJobDescription,
      sourceSystems: this._selectedSourceSystems,
      isSimulation: this._isSimulation,
      transformations: this._transformations,
      arcs: this._arcs,
      variables: this._variables
    } as SyncJob;
  }

  /**
   * @constructor
   * @param {Router} router - Angular Router for navigation.
   * @param {SourceSystemService} sourceSystemService - Service for managing source systems.
   * @param {SyncJobService} syncJobService - Service for managing Sync Jobs.
   * @param {TransformationTempStoreService} tempStore - Temporary store for transformations.
   * @param transformationService
   */
  constructor(private router: Router, private sourceSystemService: SourceSystemService, readonly syncJobService: SyncJobService, private tempStore: TransformationTempStoreService, private transformationService: TransformationService) {
  }

  /**
   * Lifecycle hook that is called after the component is initialized.
   */
  ngOnInit() {
    console.log("Sync Job Creation Component Initialized");
    if (this.selectedSyncJobId) {
      console.log("Loading Sync Job with ID:", this.selectedSyncJobId);
      this.syncJobService.getById(this.selectedSyncJobId).subscribe({
        next: (job) => {
          this.mySyncJob = job;
          this.syncJobName = job.name || '';
          this.syncJobDescription = job.description || '';
          this.selectedSourceSystems = (job.transformations || [])
            .flatMap((t: Transformation) =>
              Array.isArray(t.sourceSystemApiRequestConfigurations)
                ? t.sourceSystemApiRequestConfigurations.map(cfg => cfg.sourceSystem)
                : []
            )
            .filter(
              (s: SourceSystem | undefined, i: number, arr: (SourceSystem | undefined)[]) =>
                s !== undefined && arr.findIndex(ss => ss && s && ss.id === s.id) === i
            ) as SourceSystem[];
          console.log("Selected Source Systems:", this.selectedSourceSystems);
          this.isSimulation = job.isSimulation || false;
          this.transformations = Array.from(job.transformations || []);
          console.log("Transformations:", this.transformations);
          this.transformations.forEach(t => this.tempStore.addTransformation(t));
          console.log("Transformations in Temp Store:", this.tempStore.getTransformations());
          this.deployed = job.deployed || false;
        },
        error: (err) => {
          console.error("Error loading Sync Job:", err);
        }
      });
    } else {
      this.resetStepperData();
    }
    this.loadSystems();
  }

  /**
   * @property {number} activeStep - Current active step in the stepper UI.
   */
  activeStep = 1;

  /**
   * Navigates to a specific step in the stepper UI.
   * @param {number} step - Step number to navigate to.
   */
  goToStep(step: number) {
    this.activeStep = step;
  }

  /**
   * Cancels the creation or editing of a Sync Job and resets the component state.
   */
  cancel() {
    this.visible = false;
    this.visibleChange.emit(false);
    this.router.navigate(['sync-jobs']);
    this.resetStepperData();
  }

  /**
   * Loads all available source systems from the backend.
   */
  private loadSystems() {
    this.sourceSystemService.getAll().subscribe({
      next: list => {
        this.sourceSystems = list.map(s => ({id: s.id, name: s.name}));
      },
      error: err => {
        console.error(err);
      }
    });
  }

  /**
   * Creates or updates a Sync Job based on the current component state.
   */
  createSyncJob() {
    const syncJob = {
      id: this.selectedSyncJobId,
      name: this.syncJobName,
      description: this.syncJobDescription,
      isSimulation: this.isSimulation,
      transformationIds: this.transformations.map(t => t.id),
      deployed: this.deployed
    };
    console.log(syncJob.transformationIds);
    if (!syncJob.name || syncJob.name.trim().length < 2) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Failed',
        detail: 'Der Name muss mindestens 2 Zeichen lang sein.'
      });
      return;
    }
    console.log("Creating Sync Job with data:", syncJob);

    if (this.selectedSyncJobId) {
      // Update (PUT)
      this.syncJobService.update(this.selectedSyncJobId, syncJob).subscribe({
        next: (updatedJob) => {
          console.log("Sync Job erfolgreich aktualisiert:", updatedJob);
          this.cancel();
          this.resetStepperData();
        },
        error: (err) => {
          console.error("Fehler beim Aktualisieren des Sync Jobs:", err);
        }
      });
    } else {
      // Create (POST)
      this.syncJobService.create(syncJob).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Sync Job Created',
            detail: `Sync Job "${syncJob.name}" created successfully.`
          });
          this.cancel();
          this.resetStepperData();
        },
        error: (err) => {
          console.error("Fehler beim Erstellen des Sync Jobs:", err);
        }
      });
    }
  }

  /**
   * Handles changes to the transformations list.
   * @param {Transformation[]} $event - Updated list of transformations.
   */
  onTransformationsChanged($event: Transformation[]) {
    this.transformations = $event;
    this.updateArcsAndVariables();
    console.log("Transformations changed:", this.transformations);
  }

  /**
   * Updates the lists of arcs and variables based on the transformations.
   * This method iterates over the transformations, extracts `sourceSystemApiRequestConfigurations`
   * and `sourceSystemVariables`, removes duplicates, and updates the corresponding properties.
   */
  private updateArcsAndVariables() {
    // Temporary arrays for all arcs and variables
    const allArcs: any[] = [];
    const allVariables: any[] = [];

    // Iterate over all transformations
    this._transformations.forEach(t => {
      // Add `sourceSystemApiRequestConfigurations` to `allArcs` if present
      if (t.sourceSystemApiRequestConfigurations && Array.isArray(t.sourceSystemApiRequestConfigurations)) {
        allArcs.push(...t.sourceSystemApiRequestConfigurations);
      }
      // Add `sourceSystemVariables` to `allVariables` if present
      if (t.sourceSystemVariables && Array.isArray(t.sourceSystemVariables)) {
        allVariables.push(...t.sourceSystemVariables);
      }
    });

    // Remove duplicates in `allArcs` and update `_arcs`
    this._arcs = Array.from(new Set(allArcs.map(a => JSON.stringify(a)))).map(a => JSON.parse(a));
    // Remove duplicates in `allVariables` and update `_variables`
    this._variables = Array.from(new Set(allVariables.map(v => JSON.stringify(v)))).map(v => JSON.parse(v));
  }

  /**
   * Resets the stepper data and clears the component state.
   */
  resetStepperData() {
    this.syncJobName = '';
    this.syncJobDescription = '';
    this.selectedSourceSystems = [];
    this.isSimulation = false;
    this.transformations = [];
    this.activeStep = 1;
    this.selectedSyncJobId = undefined;
    this.tempStore.clear();
  }
}
