import {SourceSystemApiRequestConfiguration} from '../../script-editor/models/arc.models';

export interface Transformation{
  sourceSystemApiRequestConfiguration?: SourceSystemApiRequestConfiguration;
  id?: number;
  name?: string;
  description?: string;
  transformationRule?: string;
  transformationScript?: string;
  syncJobId?: number;
}
