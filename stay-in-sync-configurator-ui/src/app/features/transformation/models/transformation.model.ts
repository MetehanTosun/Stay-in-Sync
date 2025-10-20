import {SourceSystemApiRequestConfiguration} from '../../script-editor/models/arc.models';
import {JobDeploymentStatus} from '../../../shared/components/job-status-tag/job-status-tag.component';
import {TransformationScript} from './transformation-script.model';

export interface Transformation {
  transformationRuleId?: number | null;
  targetSystemEndpointId?: number | null;
  sourceSystemApiRequestConfigurations?: SourceSystemApiRequestConfiguration[];
  sourceSystemVariables?: any[];
  targetSystemEndpoint?: any;
  id?: number;
  name?: string;
  description?: string;
  transformationRule?: TransformationRule;
  script?: TransformationScript;
  syncJobId?: number;
  transformationScriptId?: number;
  deploymentStatus?: JobDeploymentStatus;
  added?: boolean;
}

export interface TransformationRule {
  id?: number;
  name?: string;
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
