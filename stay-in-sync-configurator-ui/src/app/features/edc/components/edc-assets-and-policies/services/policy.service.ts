import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, map, of, delay, tap } from 'rxjs';
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
  private baseUrl = 'http://localhost:8090/api/config/edcs';

  constructor(private http: HttpClient) {}

  /**
   * Holt alle Policies und entpackt das DTO so, dass es wie OdrlPolicyDefinition aussieht.
   */
  // getPolicies(): Observable<OdrlPolicyDefinition[]> {
  //   return this.http.get<any[]>(this.backendUrl).pipe(
  getPolicies(edcId: string): Observable<OdrlPolicyDefinition[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${edcId}/policies`).pipe(
      map((dtos: any[]) => {
        console.log('Policies DTOs from backend:', dtos);
        if (dtos && dtos.length > 0) {
          console.log('Example policy structure:', JSON.stringify(dtos[0], null, 2));
        }

        const extractBpn = (policy: any): string | undefined => {
          const perms = policy?.permission || policy?.policy?.permission || [];
          const allConstraints = perms.flatMap((p: any) => p?.constraint || []);
          const flatConstraints = (arr: any[]): any[] => arr.flatMap((c: any) => [
            c,
            ...(Array.isArray(c.and) ? flatConstraints(c.and) : []),
            ...(Array.isArray(c.or) ? flatConstraints(c.or) : []),
          ]);
          const constraints = flatConstraints(allConstraints);
          const hit = constraints.find((c: any) =>
            typeof c?.leftOperand === 'string' && /bpn|businesspartner/i.test(c.leftOperand)
          );
          const ro = hit?.rightOperand;
          if (Array.isArray(ro)) return ro[0];
          return typeof ro === 'string' ? ro : undefined;
        };

        return dtos.map(dto => {
          const unpacked = { ...dto.policy };
          return {
            ...unpacked,                      // entpacke ODRL-Struktur
            policyId: unpacked?.['@id'],      // fürs UI, ersetzt 'id'
            dbId: dto.id,                     // DB-UUID
            bpn: extractBpn(unpacked) || '',   // abgeleitete BPN falls vorhanden
            thirdPartyChanges: dto.thirdPartyChanges,
          };
        });
      })
    );
  }

  /**
   * Lädt eine einzelne Policy
   */
  // getPolicy(dbId: string): Observable<OdrlPolicyDefinition> {
  //   return this.http.get<any>(`${this.backendUrl}/${dbId}`).pipe(
  getPolicy(edcId: string, dbId: string): Observable<OdrlPolicyDefinition> {
    return this.http.get<any>(`${this.baseUrl}/${edcId}/policies/${dbId}`).pipe(
      map(dto => ({
        ...dto.policy,
        policyId: dto.policy?.['@id'],
        dbId: dto.id
      }))
    );
  }

  /**
   * Neue Policy anlegen → schickt nur policyId + Policy-Struktur
   */
// uploadPolicyDefinition(raw: any) {
  uploadPolicyDefinition(edcId: string, raw: any) {

  // Prüfen, ob es sich um ein Update oder eine neue Policy handelt
  const isUpdate = !!raw.dbId;

  // 1) Robuste Normalisierung aus Editor:
  //    - Erlaube sowohl { permission: [...] } als auch { policy: { permission: [...] } }
  const permission = Array.isArray(raw?.permission)
    ? raw.permission
    : Array.isArray(raw?.policy?.permission)
      ? raw.policy.permission
      : [];

  // Generiere automatisch eine Policy-ID, wenn keine vorhanden ist
  const policyId = String(raw?.['@id'] ?? '').trim() || `policy-${Date.now()}`;

  const normalizedPolicy = {
    '@context': raw?.['@context'] ?? { odrl: 'http://www.w3.org/ns/odrl/2/' },
    '@id': policyId,
    permission
  };

  const dto = {
    policyId: normalizedPolicy['@id'],
    policy: normalizedPolicy
  };

  // Wenn dbId vorhanden ist, handelt es sich um ein Update
  if (isUpdate) {
    console.log(`[PolicyService] Updating policy with dbId ${raw.dbId} for EDC ${edcId}`);
    console.log('[PolicyService] Update DTO ->', dto);

    // PUT request für Update
    return this.http.put(`${this.baseUrl}/${edcId}/policies/${raw.dbId}`, dto, { observe: 'response' })
      .pipe(
        map((resp) => this.ensurePolicyResponseBody(resp, dto)),
        tap(
          () => console.log(`Successfully updated policy ${raw.dbId}`),
          (error: any) => console.error(`Error updating policy ${raw.dbId}:`, error)
        )
      );
  } else {
    console.log('[PolicyService] Creating new policy for EDC', edcId);
    console.log('[PolicyService] Create DTO ->', dto);

    // POST request für neue Policy
    return this.http.post(`${this.baseUrl}/${edcId}/policies`, dto, { observe: 'response' })
      .pipe(
        map((resp) => this.ensurePolicyResponseBody(resp, dto)),
        tap(
          () => console.log(`Successfully created new policy`),
          (error: any) => console.error(`Error creating policy:`, error)
        )
      );
  }
}



  /**
   * Bestehende Policy updaten → DB-UUID notwendig
   */
  // updatePolicyDefinition(policy: OdrlPolicyDefinition): Observable<any> {
  updatePolicyDefinition(edcId: string, policy: OdrlPolicyDefinition): Observable<any> {
    if (!policy.dbId) {
      throw new Error('Cannot update policy without dbId');
    }
    const dto = {
      id: policy.dbId,
      policyId: policy.policyId || policy['@id'],
      policy: {
        '@context': policy['@context'],
        '@id': policy.policyId || policy['@id'],
        permission: policy.permission || policy.policy?.permission || [],
      },
    };
    // return this.http.put(`${this.backendUrl}/${policy.dbId}`, dto);
    return this.http.put(`${this.baseUrl}/${edcId}/policies/${policy.dbId}`, dto);
  }

  /**
   * Policy löschen (immer mit DB-UUID)
   */
  // deletePolicy(dbId: string): Observable<void> {
  //   return this.http.delete<void>(`${this.backendUrl}/${dbId}`);
  deletePolicy(edcId: string, dbId: string): Observable<void> {
    console.log(`Sending DELETE request to ${this.baseUrl}/${edcId}/policies/${dbId}`);
    return this.http.delete<void>(`${this.baseUrl}/${edcId}/policies/${dbId}`)
      .pipe(
        tap(
          () => console.log(`Successfully deleted policy ${dbId}`),
          (error: any) => console.error(`Error deleting policy ${dbId}:`, error)
        )
      );
  }

  redeployPolicy(edcId: string, dbId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${edcId}/policies/${dbId}/redeploy`, {});
  }

  // Normalize backend responses for create/update policies: ensure body.id/policyId present
  private ensurePolicyResponseBody(resp: HttpResponse<any>, sentDto: { policyId: string; policy: any }): HttpResponse<any> {
    const body = resp?.body || {};
    let id = body?.id || body?.policyId;
    if (!id) {
      const loc = resp.headers?.get('Location') || resp.headers?.get('location');
      if (loc) {
        try {
          id = loc.split('/').filter(Boolean).pop();
        } catch {
          id = undefined as any;
        }
      }
    }
    // Fallback to sent policyId as a usable identifier
    if (!id) {
      id = sentDto?.policyId;
    }
    const normalizedBody = {
      ...body,
      id: body?.id || id,
      policyId: body?.policyId || sentDto?.policyId,
    };
    // Return a cloned HttpResponse with normalized body
    return new HttpResponse<any>({
      body: normalizedBody,
      headers: resp.headers,
      status: resp.status,
      statusText: resp.statusText,
      url: resp.url || undefined,
    });
  }


  // CONTRACT DEFINITIONS:
  // --------------------------------------------------------

  // getContractDefinitions(): Observable<OdrlContractDefinition[]> {
  //   return this.http.get<OdrlContractDefinition[]>(this.contractDefUrl);
  getContractDefinitions(edcId: string): Observable<OdrlContractDefinition[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${edcId}/contract-definitions`).pipe(
      map((dtos) => (dtos || []).map((dto) => {
  const cdId = dto.contractDefinitionId || dto['@id'] || dto.id || '';
        const assetId = dto.assetId || '';
        const accessPolicyId = dto.accessPolicyIdStr || dto.accessPolicyId || '';
        const assetsSelector = assetId
          ? [{
              operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
              operator: 'eq',
              operandRight: assetId,
            }]
          : [];
        return {
          ...dto,
          ['@id']: cdId,
          assetsSelector,
          accessPolicyId,
          thirdPartyChanges: dto.thirdPartyChanges,
        } as OdrlContractDefinition;
      }))
    );
  }

  // createContractDefinition(cd: OdrlContractDefinition): Observable<any> {
  //   return this.http.post(this.contractDefUrl, cd);
  createContractDefinition(edcId: string, cd: OdrlContractDefinition): Observable<any> {
    return this.http.post(`${this.baseUrl}/${edcId}/contract-definitions`, cd);
  }

  // updateContractDefinition(cd: OdrlContractDefinition): Observable<any> {
  //   return this.http.put(`${this.contractDefUrl}/${cd['@id']}`, cd);
  updateContractDefinition(edcId: string, cd: OdrlContractDefinition): Observable<any> {
    return this.http.put(`${this.baseUrl}/${edcId}/contract-definitions/${cd['@id']}`, cd);
  }

  // deleteContractDefinition(id: string): Observable<void> {
  //   return this.http.delete<void>(`${this.contractDefUrl}/${id}`);
  deleteContractDefinition(edcId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${edcId}/contract-definitions/${id}`);
  }

  redeployContractDefinition(edcId: string, contractDefinitionId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${edcId}/contract-definitions/${contractDefinitionId}/redeploy`, {});
  }
}
