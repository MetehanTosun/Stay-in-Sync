import {SourceSystemApiRequestConfiguration} from '../../script-editor/models/arc.models';
import {JobDeploymentStatus} from '../../../shared/components/job-status-tag/job-status-tag.component';

export interface Transformation {
  transformationRuleId?: number | null;
  targetSystemEndpointId?: number | null;
  sourceSystemApiRequestConfigurations?: SourceSystemApiRequestConfiguration[];
  sourceSystemVariables?: any[];
  targetSystemEndpoint?: any;
  id?: number;
  name?: string;
  description?: string;
  transformationRule?: string;
  script?: string;
  syncJobId?: number;
  transformationScriptId?: number;
  deploymentStatus?: JobDeploymentStatus;
  added?: boolean;
}

export interface UpdateTransformationRequest {
  id?: number;
  syncJobId?: number | null;
  sourceSystemEndpointIds?: number[];
  // sourceSystemVariableIds?: number[];
  targetSystemEndpointId?: number | null;
  transformationRuleId?: number | null;
  transformationScriptId?: number | null;
}
