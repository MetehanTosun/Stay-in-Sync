import { Injectable } from '@angular/core';
import { AasClientService } from '../../source-system/services/aas-client.service';
import { MessageService } from 'primeng/api';
import { TreeNode } from 'primeng/api';

export interface AasOperationVarView { 
  idShort: string; 
  modelType?: string; 
  valueType?: string; 
}

export interface AasAnnotationView { 
  idShort: string; 
  modelType?: string; 
  valueType?: string; 
  value?: any; 
}

export interface AasElementLivePanel {
  label: string;
  type: string;
  value?: any;
  valueType?: string;
  min?: any;
  max?: any;
  inputVariables?: AasOperationVarView[];
  outputVariables?: AasOperationVarView[];
  inoutputVariables?: AasOperationVarView[];
  firstRef?: string;
  secondRef?: string;
  annotations?: AasAnnotationView[];
}

@Injectable({
  providedIn: 'root'
})
export class AasManagementService {

  constructor(
    private aasClientService: AasClientService,
    private messageService: MessageService
  ) {}

  /**
   * Discover AAS submodels
   */
  async discoverSubmodels(systemId: number): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      console.log('[AasManagement] discoverSubmodels: Starting with systemId:', systemId);
      // Try SNAPSHOT first, then fallback to no source parameter (LIVE)
      this.aasClientService.listSubmodels('target', systemId, { source: 'SNAPSHOT' }).subscribe({
        next: (response: any) => {
          console.log('[AasManagement] discoverSubmodels: SNAPSHOT response:', response);
          const submodels = Array.isArray(response) ? response : (response?.result ?? []);
          console.log('[AasManagement] discoverSubmodels: Extracted submodels:', submodels.length, submodels);
          const treeNodes = submodels.map((sm: any) => this.mapSmToNode(sm));
          console.log('[AasManagement] discoverSubmodels: Mapped treeNodes:', treeNodes.length, treeNodes);
          resolve(treeNodes);
        },
        error: (error: any) => {
          console.log('[AasManagement] discoverSubmodels: SNAPSHOT failed, trying LIVE:', error);
          // Fallback to LIVE (no source parameter)
          this.aasClientService.listSubmodels('target', systemId).subscribe({
            next: (response: any) => {
              console.log('[AasManagement] discoverSubmodels: LIVE response:', response);
              const submodels = Array.isArray(response) ? response : (response?.result ?? []);
              console.log('[AasManagement] discoverSubmodels: Extracted submodels:', submodels.length, submodels);
              const treeNodes = submodels.map((sm: any) => this.mapSmToNode(sm));
              console.log('[AasManagement] discoverSubmodels: Mapped treeNodes:', treeNodes.length, treeNodes);
              resolve(treeNodes);
            },
            error: (liveError: any) => {
              console.error('[AasManagement] discoverSubmodels: Both SNAPSHOT and LIVE failed:', liveError);
              reject(liveError);
            }
          });
        }
      });
    });
  }

  /**
   * Load submodel elements
   */
  async loadSubmodelElements(systemId: number, submodelId: string): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      // Use normal Base64 with padding for BaSyx compatibility
      const smIdB64 = btoa(submodelId);
      this.aasClientService.listElements('target', systemId, smIdB64).subscribe({
        next: (response: any) => {
          let elements: any[] = [];
          if (Array.isArray(response)) {
            elements = response;
          } else if (response && response.result && Array.isArray(response.result)) {
            elements = response.result;
          }
          
          const treeNodes = elements.map(el => {
            // Ensure idShortPath is set correctly - build full path like Source System
            let idShortPath = el.idShortPath || el.idShort;
            if (!el.idShortPath && el.idShort) {
              // For root elements, just use idShort
              idShortPath = el.idShort;
            }
            console.log('[AasManagement] loadSubmodelElements: Mapping element', {
              idShort: el.idShort,
              idShortPath: idShortPath,
              modelType: el.modelType
            });
            
            return {
              label: el.idShort || 'Element',
              data: {
                type: 'element',
                submodelId: submodelId,
                idShortPath: idShortPath,
                raw: el,
                modelType: el.modelType
              },
              leaf: this.isLeafElement(el),
              children: [],
              expandedIcon: 'pi pi-folder-open',
              collapsedIcon: 'pi pi-folder'
            };
          });
          resolve(treeNodes);
        },
        error: (error: any) => reject(error)
      });
    });
  }

  /**
   * Load element children
   */
  async loadElementChildren(systemId: number, submodelId: string, parentPath: string): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      // Use normal Base64 with padding for BaSyx compatibility
      const smId = btoa(submodelId);
      
      this.aasClientService.listElements('target', systemId, smId, 'shallow', parentPath).subscribe({
        next: (response: any) => {
          let children: any[] = [];
          if (Array.isArray(response)) {
            children = response;
          } else if (response && response.result && Array.isArray(response.result)) {
            children = response.result;
          }
          
          const treeNodes = children.map(el => {
            // Ensure idShortPath is set correctly - build full path like Source System
            let idShortPath = el.idShortPath || el.idShort;
            if (!el.idShortPath && el.idShort) {
              // Build full path: parentPath + '/' + idShort
              idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            console.log('[AasManagement] loadElementChildren: Mapping child element', {
              idShort: el.idShort,
              idShortPath: idShortPath,
              modelType: el.modelType,
              parentPath: parentPath
            });
            
            return {
              label: el.idShort || 'Element',
              data: {
                type: 'element',
                submodelId: submodelId,
                idShortPath: idShortPath,
                raw: el,
                modelType: el.modelType
              },
              leaf: this.isLeafElement(el),
              children: [],
              expandedIcon: 'pi pi-folder-open',
              collapsedIcon: 'pi pi-folder'
            };
          });
          resolve(treeNodes);
        },
        error: (error: any) => reject(error)
      });
    });
  }

  /**
   * Load element details
   */
  async loadElementDetails(systemId: number, submodelId: string, elementPath: string): Promise<AasElementLivePanel> {
    return new Promise((resolve, reject) => {
      // Use normal Base64 with padding for BaSyx compatibility
      const smId = btoa(submodelId);
      
      // URL encode the element path to handle spaces and special characters
      const encodedElementPath = encodeURIComponent(elementPath);
      console.log('[AasManagement] loadElementDetails: Encoding element path', {
        original: elementPath,
        encoded: encodedElementPath
      });
      
      this.aasClientService.getElement('target', systemId, smId, encodedElementPath).subscribe({
        next: (element: any) => {
          const livePanel: AasElementLivePanel = {
            label: element.idShort || 'Element',
            type: element.modelType || 'Unknown',
            value: element.value,
            valueType: element.valueType,
            min: element.min,
            max: element.max,
            inputVariables: element.inputVariables?.map((v: any) => ({
              idShort: v.idShort,
              modelType: v.modelType,
              valueType: v.valueType
            })),
            outputVariables: element.outputVariables?.map((v: any) => ({
              idShort: v.idShort,
              modelType: v.modelType,
              valueType: v.valueType
            })),
            inoutputVariables: element.inoutputVariables?.map((v: any) => ({
              idShort: v.idShort,
              modelType: v.modelType,
              valueType: v.valueType
            })),
            firstRef: element.first?.keys?.map((k: any) => `${k.type}:${k.value}`).join(',') || element.first,
            secondRef: element.second?.keys?.map((k: any) => `${k.type}:${k.value}`).join(',') || element.second,
            annotations: element.annotations?.map((a: any) => ({
              idShort: a.idShort,
              modelType: a.modelType,
              valueType: a.valueType,
              value: a.value
            }))
          };
          resolve(livePanel);
        },
        error: (error: any) => reject(error)
      });
    });
  }

  /**
   * Create submodel
   */
  async createSubmodel(systemId: number, submodelData: any): Promise<void> {
    return new Promise((resolve, reject) => {
      console.log('[AasManagement] createSubmodel: Starting with systemId:', systemId, 'submodelData:', submodelData);
      this.aasClientService.createSubmodel('target', systemId, submodelData).subscribe({
        next: (response: any) => {
          console.log('[AasManagement] createSubmodel: Success response:', response);
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Submodel created', 
            detail: 'New submodel added to shell' 
          });
          resolve();
        },
        error: (error: any) => {
          console.error('[AasManagement] createSubmodel: Error:', error);
          this.messageService.add({ 
            severity: 'error', 
            summary: 'Create failed', 
            detail: error?.error || error?.message || 'See console for details' 
          });
          reject(error);
        }
      });
    });
  }

  /**
   * Create element
   */
  async createElement(systemId: number, submodelId: string, elementData: any, parentPath?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      // Use normal Base64 with padding for BaSyx compatibility
      const smIdB64 = btoa(submodelId);
      this.aasClientService.createElement('target', systemId, smIdB64, elementData, parentPath).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Element created', 
            detail: 'New element added to submodel' 
          });
          resolve();
        },
        error: (error: any) => {
          this.messageService.add({ 
            severity: 'error', 
            summary: 'Create failed', 
            detail: error?.error || error?.message || 'See console for details' 
          });
          reject(error);
        }
      });
    });
  }

  /**
   * Delete submodel
   */
  async deleteSubmodel(systemId: number, submodelId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      // Use normal Base64 with padding for BaSyx compatibility
      const smIdB64 = btoa(submodelId);
      this.aasClientService.deleteSubmodel('target', systemId, smIdB64).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Submodel deleted', 
            detail: 'Submodel removed from shell' 
          });
          resolve();
        },
        error: (error: any) => {
          this.messageService.add({ 
            severity: 'error', 
            summary: 'Delete failed', 
            detail: error?.error || error?.message || 'See console for details' 
          });
          reject(error);
        }
      });
    });
  }

  /**
   * Delete element
   */
  async deleteElement(systemId: number, submodelId: string, elementPath: string): Promise<void> {
    return new Promise((resolve, reject) => {
      // Use normal Base64 with padding for BaSyx compatibility
      const smIdB64 = btoa(submodelId);
      this.aasClientService.deleteElement('target', systemId, smIdB64, elementPath).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Element deleted', 
            detail: 'Element removed from submodel' 
          });
          resolve();
        },
        error: (error: any) => {
          this.messageService.add({ 
            severity: 'error', 
            summary: 'Delete failed', 
            detail: error?.error || error?.message || 'See console for details' 
          });
          reject(error);
        }
      });
    });
  }

  /**
   * Set element value
   */
  async setElementValue(systemId: number, submodelId: string, elementPath: string, value: any): Promise<void> {
    return new Promise((resolve, reject) => {
      // Use normal Base64 with padding for BaSyx compatibility
      const smIdB64 = btoa(submodelId);
      this.aasClientService.patchElementValue('target', systemId, smIdB64, elementPath, value).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Value updated', 
            detail: 'Property value saved' 
          });
          resolve();
        },
        error: (error: any) => {
          this.messageService.add({ 
            severity: 'error', 
            summary: 'Update failed', 
            detail: error?.error || error?.message || 'See console for details' 
          });
          reject(error);
        }
      });
    });
  }

  /**
   * Check if element is a leaf
   */
  private isLeafElement(element: any): boolean {
    const type = element.modelType;
    return type !== 'SubmodelElementCollection' && type !== 'SubmodelElementList' && type !== 'Entity';
  }

  /**
   * Map submodel to tree node
   */
  private mapSmToNode(sm: any): TreeNode {
    const id = sm.submodelId || sm.id || (sm.keys && sm.keys[0]?.value);
    const label = (sm.submodelIdShort || sm.idShort) || id;
    const kindRaw = (sm.kind || sm.submodelKind || '').toString();
    const isTemplate = kindRaw && kindRaw.toLowerCase().includes('template');
    const modelType = isTemplate ? 'Submodel Template' : 'Submodel';
    return { 
      key: id, 
      label, 
      data: { type: 'submodel', id, modelType, raw: sm }, 
      leaf: false, 
      children: [] 
    } as TreeNode;
  }
}
