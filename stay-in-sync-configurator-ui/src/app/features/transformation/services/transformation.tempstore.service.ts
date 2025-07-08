// stay-in-sync-configurator-ui/src/app/features/transformation/services/transformation-temp-store.service.ts
import { Injectable } from '@angular/core';
import { Transformation } from '../models/transformation.model';

@Injectable({
  providedIn: 'root'
})
export class TransformationTempStoreService {
  private addedTransformations: Transformation[] = [];

  getTransformations(): Transformation[] {
    return this.addedTransformations;
  }

  addTransformation(transformation: Transformation) {
    if (!this.addedTransformations.some(t => t.name === transformation.name)) {
      this.addedTransformations.push(transformation);
    }
  }

  removeTransformation(transformation: Transformation) {
    this.addedTransformations = this.addedTransformations.filter(t => t.name !== transformation.name);
  }

  clear() {
    this.addedTransformations = [];
  }
}
