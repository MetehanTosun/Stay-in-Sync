import { Injectable } from '@angular/core';
import { TypeDefinitionsResponse } from '../../features/script-editor/models/target-system.models';
import { BehaviorSubject, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MonacoEditorService {

  private typeUpdateRequestedSource = new BehaviorSubject<TypeDefinitionsResponse | null>(null);

  public typeUpdateRequested$ = this.typeUpdateRequestedSource.asObservable();

  public requestTypeUpdate(response: TypeDefinitionsResponse): void {
    console.log('[MonacoEditorService] Type update requested. Emitting new definitions.');
    this.typeUpdateRequestedSource.next(response);
  }
}
