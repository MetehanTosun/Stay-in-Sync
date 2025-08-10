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

  //#region Read Operations
  /**
   * Sends a GET request to read a specific transformation graph.
   *
   * @param ruleId
   * @returns the frontend (vflow) graph representation
   */
  getGraph(ruleId: number): Observable<VFlowGraphDTO> {
    return this.http.get<VFlowGraphDTO>(`${this.apiUrl}/${ruleId}/graph`);
  }
  //#endregion

  //#region Update Operations
  /**
   * Sends a PUT request to update a specific transformation graph.
   *
   * @param ruleId
   * @param graphDto
   * @returns the database representation of the created graph
   */
  updateGraph(ruleId: number, graphDto: VFlowGraphDTO): Observable<unknown> {
    return this.http.put<unknown>(`${this.apiUrl}/${ruleId}`, graphDto);
  }
  //#endregion
}
