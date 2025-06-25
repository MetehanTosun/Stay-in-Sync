import {Component, EventEmitter, Input, NgIterable, Output} from '@angular/core';
import {Dialog} from 'primeng/dialog';
import {Button} from 'primeng/button';
import {Step, StepList, StepPanel, StepPanels, Stepper} from 'primeng/stepper';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {NgForOf} from '@angular/common';
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
    SyncJobOverviewComponent
  ],
  templateUrl: './sync-job-creation.component.html',
  styleUrl: './sync-job-creation.component.css'
})
export class SyncJobCreationComponent {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  syncJobName: string = '';
  syncJobDescription: string= '';
  selectedSourceSystem: any;
  sourceSystems: (NgIterable<SourceSystem>) | undefined | null;
  isSimulation: boolean = false;
  transformations: Transformation[] = [];


  constructor(private router: Router, private aas: AasService, readonly syncJobService: SyncJobService) {
  }

  cancel() {
    this.visible = false;
    this.visibleChange.emit(false);
    this.router.navigate(['sync-jobs']);
  }

  ngOnInit() {
    this.loadSystems();
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
      //sourceSystemId: this.selectedSourceSystem.id,
      isSimulation: this.isSimulation,
      transformations: this.transformations
    };
    console.log("Creating Sync Job:", syncJob);
    this.syncJobService.create(syncJob).subscribe({
      next: (createdJob) => {
        console.log("Sync Job created successfully:", createdJob);
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
}
