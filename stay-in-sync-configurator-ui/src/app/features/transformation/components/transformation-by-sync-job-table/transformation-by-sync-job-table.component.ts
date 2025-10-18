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
import {Button, ButtonDirective} from 'primeng/button';
import {Dialog} from 'primeng/dialog';
import {TransformationAddSyncJobComponent} from '../transformation-add-sync-job/transformation-add-sync-job.component';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {TransformationService} from '../../services/transformation.service';
import {MessageService} from 'primeng/api';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {SyncJobService} from '../../../sync-job/services/sync-job.service';
import {forkJoin} from 'rxjs';
import {TransformationRulesApiService} from '../../../sync-rules/service';
import {TransformationRule} from '../../../sync-rules/models';

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
        Dialog,
        TransformationAddSyncJobComponent,
        Button,
        RouterLink,
        ButtonDirective
    ],
  templateUrl: './transformation-by-sync-job-table.component.html',
  styleUrl: './transformation-by-sync-job-table.component.css'
})
export class TransformationBySyncJobTableComponent implements OnInit {

  @ViewChild(TransformationAddSyncJobComponent) addTransformationComponent!: TransformationAddSyncJobComponent;


  statusOptions = Object.values(JobDeploymentStatus);

  selectedStatus?: JobDeploymentStatus;

  transformations: Transformation[] = [];

  allTransformationRules: TransformationRule[] = [];

  addTransformationVisible: boolean = false;

  syncJobId?: number;

  constructor(private readonly route: ActivatedRoute,
              private readonly router: Router,
              private readonly transformationService: TransformationService,
              private readonly transformationRuleService: TransformationRulesApiService,
              private readonly syncJobService: SyncJobService,
              private readonly messageService: MessageService,
              private readonly httpErrorService: HttpErrorService,
              private cdr: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.syncJobId = Number.parseInt(this.route.snapshot.paramMap.get('id')!);
    forkJoin({
      transformations: this.transformationService.getBySyncJobId(this.syncJobId),
      rules: this.transformationRuleService.getRules()
    }).subscribe({
      next: ({ transformations, rules }) => {
        this.transformations = transformations;
        this.allTransformationRules = rules;
        this.transformations.forEach(transformation => {
          if (!transformation.transformationRule) {
            transformation.transformationRule = {};
          }
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

  getSelectableTransformationRules(currentRowData: any) {
    return this.allTransformationRules.map(rule => ({
      ...rule,
      disabled: rule.id !== null && rule.id !== currentRowData.transformationRule?.id
    }));
  }

  openCreateDialog() {
    this.addTransformationVisible = true;
  }

  loadSyncJobDetails(id: number): void {
    this.transformationService.getBySyncJobId(id).subscribe({
      next: data => {
        this.transformations = data;
        this.transformations.forEach(transformation => {
          if (!transformation.transformationRule) {
            transformation.transformationRule = {};
          }
        });
      },
      error: err => {
        this.httpErrorService.handleError(err);
      }
    });
  }

  loadTransformationRules(): void {
    this.transformationRuleService.getRules().subscribe({
      next: data => {
        this.allTransformationRules = data;
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
    console.log('YAS')
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
          this.loadTransformationRules();
        },
        error: err => {
          this.httpErrorService.handleError(err);
        }
      });
    } else {
      this.transformationService.removeRule(transformation.id!).subscribe({
        next: data => {
          this.loadTransformationRules();
        },
        error: err => {
          this.httpErrorService.handleError(err);
        }
      });
    }
  }
}
