import {JobDeploymentStatus} from '../../../shared/components/job-status-tag/job-status-tag.component';

export interface TransformationStatusUpdate {
  transformationId: number;
  syncJobId: number;
  deploymentStatus: JobDeploymentStatus;
}
