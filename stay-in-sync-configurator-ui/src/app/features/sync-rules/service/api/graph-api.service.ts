import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { VFlowGraphDTO } from '../../models';

/**
 * An injectable singleton class which allows the communication with the backend
 * regarding all endpoint to manage transformation graphs.
 */
@Injectable({
  providedIn: 'root'
})
export class GraphAPIService {
  private readonly apiUrl = '/api/config/transformation-rule';

  constructor(private http: HttpClient) { }

  //#region Read
  /**
   * Sends a GET request for a transformation graph.
   *
   * @param ruleId
   * @returns the frontend (vflow) graph representation
   */
  getGraph(ruleId: number): Observable<VFlowGraphDTO> {
    return this.http.get<VFlowGraphDTO>(`${this.apiUrl}/${ruleId}/graph`);
  }
  //#endregion

  //#region Update
  /**
   * Sends a PUT request for a transformation graph.
   *
   * @param ruleId
   * @param graphDto the changes that are ought to be made
   * @returns the database representation of the created graph
   */
  updateGraph(ruleId: number, graphDto: VFlowGraphDTO): Observable<VFlowGraphDTO> {
    return this.http.put<VFlowGraphDTO>(`${this.apiUrl}/${ruleId}/graph`, graphDto);
  }
  //#endregion
}
