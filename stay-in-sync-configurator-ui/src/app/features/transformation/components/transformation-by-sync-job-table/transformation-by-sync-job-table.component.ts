import {ChangeDetectorRef, Component, OnInit, ViewChild} from '@angular/core';
import {
  JobDeploymentStatus,
  JobStatusTagComponent
} from '../../../../shared/components/job-status-tag/job-status-tag.component';
import {TableModule} from 'primeng/table';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ToggleButtonModule} from 'primeng/togglebutton';

import {Select} from 'primeng/select';
import {CheckboxModule} from 'primeng/checkbox';
import {Transformation} from '../../models/transformation.model';
import {Button} from 'primeng/button';
import {NgIf} from '@angular/common';
import {Card} from "primeng/card";
import {Toolbar} from "primeng/toolbar";
import {Dialog} from 'primeng/dialog';
import {TransformationAddSyncJobComponent} from '../transformation-add-sync-job/transformation-add-sync-job.component';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {TransformationService} from '../../services/transformation.service';
import {MessageService} from 'primeng/api';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {SyncJobService} from '../../../sync-job/services/sync-job.service';
import {TransformationRuleModel} from '../../models/transformation-rule.model';
import {forkJoin} from 'rxjs';

@Component({
  selector: 'app-transformation-by-sync-job-table',
  standalone: true,
  imports: [
    TableModule,
    FormsModule,
    ToggleButtonModule,
    ReactiveFormsModule,
    Select,
    JobStatusTagComponent,
    CheckboxModule,
    Button,
    Button,
    NgIf,
    Card,
    Toolbar,
    Dialog,
    TransformationAddSyncJobComponent,
    Button,
    RouterLink
  ],
  templateUrl: './transformation-by-sync-job-table.component.html',
  styleUrl: './transformation-by-sync-job-table.component.css'
})
export class TransformationBySyncJobTableComponent implements OnInit {

  @ViewChild(TransformationAddSyncJobComponent) addTransformationComponent!: TransformationAddSyncJobComponent;


  statusOptions = Object.values(JobDeploymentStatus);

  selectedStatus?: JobDeploymentStatus;

  transformations: Transformation[] = [];

  transformationsRules: TransformationRuleModel[] = [];

  addTransformationVisible: boolean = false;

  syncJobId?: number;

  constructor(private readonly route: ActivatedRoute,
              private readonly router: Router,
              private readonly transformationService: TransformationService,
              private readonly syncJobService: SyncJobService,
              private readonly messageService: MessageService,
              private readonly httpErrorService: HttpErrorService,
              private cdr: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.syncJobId = Number.parseInt(this.route.snapshot.paramMap.get('id')!);
    forkJoin({
      transformations: this.transformationService.getBySyncJobId(this.syncJobId),
      rules: this.transformationService.getAllRules()
    }).subscribe({
      next: ({ transformations, rules }) => {
        this.transformationsRules = rules;
        this.transformations = transformations;

        transformations.forEach(t => {
          console.log(`Transformation ${t.id} has transformationScriptId: ${t.transformationScriptId}`);
          const matchingRule = rules.find(r => r.id === t.transformationScriptId);
          console.log('Matching rule:', matchingRule);
        });
      },
      error: err => {
        console.log(err);
      }
    });

    this.transformationService.watchDeploymentStatus(this.syncJobId).subscribe({
      next: data => {
        this.transformations = this.transformations.map(transformation =>
          transformation.id === data.transformationId
            ? {...transformation, deploymentStatus: data.deploymentStatus} // Use actual status from data
            : transformation
        );
        this.cdr.detectChanges();
      },
      error: err => {
        console.error(err);
      }
    });

  }

  openCreateDialog() {
    this.addTransformationVisible = true;
  }

  loadSyncJobDetails(id: number): void {
    this.transformationService.getBySyncJobId(id).subscribe({
      next: data => {
        this.transformations = data;
      },
      error: err => {
        console.log(err);
      }
    });
  }

  loadTransformationRules(): void {
    this.transformationService.getAllRules().subscribe({
      next: data => {
        this.transformationsRules = data;
      },
      error: err => {
        console.log(err);
      }
    });
  }

  remove(transformation: Transformation) {
    if (transformation.deploymentStatus !== JobDeploymentStatus.UNDEPLOYED) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Unsupported operation',
        detail: 'Only undeployed transformations can be removed!',
        life: 5000
      });
      return;
    }

    this.syncJobService.removeTransformation(this.syncJobId!, transformation.id!).subscribe({
      next: data => {
        this.loadSyncJobDetails(this.syncJobId!)
        this.addTransformationComponent.loadUnanssignedTransformations();
      },
      error: err => {
        console.log(err);
      }
    });
  }

  toggleDeploymentStatus(transformation: Transformation) {
    console.log("TOGGLS " + transformation.deploymentStatus,)
    switch (transformation.deploymentStatus) {
      case JobDeploymentStatus.FAILING:
      case JobDeploymentStatus.DEPLOYED:
        this.transformationService.manageDeployment(transformation.id!, JobDeploymentStatus.STOPPING).subscribe({
          complete: () => {
          },
          error: err => {
            this.httpErrorService.handleError(err);
          }
        });
        break;
      case JobDeploymentStatus.UNDEPLOYED:
        this.transformationService.manageDeployment(transformation.id!, JobDeploymentStatus.DEPLOYING).subscribe({
          next: data => {
          },
          error: err => {
            this.httpErrorService.handleError(err);
          }
        });
        break;
      case JobDeploymentStatus.DEPLOYING:
      case JobDeploymentStatus.STOPPING:
      case JobDeploymentStatus.RECONFIGURING:

        this.messageService.add({
          severity: 'warn',
          summary: 'Unsupported operation',
          detail: 'While a transformation is transitioning its deployment status can not be changed',
          life: 5000
        });

        break;
      default:
        break;
    }


  }

  onRuleChange(rule: number, transformation: Transformation) {
    if (rule) {
      this.transformationService.addRule(transformation.id!, rule!).subscribe({
        next: data => {
        },
        error: err => {
          this.httpErrorService.handleError(err);
        }
      });
    } else {
      this.transformationService.removeRule(transformation.id!).subscribe({
        next: data => {
        },
        error: err => {
          this.httpErrorService.handleError(err);
        }
      });
    }
  }
}
