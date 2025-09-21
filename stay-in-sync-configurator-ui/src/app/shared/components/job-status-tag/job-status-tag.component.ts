import {Component, EventEmitter, Input, Output} from '@angular/core';
import {TagModule} from 'primeng/tag';

@Component({
  selector: 'app-job-status-tag',
  standalone: true,
  imports: [TagModule],
  templateUrl: './job-status-tag.component.html',
  styleUrl: './job-status-tag.component.css'
})
export class JobStatusTagComponent {
  @Input() status: JobDeploymentStatus = JobDeploymentStatus.STOPPING;

  @Input() interactive: boolean = false;

  @Output() click = new EventEmitter<void>();

  getStatusLabel(status: JobDeploymentStatus): string {
    return status;
  }

  getStatusSeverity(status: JobDeploymentStatus): string {
    switch (status) {
      case JobDeploymentStatus.DEPLOYED:
        return 'success';
      case JobDeploymentStatus.FAILING:
        return 'danger';
      case JobDeploymentStatus.STOPPING:
        return 'warning';
      case JobDeploymentStatus.UNDEPLOYED:
        return 'secondary';
      default:
        return 'info';
    }
  }

  getStatusIcon(status: JobDeploymentStatus): string {
    switch (status) {
      case JobDeploymentStatus.DEPLOYED:
        return 'pi pi-check-circle';
      case JobDeploymentStatus.FAILING:
        return 'pi pi-exclamation-triangle';
      case JobDeploymentStatus.UNDEPLOYED:
        return 'pi pi-stop-circle';
      case JobDeploymentStatus.STOPPING:
      case JobDeploymentStatus.DEPLOYING:
      case JobDeploymentStatus.RECONFIGURING:
        return 'pi pi-spin pi-spinner';
      default:
        return 'pi pi-info-circle';
    }
  }


  clicked() {

  }
}


export enum JobDeploymentStatus {
  DEPLOYED = 'DEPLOYED',
  DEPLOYING = 'DEPLOYING',
  FAILING = 'FAILING',
  RECONFIGURING = 'RECONFIGURING',
  STOPPING = 'STOPPING',
  UNDEPLOYED = 'UNDEPLOYED'
}
