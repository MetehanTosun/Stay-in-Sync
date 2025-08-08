import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { LogicOperator } from '../../models';

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

  /**
   * Sends a GET request to receive a collection of all available logic operators
   */
  getOperators(): Observable<LogicOperator[]> {
    return this.http.get<LogicOperator[]>(this.apiUrl);
  }

  /**
   * Extracts all operator categories of all available operators
   * @returns all operator categories
   */
  getOperatorCategories(): Observable<string[]> {
    return this.getOperators().pipe(
      map(operators => [...new Set(operators.map(op => op.category))])
    );
  }

  /**
   * Extracts all possible data types used in transformation graphs of all available operators
   * @returns data types used in transformation graphs
   */
  getGraphDataTypes(): Observable<string[]> {
    return this.getOperators().pipe(
      map(operators => [...new Set([
        ...operators.flatMap(op => op.inputTypes),
        ...operators.map(op => op.outputType)
      ])])
    );
  }
}
