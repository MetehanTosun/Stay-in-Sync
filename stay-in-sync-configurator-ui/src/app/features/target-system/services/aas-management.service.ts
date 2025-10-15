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

/**
 * Service responsible for managing AAS (Asset Administration Shell) operations for target systems.
 * Provides methods for discovering submodels, loading elements, and handling AASX file interactions.
 */
@Injectable({
  providedIn: 'root'
})
export class AasManagementService {

  constructor(
    private aasClientService: AasClientService,
    private messageService: MessageService
  ) {}

  /**
   * Discovers all submodels of an AAS for a given target system.
   * First attempts to load from SNAPSHOT source, then falls back to LIVE mode if SNAPSHOT fails.
   * @param systemId ID of the target system.
   * @returns Promise resolving to an array of TreeNode objects representing submodels.
   */
  async discoverSubmodels(systemId: number): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      this.aasClientService.listSubmodels('target', systemId, { source: 'SNAPSHOT' }).subscribe({
        next: (response: any) => {
          const submodels = Array.isArray(response) ? response : (response?.result ?? []);
          const treeNodes = submodels.map((sm: any) => this.mapSmToNode(sm));
          resolve(treeNodes);
        },
        error: (error: any) => {
          this.aasClientService.listSubmodels('target', systemId).subscribe({
            next: (response: any) => {
              const submodels = Array.isArray(response) ? response : (response?.result ?? []);
              const treeNodes = submodels.map((sm: any) => this.mapSmToNode(sm));
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
   * Loads all elements of a specific submodel from the AAS.
   * Builds tree nodes for display in the UI.
   * @param systemId ID of the target system.
   * @param submodelId ID of the submodel.
   * @returns Promise resolving to TreeNode objects representing the submodel elements.
   */
  async loadSubmodelElements(systemId: number, submodelId: string): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
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
            let idShortPath = el.idShortPath || el.idShort;
            if (!el.idShortPath && el.idShort) {
              idShortPath = el.idShort;
            }
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
   * Loads all child elements of a given parent element within a submodel.
   * @param systemId ID of the target system.
   * @param submodelId ID of the submodel.
   * @param parentPath Path of the parent element within the submodel hierarchy.
   * @returns Promise resolving to TreeNode objects representing child elements.
   */
  async loadElementChildren(systemId: number, submodelId: string, parentPath: string): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
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
            let idShortPath = el.idShortPath || el.idShort;
            if (!el.idShortPath && el.idShort) {
              idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
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
   * Loads detailed live information of a specific AAS element.
   * Includes variable definitions, annotations, and reference mappings.
   * @param systemId ID of the target system.
   * @param submodelId ID of the submodel.
   * @param elementPath Path of the element to load.
   * @returns Promise resolving to an AasElementLivePanel object containing element details.
   */
  async loadElementDetails(systemId: number, submodelId: string, elementPath: string): Promise<AasElementLivePanel> {
    return new Promise((resolve, reject) => {
      const smId = btoa(submodelId);
      const encodedElementPath = encodeURIComponent(elementPath);
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
   * Creates a new submodel in the target AAS system.
   * Displays a success or error message upon completion.
   * @param systemId ID of the target system.
   * @param submodelData The submodel data payload to create.
   */
  async createSubmodel(systemId: number, submodelData: any): Promise<void> {
    return new Promise((resolve, reject) => {
      this.aasClientService.createSubmodel('target', systemId, submodelData).subscribe({
        next: (response: any) => {
          this.messageService.add({ 
            key: 'targetAAS',
            severity: 'success', 
            summary: 'Submodel created', 
            detail: 'New submodel added to shell' 
          });
          resolve();
        },
        error: (error: any) => {
          console.error('[AasManagement] createSubmodel: Error:', error);
          this.messageService.add({ 
            key: 'targetAAS',
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
   * Creates a new element in a given submodel.
   * Displays success or error messages upon completion.
   * @param systemId ID of the target system.
   * @param submodelId ID of the submodel.
   * @param elementData The data for the element to be created.
   * @param parentPath Optional parent path for nested elements.
   */
  async createElement(systemId: number, submodelId: string, elementData: any, parentPath?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const smIdB64 = btoa(submodelId);
      this.aasClientService.createElement('target', systemId, smIdB64, elementData, parentPath).subscribe({
        next: () => {
          this.messageService.add({ 
            key: 'targetAAS',
            severity: 'success', 
            summary: 'Element created', 
            detail: 'New element added to submodel' 
          });
          resolve();
        },
        error: (error: any) => {
          this.messageService.add({ 
            key: 'targetAAS',
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
   * Deletes a submodel from the target system's AAS.
   * Displays success or error messages upon completion.
   * @param systemId ID of the target system.
   * @param submodelId ID of the submodel to delete.
   */
  async deleteSubmodel(systemId: number, submodelId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const smIdB64 = btoa(submodelId);
      this.aasClientService.deleteSubmodel('target', systemId, smIdB64).subscribe({
        next: () => {
          this.messageService.add({ 
            key: 'targetAAS',
            severity: 'success', 
            summary: 'Submodel deleted', 
            detail: 'Submodel removed from shell' 
          });
          resolve();
        },
        error: (error: any) => {
          this.messageService.add({ 
            key: 'targetAAS',
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
   * Deletes a specific element within a submodel.
   * @param systemId ID of the target system.
   * @param submodelId ID of the submodel.
   * @param elementPath Path of the element to delete.
   */
  async deleteElement(systemId: number, submodelId: string, elementPath: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const smIdB64 = btoa(submodelId);
      this.aasClientService.deleteElement('target', systemId, smIdB64, elementPath).subscribe({
        next: () => {
          this.messageService.add({ 
            key: 'targetAAS',
            severity: 'success', 
            summary: 'Element deleted', 
            detail: 'Element removed from submodel' 
          });
          resolve();
        },
        error: (error: any) => {
          this.messageService.add({ 
            key: 'targetAAS',
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
   * Updates the value of a specific property element in a submodel.
   * Displays a success or error message upon completion.
   * @param systemId ID of the target system.
   * @param submodelId ID of the submodel.
   * @param elementPath Path of the element whose value should be updated.
   * @param value The new value to set.
   */
  async setElementValue(systemId: number, submodelId: string, elementPath: string, value: any): Promise<void> {
    return new Promise((resolve, reject) => {
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
   * Determines if an element is a leaf node (i.e., has no children).
   * @param element The element to check.
   * @returns True if the element is not a collection, list, or entity; otherwise false.
   */
  private isLeafElement(element: any): boolean {
    const type = element.modelType;
    return type !== 'SubmodelElementCollection' && type !== 'SubmodelElementList' && type !== 'Entity';
  }

  /**
   * Maps a submodel object into a TreeNode representation for UI display.
   * @param sm The submodel object.
   * @returns TreeNode representing the submodel in the UI.
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

  /**
   * Generates a preview of an uploaded AASX file before attaching or importing.
   * @param systemId ID of the target system.
   * @param file The AASX file to preview.
   * @returns Promise resolving to the preview response data.
   */
  async previewAasx(systemId: number, file: File): Promise<any> {
    return new Promise((resolve, reject) => {
      this.aasClientService.previewAasx('target', systemId, file).subscribe({
        next: (response) => {
          resolve(response);
        },
        error: (error: any) => {
          console.error('[AasManagement] Error previewing AASX:', error);
          reject(error);
        }
      });
    });
  }

  /**
   * Attaches selected content from a previewed AASX file to the target AAS system.
   * Displays success or error messages based on backend response.
   * @param systemId ID of the target system.
   * @param file The AASX file being attached.
   * @param selection The selected content to attach from the file.
   */
  async attachSelectedAasx(systemId: number, file: File, selection: any): Promise<void> {
    return new Promise((resolve, reject) => {
      this.aasClientService.attachSelectedAasx('target', systemId, file, selection).subscribe({
        next: () => {
          this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Upload accepted', detail: 'Selected AASX content attached successfully.' });
          resolve();
        },
        error: (error: any) => {
          console.error('[AasManagement] Error attaching selected AASX content:', error);
          reject(error);
        }
      });
    });
  }

  /**
   * Uploads a full AASX file to the backend and integrates it with the target AAS.
   * Displays success or error messages based on upload result.
   * @param systemId ID of the target system.
   * @param file The AASX file to upload.
   */
  async uploadAasx(systemId: number, file: File): Promise<void> {
    return new Promise((resolve, reject) => {
      this.aasClientService.uploadAasx('target', systemId, file).subscribe({
        next: () => {
          this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Upload accepted', detail: 'AASX uploaded successfully.' });
          resolve();
        },
        error: (error: any) => {
          console.error('[AasManagement] Error uploading AASX:', error);
          reject(error);
        }
      });
    });
  }
}
