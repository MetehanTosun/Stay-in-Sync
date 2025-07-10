import {SourceSystemApiRequestConfiguration} from '../../script-editor/models/arc.models';

export interface Transformation{
  transformationRuleId?: number | null;
  targetSystemEndpointId?: number | null;
  sourceSystemApiRequestConfiguration?: SourceSystemApiRequestConfiguration;
  id?: number;
  name?: string;
  description?: string;
  transformationRule?: string;
  script?: string;
  syncJobId?: number;
  transformationScriptId?: number;
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
