import {Component, EventEmitter, Input, NgIterable, Output} from '@angular/core';
import {Dialog} from 'primeng/dialog';
import {Button} from 'primeng/button';
import {Step, StepList, StepPanel, StepPanels, Stepper} from 'primeng/stepper';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {NgForOf, NgSwitch, NgSwitchCase} from '@angular/common';
import {SourceSystem} from '../../../source-system/models/source-system.model';
import {AasService} from "../../../source-system/services/aas.service";
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
    NgForOf,
    ToggleSwitch,
    FloatLabel,
    Textarea,
    TransformationBaseComponent,
    SyncJobOverviewComponent,
    NgSwitch,
    NgSwitchCase
  ],
  templateUrl: './sync-job-creation.component.html',
  styleUrl: './sync-job-creation.component.css'
})
export class SyncJobCreationComponent {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  mySyncJob: SyncJob = {};

  private _syncJobName: string = '';
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

  private _selectedSourceSystem: any;
  get selectedSourceSystem(): any {
    return this._selectedSourceSystem;
  }
  set selectedSourceSystem(value: any) {
    this._selectedSourceSystem = value;
    this.updateMySyncJob();
  }

  sourceSystems: (NgIterable<SourceSystem>) | undefined | null;
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
      sourceSystemId: this._selectedSourceSystem?.id,
      isSimulation: this._isSimulation,
      transformations: this._transformations
    } as SyncJob;
  }



  constructor(private router: Router, private aas: AasService, readonly syncJobService: SyncJobService) {
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
  }


  private loadSystems() {
    this.aas.getAll().subscribe({
      next: list => {
        this.sourceSystems = list.map(s => ({ id: s.id, name: s.name}));
      },
      error: err => {
        console.error(err);
      }
    });
  }

  createSyncJob() {
    // if (!this.syncJobName || !this.selectedSourceSystem || !this.transformations.length) {
    //   console.error("Please fill in all required fields.");
    //   return;
    // }
    const syncJob = {
      name: this.syncJobName,
      description: this.syncJobDescription,
      isSimulation: this.isSimulation,
      transformations: this.transformations
    };
    console.log("Creating Sync Job:", syncJob);
    this.syncJobService.create(syncJob).subscribe({
      next: (createdJob) => {
        console.log("Sync Job created successfully:", createdJob);
        this.resetStepperData();
        this.cancel();
      },
      error: (err) => {
        console.error("Error creating Sync Job:", err);
      }
    })
  }

  onTransformationsChanged($event: Transformation[]) {
    this.transformations = $event;
    console.log("Transformations changed:", this.transformations);
  }

  resetStepperData() {
    this.syncJobName = '';
    this.syncJobDescription = '';
    this.selectedSourceSystem = null;
    this.isSimulation = false;
    this.transformations = [];
    this.activeStep = 1; // Optional: Zurück zum ersten Schritt
  }
}
