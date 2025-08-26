import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  OdrlPolicyDefinition,
  OdrlContractDefinition,
} from '../models/policy.model';

@Injectable({
  providedIn: 'root',
})
export class PolicyService {
  private backendUrl = 'http://localhost:8090/api/config/policies';
  private contractDefUrl = 'http://localhost:8090/api/config/edcs/contract-definitions';

  constructor(private http: HttpClient) {}

  /**
   * Holt alle Policies und entpackt das DTO so, dass es wie OdrlPolicyDefinition aussieht.
   */
  getPolicies(): Observable<OdrlPolicyDefinition[]> {
    return this.http.get<any[]>(this.backendUrl).pipe(
      map((dtos: any[]) => {
        console.log('Policies DTOs from backend:', dtos);
        return dtos.map(dto => ({
          ...dto.policy,               // entpacke ODRL-Struktur
          id: dto.policy?.['@id'],     // fürs UI
          dbId: dto.id,                // DB-UUID
          policyId: dto.policyId,      // Business-Key
        }));
      })
    );
  }

  /**
   * Lädt eine einzelne Policy
   */
  getPolicy(dbId: string): Observable<OdrlPolicyDefinition> {
    return this.http.get<any>(`${this.backendUrl}/${dbId}`).pipe(
      map(dto => ({
        ...dto.policy,
        id: dto.policy?.['@id'],
        dbId: dto.id,
        policyId: dto.policyId,
      }))
    );
  }

  /**
   * Neue Policy anlegen → schickt nur policyId + Policy-Struktur
   */
// policy.service.ts
uploadPolicyDefinition(raw: any) {
  // 1) Robuste Normalisierung aus Editor:
  //    - Erlaube sowohl { permission: [...] } als auch { policy: { permission: [...] } }
  const permission = Array.isArray(raw?.permission)
    ? raw.permission
    : Array.isArray(raw?.policy?.permission)
      ? raw.policy.permission
      : [];

  const normalizedPolicy = {
    '@context': raw?.['@context'] ?? { odrl: 'http://www.w3.org/ns/odrl/2/' },
    '@id': String(raw?.['@id'] ?? '').trim(),
    permission
  };

  const dto = {
    policyId: normalizedPolicy['@id'],
    policy: normalizedPolicy
  };

  console.log('[PolicyService] Uploading DTO ->', dto); // <-- siehst du im Browser

  // 2) POST + Fehlerdetails loggen
  return this.http.post(this.backendUrl, dto, { observe: 'response' });
}



  /**
   * Bestehende Policy updaten → DB-UUID notwendig
   */
  updatePolicyDefinition(policy: OdrlPolicyDefinition): Observable<any> {
    if (!policy.dbId) {
      throw new Error('Cannot update policy without dbId');
    }
    const dto = {
      id: policy.dbId,
      policyId: policy['@id'],
      policy: {
        '@context': policy['@context'],
        '@id': policy['@id'],
        permission: policy.permission || policy.policy?.permission || [],
      },
    };
    return this.http.put(`${this.backendUrl}/${policy.dbId}`, dto);
  }

  /**
   * Policy löschen (immer mit DB-UUID)
   */
  deletePolicy(dbId: string): Observable<void> {
    return this.http.delete<void>(`${this.backendUrl}/${dbId}`);
  }

  // --------------------------------------------------------
  // CONTRACT DEFINITIONS
  // --------------------------------------------------------

  getContractDefinitions(): Observable<OdrlContractDefinition[]> {
    return this.http.get<OdrlContractDefinition[]>(this.contractDefUrl);
  }

  createContractDefinition(cd: OdrlContractDefinition): Observable<any> {
    return this.http.post(this.contractDefUrl, cd);
  }

  updateContractDefinition(cd: OdrlContractDefinition): Observable<any> {
    return this.http.put(`${this.contractDefUrl}/${cd['@id']}`, cd);
  }

  deleteContractDefinition(id: string): Observable<void> {
    return this.http.delete<void>(`${this.contractDefUrl}/${id}`);
  }
}
