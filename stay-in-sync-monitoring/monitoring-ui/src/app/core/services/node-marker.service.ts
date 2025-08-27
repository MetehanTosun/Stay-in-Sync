import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NodeMarkerService {
  private markedNodesSubject = new BehaviorSubject<{ [nodeId: string]: boolean }>({});
  markedNodes$ = this.markedNodesSubject.asObservable();

  updateMarkedNodes(markedNodes: { [nodeId: string]: boolean }) {
    this.markedNodesSubject.next(markedNodes);
  }
}
