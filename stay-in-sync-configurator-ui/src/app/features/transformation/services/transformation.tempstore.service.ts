// stay-in-sync-configurator-ui/src/app/features/transformation/services/transformation-temp-store.service.ts
import { Injectable } from '@angular/core';
import { Transformation } from '../models/transformation.model';

@Injectable({
  providedIn: 'root'
})
export class TransformationTempStoreService {
  private readonly STORAGE_KEY = 'addedTransformations';
  private addedTransformations: Transformation[] = [];

  constructor() {
    this.loadFromStorage();
  }

  getTransformations(): Transformation[] {
    this.loadFromStorage();
    return this.addedTransformations;
  }

  addTransformation(transformation: Transformation) {
    if (!this.addedTransformations.some(t => t.id === transformation.id)) {
      this.addedTransformations.push(transformation);
      this.saveToStorage();
    }
  }

  removeTransformation(transformation: Transformation) {
    this.addedTransformations = this.addedTransformations.filter(t => t.name !== transformation.name);
    this.saveToStorage();
  }

  clear() {
    this.addedTransformations = [];
    this.saveToStorage();
  }

  private saveToStorage() {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.addedTransformations));
  }

  private loadFromStorage() {
    const storedData = localStorage.getItem(this.STORAGE_KEY);
    if (storedData) {
      this.addedTransformations = JSON.parse(storedData);
    } else {
      this.addedTransformations = [];
    }
  }
}
