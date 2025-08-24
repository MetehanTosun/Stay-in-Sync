import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { LogicOperatorMeta } from '../../models';

/**
 * An injectable singleton class which allows the communication with the backend
 * regarding all endpoint to manage the operator nodes of transformation graphs
 */
@Injectable({
  providedIn: 'root'
})
export class OperatorNodesApiService {
  private readonly apiUrl = '/api/config/transformation-rule/operators';

  constructor(private http: HttpClient) { }

  //#region Read Operations
  /**
   * Sends a GET request to receive a collection of all available logic operators
   */
  getOperators(): Observable<LogicOperatorMeta[]> {
    return this.http.get<LogicOperatorMeta[]>(this.apiUrl);
  }

  /**
   * Groups the all Operators by category
   * @returns Map of Operators identified by category
   */
  getGroupedOperators(): Observable<Map<string, LogicOperatorMeta[]>> {
    return this.getOperators().pipe(
      map(operators => {
        const groups = new Map<string, LogicOperatorMeta[]>();
        operators.forEach(op => {
          if (!groups.has(op.category)) {
            groups.set(op.category, []);
          }
          groups.get(op.category)!.push(op);
        });
        return groups;
      })
    );
  }
  //#endregion
}
