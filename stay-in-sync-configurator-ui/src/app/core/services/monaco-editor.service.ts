import { Injectable } from '@angular/core';
import { TypeDefinitionsResponse } from '../../features/script-editor/models/target-system.models';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class MonacoEditorService {
  public typeUpdateRequestedSource =
    new BehaviorSubject<TypeDefinitionsResponse | null>(null);

  public typeUpdateRequested$ = this.typeUpdateRequestedSource.asObservable();

  public requestTypeUpdate(response: TypeDefinitionsResponse): void {
    this.typeUpdateRequestedSource.next(response);
  }
}
