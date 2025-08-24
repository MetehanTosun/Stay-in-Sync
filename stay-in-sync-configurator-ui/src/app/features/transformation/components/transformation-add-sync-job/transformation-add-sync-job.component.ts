import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {
  JobDeploymentStatus,
  JobStatusTagComponent
} from '../../../../shared/components/job-status-tag/job-status-tag.component';
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ToggleButtonModule} from 'primeng/togglebutton';
import {Dialog} from 'primeng/dialog';
import {NgIf} from '@angular/common';
import {Select} from 'primeng/select';

import {CheckboxModule} from 'primeng/checkbox';
import {Transformation} from '../../models/transformation.model';
import {TransformationService} from '../../services/transformation.service';
import {SyncJobService} from '../../../sync-job/services/sync-job.service';
import {SyncJob} from '../../../source-system/models/syncJob';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-transformation-add-sync-job',
  standalone: true,
  imports: [   TableModule,
    Button,
    FormsModule,
    ToggleButtonModule,
    Dialog,
    ReactiveFormsModule,

    NgIf,
    Select,
    JobStatusTagComponent,
    CheckboxModule],
  templateUrl: './transformation-add-sync-job.component.html',
  styleUrl: './transformation-add-sync-job.component.css'
})
export class TransformationAddSyncJobComponent implements OnInit{

  @Output() transformationAdded = new EventEmitter<void>();


  statusOptions = Object.values(JobDeploymentStatus);

  selectedStatus?: JobDeploymentStatus;

  transformations: Transformation[] = [];
  private syncJobId?: number;

  constructor(private readonly route: ActivatedRoute, private readonly transformationService: TransformationService, private readonly syncJobService: SyncJobService) {


  }

  loadUnanssignedTransformations() {
    this.transformationService.getAllWithoutSyncJob().subscribe(
      {next: data => {
          console.log(data);
          this.transformations = data;
        },
        error: err => {
          console.log(err);
        }}
    )
  }

  addRule(rowData: Transformation) {
  }

  add(rowData: Transformation) {
    this.syncJobService.addTransformation(this.syncJobId!, rowData.id!).subscribe({
      next: data => {
        this.loadUnanssignedTransformations();
        this.transformationAdded.emit();
      },
      error: err => {
        console.log(err);
      }
    });
  }

  ngOnInit(): void {
    this.syncJobId = Number.parseInt(this.route.snapshot.paramMap.get('id')!);

    this.loadUnanssignedTransformations();
  }
}
