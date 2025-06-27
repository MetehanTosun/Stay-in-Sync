import {SourceSystem} from '../../source-system/models/source-system.model';
import {NgIterable} from '@angular/core';
import {Transformation} from '../../transformation/models/transformation.model';

export interface SyncJob {
  id?: number;
  name?: string;
  description?: string;
  sourceSystem?: SourceSystem;
  endpoints?: (NgIterable<unknown> & NgIterable<any>) | undefined | null;
  variables?: (NgIterable<unknown> & NgIterable<any>) | undefined | null;
  transformations?: (NgIterable<Transformation>);
  isSimulation?: boolean;
}
