import { HttpClient, HttpResponse } from "@angular/common/http";
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

  /**
   * Sends a POST request for the creation of a new transformation rule.
   *
   * @param newRuleDto
   * @returns the entire http response of the POST request
   */
  createRule(newRuleDto: RuleCreationDTO): Observable<HttpResponse<TransformationRule>> {
    return this.http.post<TransformationRule>(this.apiUrl, newRuleDto, {
      observe: 'response'
    });
  }

  /**
   * Sends a GET request to read a specific transformation rules metadata.
   *
   * @param ruleId
   * @returns a specific transformation rules metadata
   */
  getRule(ruleId: number): Observable<TransformationRule> {
    return this.http.get<TransformationRule>(`${this.apiUrl}/${ruleId}`);
  }

  /**
   * Sends a GET request to receive a collection of all transformation rules metadata.
   *
   * @returns a collection of all transformation rules metadata
   */
  getRules(): Observable<TransformationRule[]> {
    return this.http.get<TransformationRule[]>(this.apiUrl);
  }

  /**
   * Sends a PUT request to update a specific transformation rules name or description.
   *
   * @param ruleId
   * @param updatedRuleDto
   * @returns the updated transformation rules metadata
   */
  updateRule(ruleId: number, updatedRuleDto: Partial<RuleCreationDTO>): Observable<TransformationRule> {
    return this.http.put<TransformationRule>(`${this.apiUrl}/${ruleId}`, updatedRuleDto);
  }

  /**
   * Sends a DELETE request to delete a specific transformation rule
   *
   * @param ruleId
   * @returns void
   */
  deleteRule(ruleId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${ruleId}`);
  }
}
