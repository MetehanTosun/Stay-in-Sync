import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom, map, Observable } from 'rxjs';
import {
  AccessPolicy,
  OdrlContractDefinition,
  OdrlPolicyDefinition,
} from '../models/policy.model';

@Injectable({
  providedIn: 'root',
})
export class PolicyService {
  private policiesUrl = 'http://localhost:8090/api/config/edcs/access-policies';
private contractsUrl = 'http://localhost:8090/api/config/edcs/contract-definitions';


  constructor(private http: HttpClient) {}

  // ---- Access Policies ----
  getAccessPolicies(): Observable<AccessPolicy[]> {
    return this.http.get<AccessPolicy[]>(this.policiesUrl);
  }

  createAccessPolicy(policy: OdrlPolicyDefinition): Observable<OdrlPolicyDefinition> {
    return this.http.post<OdrlPolicyDefinition>(this.policiesUrl, policy);
 }
 
  uploadPolicyDefinition(policy: OdrlPolicyDefinition): Observable<OdrlPolicyDefinition> {
    return this.http.post<OdrlPolicyDefinition>(this.policiesUrl, policy);
  }

  deleteAccessPolicy(policyId: string): Observable<void> {
    return this.http.delete<void>(`${this.policiesUrl}/${policyId}`);
  }

  // ---- Contract Definitions ----
  getContractDefinitions(): Observable<OdrlContractDefinition[]> {
    return this.http.get<OdrlContractDefinition[]>(this.contractsUrl);
  }

  createContractDefinition(def: OdrlContractDefinition): Observable<OdrlContractDefinition> {
    return this.http.post<OdrlContractDefinition>(this.contractsUrl, def);
  }

updateContractDefinition(def: OdrlContractDefinition): Observable<OdrlContractDefinition> {
  return this.http.put<OdrlContractDefinition>(`${this.contractsUrl}/${def['@id']}`, def);
}


  deleteContractDefinition(id: string): Observable<void> {
    return this.http.delete<void>(`${this.contractsUrl}/${id}`);
  }
}
