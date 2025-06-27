// transformation-state.service.ts
import { Injectable } from '@angular/core';
import {Transformation} from '../models/transformation.model';

@Injectable({ providedIn: 'root' })
export class TransformationStateService {
  transformations: Transformation[] = [];
}
