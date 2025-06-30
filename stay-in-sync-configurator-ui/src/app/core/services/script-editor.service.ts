import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay, map} from 'rxjs/operators';
import { SyncJobContextData } from '../../features/script-editor/sync-job-context-panel/sync-job-context-panel.component';

import { 
  ApiRequestConfiguration, 
  ArcSaveRequest, 
  ArcTestCallRequest, 
  ArcTestCallResponse 
} from '../../features/script-editor/models/arc.models';

export interface ScriptPayload {
  typescriptCode: string;
  javascriptCode: string;
  hash: string;
}

export interface SavedScript {
  typescriptCode: string;
}

@Injectable({
  providedIn: 'root'
})
export class ScriptEditorService {

  //TODO placeholder api endpoint baseurl
  private readonly API_URL = '/api';

  constructor(private http: HttpClient) {}

  /**
   * Sends a temporary ARC configuration to the backend for a live test call.
   * @param request The configuration to test.
   * @returns An observable of the test call result.
   */
  testArcConfiguration(request: ArcTestCallRequest): Observable<ArcTestCallResponse> {
    return this.http.post<ArcTestCallResponse>(`${this.API_URL}/arc/test-call`, request); // TODO: Bind resource endpoint
  }

  /**
   * Saves a new or updated ApiRequestConfiguration to the backend.
   * @param request The ARC data to save.
   * @returns An observable of the saved ARC, including its new ID if created.
   */
  saveArcConfiguration(request: ArcSaveRequest): Observable<ApiRequestConfiguration> {
    if (request.id) {
      return this.http.put<ApiRequestConfiguration>(`${this.API_URL}/arcs/${request.id}`, request);
    }
    return this.http.post<ApiRequestConfiguration>(`${this.API_URL}/arcs`, request);
  }

  /**
   * Fetches all ARCs associated with a specific Source System.
   * This is used for lazy-loading types into the editor.
   * @param systemId The ID of the source system.
   * @returns An observable array of ARCs.
   */
  getArcsForSourceSystem(systemId: string): Observable<ApiRequestConfiguration[]> {
    // --- MOCK IMPLEMENTATION ---
    // In a real app, this would be an HTTP call:
    // return this.http.get<ApiRequestConfiguration[]>(`${this.API_URL}/source-systems/${systemId}/arcs`);
    
    // For demonstration, returning mock data.
    if (systemId === 'sys-crm-01') {
      return of([
        { id: 'arc-1', alias: 'activeCustomers', sourceSystemId: 'sys-crm-01', endpointId: 'ep-1', endpointPath: '/customers', httpMethod: 'GET', responseDts: 'interface ActiveCustomersType { id: string; name: string; status: "active" | "inactive"; }' },
        { id: 'arc-2', alias: 'customerById', sourceSystemId: 'sys-crm-01', endpointId: 'ep-2', endpointPath: '/customers/{id}', httpMethod: 'GET', responseDts: 'interface CustomerByIdType { id: string; name: string; email: string; address: { street: string; city: string; }; }' }
      ]);
    }
    return of([]);
    // --- END MOCK ---
  }

  getSyncJobContext(jobId: string): Observable<SyncJobContextData> {
    // TODO: Replace mock with your actual API endpoint.
    // return this.http.get<SyncJobContextData>(`${this.API_URL}/sync-jobs/${jobId}/context`);

    return this.fetchMockSyncJobContextData(jobId);
  }

  getTypeDefinitions(jobId: string): Observable<string> {
    // TODO: Replace mock with your actual API endpoint.
    // return this.http.get(`${this.API_URL}/sync-jobs/${jobId}/type-definitions`, { responseType: 'text' });

    return this.fetchMockDtsForJob(jobId);
  }

  getSavedScript(jobId: string): Observable<SavedScript | null>{
    // TODO: Replace mock with your actual API endpoint.
    // This endpoint should return a 404 or an empty object if no script exists.
    // return this.http.get<SavedScript>(`${this.API_URL}/sync-jobs/${jobId}/script`);

    if (jobId === 'anotherJob789'){
      return of({ typescriptCode: `// This is a previously saved script for ${jobId}\nstayinsync.log('Hello from a saved script!');`}).pipe(delay(200));
    }
    return of(null).pipe(delay(200));
  }

  saveScript(jobId: string, payload: ScriptPayload): Observable<void> {
    // TODO: Implement your actual API call.
    // return this.http.post<void>(`${this.API_URL}/sync-jobs/${jobId}/script`, payload);

    console.log(`SAVING SCRIPT for ${jobId}`, payload);
    return of(undefined).pipe(delay(500));
  }

  private fetchMockDtsForJob(jobId: string): Observable<string> {
    let dtsContent = `// No types found for ${jobId}`;
    if(jobId == 'activeJob123'){
      dtsContent = `
          // Types for Job 'Customer & Product Sync'
          declare namespace CrmSystemTypes {
              interface Customer { id: string; name: string; email?: string; lastContactDate: string; isActive: boolean; }
          }
          declare namespace ErpSystemTypes {
              interface Product { sku: string; description: string; stockLevel: number; }
          }
          // 'sourceData' is the global variable your script will use.
          declare const sourceData: {
              crmCustomer: CrmSystemTypes.Customer;
              erpProducts: ErpSystemTypes.Product[];
          };
      `;
    } else if(jobId === 'anotherJob789'){
      dtsContent = `
          declare namespace LegacySystem {
              interface DataRecord { key: string; value: any; timestamp: number; }
          }
          declare const sourceData: {
              legacyRecords: LegacySystem.DataRecord[];
          };
      `;
    }
    return of(dtsContent).pipe(delay(300));
  }

  private fetchMockSyncJobContextData(jobId: string): Observable<SyncJobContextData> {
    if(jobId === 'activeJob123'){
      const data: SyncJobContextData = {
        syncJobId: jobId,
        syncJobName: 'Customer & Product Sync (Active)',
        syncJobDescription:
          'Synchronizes active customer data from CRM and product stock from ERP.',
        sourceSystems: [
          {
            id: 'crm',
            name: 'Main CRM Platform',
            type: 'REST_OPENAPI',
            dataEntities: [
              {
                aliasInScript: 'crmCustomer',
                entityName: 'Active Customer Profile',
                schemaSummary: {
                  type: 'object',
                  title: 'Customer',
                  properties: {
                    id: { type: 'string' },
                    name: { type: 'string' },
                    email: { type: 'string' },
                    lastContactDate: { type: 'string', format: 'date-time' },
                    isActive: { type: 'boolean' },
                  },
                  required: ['id', 'name', 'isActive'],
                },
              },
            ],
          },
          {
            id: 'erp',
            name: 'Central ERP System',
            type: 'AAS',
            dataEntities: [
              {
                aliasInScript: 'erpProducts',
                entityName: 'Stocked Products List',
                schemaSummary: {
                  type: 'array',
                  title: 'Products',
                  items: {
                    type: 'object',
                    properties: {
                      sku: { type: 'string' },
                      description: { type: 'string' },
                      stockLevel: { type: 'number' },
                    },
                    required: ['sku', 'stockLevel'],
                  },
                },
              },
            ],
          },
        ],
        destinationSystem: {
          id: 'dataMart',
          name: 'Sales Data Mart',
          targetEntity: 'AggregatedCustomerProductView',
        },
      };
      return of(data).pipe(delay(400));
    }
    return throwError(()=> new Error('SyncJob not found.'));
  }
}
