import { HttpClient, HttpParams, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { RuleCreationDTO, TransformationRule } from "../../models";
import { Observable } from "rxjs";

/**
 * An injectable singleton class which allows the communication with the backend
 * regarding all endpoint to manage transformation rules (metadata)
 */
@Injectable({
  providedIn: 'root'
})
export class TransformationRulesApiService {
  private readonly apiUrl = '/api/config/transformation-rule';

  constructor(private http: HttpClient) { }

  //#region Create
  /**
   * Sends a POST request for a new transformation rule.
   *
   * @param newRuleDto
   * @returns the entire http response of the POST request
   */
  createRule(newRuleDto: RuleCreationDTO): Observable<HttpResponse<TransformationRule>> {
    return this.http.post<TransformationRule>(this.apiUrl, newRuleDto, {
      observe: 'response'
    });
  }
  //#endregion

  //#region Read
  /**
   * Sends a GET request for a transformation rules metadata.
   *
   * @param ruleId
   * @returns the received metadata
   */
  getRule(ruleId: number): Observable<TransformationRule> {
    return this.http.get<TransformationRule>(`${this.apiUrl}/${ruleId}`);
  }

  /**
   * Sends a GET request for all transformation rules metadata.
   *
   * @returns a collection of all transformation rules metadata
   */
  getRules(): Observable<TransformationRule[]> {
    return this.http.get<TransformationRule[]>(this.apiUrl);
  }
  //#endregion

  //#region Update
  /**
   * Sends a PUT request for a transformation rules.
   *
   * @param ruleId
   * @param updatedRuleDto partial DTO containing the updated name and/or description
   * @returns the updated transformation rules metadata
   */
  updateRule(ruleId: number, updatedRuleDto: Partial<RuleCreationDTO>): Observable<TransformationRule> {
    return this.http.put<TransformationRule>(`${this.apiUrl}/${ruleId}`, updatedRuleDto);
  }
  //#endregion

  //#region Delete
  /**
   * Sends a DELETE request far a transformation rule
   *
   * @param ruleId
   * @returns void
   */
  deleteRule(ruleId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${ruleId}`);
  }
  //#endregion
}
