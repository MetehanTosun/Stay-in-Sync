import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Dialog} from 'primeng/dialog';
import {Button} from 'primeng/button';
import {Step, StepList, StepPanel, StepPanels, Stepper} from 'primeng/stepper';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {NgSwitch, NgSwitchCase} from '@angular/common';
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

@Component({
  selector: 'app-sync-job-creation',
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
    ToggleSwitch,
    FloatLabel,
    Textarea,
    TransformationBaseComponent,
    SyncJobOverviewComponent,
    NgSwitch,
    NgSwitchCase,
    MultiSelect
  ],
  templateUrl: './sync-job-creation.component.html',
  styleUrl: './sync-job-creation.component.css'
})
export class SyncJobCreationComponent implements OnInit {
  @Input() visible = false;
  @Input() selectedSyncJobId: number | undefined;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() selectedSyncJobIdChange = new EventEmitter<SyncJob>();

  mySyncJob: SyncJob = {};

  private _syncJobName: string = '';
  private deployed: boolean = false;
  get syncJobName(): string {
    return this._syncJobName;
  }
  set syncJobName(value: string) {
    this._syncJobName = value;
    this.updateMySyncJob();
  }

  private _syncJobDescription: string = '';
  get syncJobDescription(): string {
    return this._syncJobDescription;
  }
  set syncJobDescription(value: string) {
    this._syncJobDescription = value;
    this.updateMySyncJob();
  }

  private _selectedSourceSystems: SourceSystem[] = [];
  get selectedSourceSystems(): SourceSystem[] {
    return this._selectedSourceSystems;
  }
  set selectedSourceSystems(value: SourceSystem[]) {
    this._selectedSourceSystems = value;
    this.updateMySyncJob();
  }


  sourceSystems: SourceSystem[] = [];
  private _isSimulation: boolean = false;
  get isSimulation(): boolean {
    return this._isSimulation;
  }
  set isSimulation(value: boolean) {
    this._isSimulation = value;
    this.updateMySyncJob();
  }

  private _transformations: Transformation[] = [];
  get transformations(): Transformation[] {
    return this._transformations;
  }
  set transformations(value: Transformation[]) {
    this._transformations = value;
    this.updateMySyncJob();
  }

  private updateMySyncJob() {
    this.mySyncJob = {
      name: this._syncJobName,
      description: this._syncJobDescription,
      sourceSystems: this._selectedSourceSystems,
      isSimulation: this._isSimulation,
      transformations: this._transformations
    } as SyncJob;
  }



  constructor(private router: Router, private sourceSystemService: SourceSystemService, readonly syncJobService: SyncJobService, private tempStore: TransformationTempStoreService) {}

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
            .map((t: Transformation) => t.sourceSystemApiRequestConfiguration ? [t.sourceSystemApiRequestConfiguration.sourceSystem] : [])
            .reduce((acc: SourceSystem[], val: SourceSystem[]) => acc.concat(val), [])
            .filter((s: SourceSystem | undefined, i: number, arr: (SourceSystem | undefined)[]) =>
              s !== undefined && arr.findIndex((ss) => ss && s && ss.id === s.id) === i
            ) as SourceSystem[];
          console.log("Selected Source Systems:", this.selectedSourceSystems);
          this.isSimulation = job.isSimulation || false;
          this.transformations = Array.from(job.transformations || []);
          console.log("Loaded Transformations:", this.transformations);
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

  activeStep = 1;

  goToStep(step: number) {
    this.activeStep = step;
  }


  cancel() {
    this.visible = false;
    this.visibleChange.emit(false);
    this.router.navigate(['sync-jobs']);
    this.resetStepperData();
  }


  private loadSystems() {
    this.sourceSystemService.getAll().subscribe({
      next: list => {
        this.sourceSystems = list.map(s => ({ id: s.id, name: s.name}));
      },
      error: err => {
        console.error(err);
      }
    });
  }

  createSyncJob() {
    const syncJob = {
      id: this.selectedSyncJobId,
      name: this.syncJobName,
      description: this.syncJobDescription,
      isSimulation: this.isSimulation,

      transformations: this.transformations.map(t => ({
        id: t.id,
        name: t.name,
        description: t.description,
        transformationRule: t.transformationRule,
        transformationScript: t.script,
      })),
      deployed: this.deployed
    };
    console.log("Creating Sync Job with data:", syncJob);

    if (this.selectedSyncJobId) {
      // Update (PUT)
      this.syncJobService.update(this.selectedSyncJobId, syncJob).subscribe({
        next: (updatedJob) => {
          console.log("Sync Job erfolgreich aktualisiert:", updatedJob);
          this.cancel();
        },
        error: (err) => {
          console.error("Fehler beim Aktualisieren des Sync Jobs:", err);
        }
      });
    } else {
      // Create (POST)
      this.syncJobService.create(syncJob).subscribe({
        next: (createdJob) => {
          this.cancel();
        },
        error: (err) => {
          console.error("Fehler beim Erstellen des Sync Jobs:", err);
        }
      });
    }
  }

  onTransformationsChanged($event: Transformation[]) {
    this.transformations = $event;
    console.log("Transformations changed:", this.transformations);
  }

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
