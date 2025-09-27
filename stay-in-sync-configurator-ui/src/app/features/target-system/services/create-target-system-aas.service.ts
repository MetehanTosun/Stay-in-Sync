import { Injectable } from '@angular/core';
import { AasClientService } from '../../source-system/services/aas-client.service';
import { MessageService } from 'primeng/api';
import { TreeNode } from 'primeng/api';

@Injectable({
  providedIn: 'root'
})
export class CreateTargetSystemAasService {

  constructor(
    private aasClient: AasClientService,
    private messageService: MessageService
  ) {}

  /**
   * Test AAS connection
   */
  async testConnection(systemId: number): Promise<{ success: boolean; data?: any; error?: string }> {
    return new Promise((resolve) => {
      this.aasClient.test('target', systemId).subscribe({
        next: (data: any) => {
          if (data && data.idShort) {
            resolve({ 
              success: true, 
              data: { idShort: data.idShort, assetKind: data.assetKind } 
            });
          } else {
            resolve({ success: true, data: {} });
          }
        },
        error: (err: any) => {
          resolve({ 
            success: false, 
            error: 'Connection failed' 
          });
        }
      });
    });
  }

  /**
   * Discover submodels
   */
  async discoverSubmodels(systemId: number): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      this.aasClient.listSubmodels('target', systemId, {}).subscribe({
        next: (resp: any) => {
          const submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const treeNodes = submodels.map((sm: any) => this.mapSubmodelToNode(sm));
          resolve(treeNodes);
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Load root elements for a submodel
   */
  async loadRootElements(systemId: number, submodelId: string): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      const smIdEnc = this.encodeIdToBase64Url(submodelId);
      
      this.aasClient.listElements('target', systemId, smIdEnc, 'shallow').subscribe({
        next: (resp: any) => {
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const roots = list.filter((el: any) => {
            const p = el?.idShortPath || el?.idShort;
            return p && !String(p).includes('/');
          });
          const treeNodes = roots.map((el: any) => this.mapElementToNode(submodelId, el));
          resolve(treeNodes);
        },
        error: (e: any) => {
          const status = e?.status;
          if (status === 400 || status === 404) {
            // Fallback: try deep and filter to roots
            this.aasClient.listElements('target', systemId, smIdEnc, 'all').subscribe({
              next: (resp2: any) => {
                const arr: any[] = Array.isArray(resp2) ? resp2 : (resp2?.result ?? []);
                const roots = arr.filter((el: any) => {
                  const p = el?.idShortPath || el?.idShort;
                  return p && !String(p).includes('/');
                });
                const treeNodes = roots.map((el: any) => this.mapElementToNode(submodelId, el));
                resolve(treeNodes);
              },
              error: (e2: any) => reject(e2)
            });
          } else {
            reject(e);
          }
        }
      });
    });
  }

  /**
   * Load children elements
   */
  async loadChildren(systemId: number, submodelId: string, parentPath: string): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      const smIdEnc = this.encodeIdToBase64Url(submodelId);
      
      this.aasClient.listElements('target', systemId, smIdEnc, 'shallow', parentPath).subscribe({
        next: (resp: any) => {
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const treeNodes = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElementToNode(submodelId, el);
          });
          resolve(treeNodes);
        },
        error: (e: any) => {
          const status = e?.status;
          if (status === 400 || status === 404) {
            // Fallback: deep and filter direct children
            this.aasClient.listElements('target', systemId, smIdEnc, 'all').subscribe({
              next: (resp2: any) => {
                const arr: any[] = Array.isArray(resp2) ? resp2 : (resp2?.result ?? []);
                const prefix = parentPath ? (parentPath.endsWith('/') ? parentPath : parentPath + '/') : '';
                const children = arr.filter((el: any) => {
                  const p = el?.idShortPath || el?.idShort;
                  if (!p) return false;
                  if (!parentPath) return !String(p).includes('/');
                  return String(p).startsWith(prefix);
                });
                const treeNodes = children.map((el: any) => {
                  if (!el.idShortPath && el.idShort) {
                    el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
                  }
                  return this.mapElementToNode(submodelId, el);
                });
                resolve(treeNodes);
              },
              error: (e2: any) => reject(e2)
            });
          } else {
            reject(e);
          }
        }
      });
    });
  }

  /**
   * Load element details
   */
  async loadElementDetails(systemId: number, submodelId: string, elementPath: string): Promise<any> {
    return new Promise((resolve, reject) => {
      const smIdEnc = this.encodeIdToBase64Url(submodelId);
      
      this.aasClient.getElement('target', systemId, smIdEnc, elementPath).subscribe({
        next: (found: any) => {
          const liveType = found?.modelType || (found?.valueType ? 'Property' : undefined);
          const minValue = found.min ?? found.minValue;
          const maxValue = found.max ?? found.maxValue;
          const inputVars = Array.isArray(found.inputVariables) ? found.inputVariables : [];
          const outputVars = Array.isArray(found.outputVariables) ? found.outputVariables : [];
          const inoutVars = Array.isArray(found.inoutputVariables) ? found.inoutputVariables : [];
          const ann1 = found.annotations;
          const ann2 = found.annotation;
          const annotationsRaw = Array.isArray(ann1) ? ann1 : (Array.isArray(ann2) ? ann2 : []);
          
          const mapVar = (v: any): any | null => {
            const val = v?.value ?? v;
            const idShort = val?.idShort;
            if (!idShort) return null;
            return { idShort, modelType: val?.modelType, valueType: val?.valueType };
          };
          
          const mapAnnotation = (a: any): any | null => {
            const val = a?.value ?? a;
            const idShort = val?.idShort;
            if (!idShort) return null;
            return { idShort, modelType: val?.modelType, valueType: val?.valueType, value: val?.value };
          };
          
          const livePanel = {
            label: found.idShort,
            type: liveType || 'Unknown',
            value: found.value,
            valueType: found.valueType,
            min: minValue,
            max: maxValue,
            inputVariables: inputVars.map(mapVar).filter(Boolean),
            outputVariables: outputVars.map(mapVar).filter(Boolean),
            inoutputVariables: inoutVars.map(mapVar).filter(Boolean),
            annotations: annotationsRaw.map(mapAnnotation).filter(Boolean)
          };
          
          resolve(livePanel);
        },
        error: (e) => reject(e)
      });
    });
  }

  /**
   * Create submodel
   */
  async createSubmodel(systemId: number, submodelData: any): Promise<void> {
    return new Promise((resolve, reject) => {
      this.aasClient.createSubmodel('target', systemId, submodelData).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Submodel created', 
            detail: 'New submodel added to shell' 
          });
          resolve();
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Create element
   */
  async createElement(systemId: number, submodelId: string, elementData: any, parentPath?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const smIdB64 = this.encodeIdToBase64Url(submodelId);
      this.aasClient.createElement('target', systemId, smIdB64, elementData, parentPath).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Element created', 
            detail: 'New element added to submodel' 
          });
          resolve();
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Delete submodel
   */
  async deleteSubmodel(systemId: number, submodelId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const smIdB64 = this.encodeIdToBase64Url(submodelId);
      this.aasClient.deleteSubmodel('target', systemId, smIdB64).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Submodel deleted', 
            detail: 'Submodel removed from shell' 
          });
          resolve();
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Delete element
   */
  async deleteElement(systemId: number, submodelId: string, elementPath: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const smIdB64 = this.encodeIdToBase64Url(submodelId);
      this.aasClient.deleteElement('target', systemId, smIdB64, elementPath).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Element deleted', 
            detail: 'Element removed from submodel' 
          });
          resolve();
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Set element value
   */
  async setElementValue(systemId: number, submodelId: string, elementPath: string, value: any): Promise<void> {
    return new Promise((resolve, reject) => {
      const smIdB64 = this.encodeIdToBase64Url(submodelId);
      this.aasClient.patchElementValue('target', systemId, smIdB64, elementPath, value).subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Value updated', 
            detail: 'Property value saved' 
          });
          resolve();
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Preview AASX file
   */
  async previewAasx(systemId: number, file: File): Promise<any> {
    return new Promise((resolve, reject) => {
      this.aasClient.previewAasx('target', systemId, file).subscribe({
        next: (resp: any) => {
          const arr = (resp && (Array.isArray(resp.submodels) ? resp.submodels : (resp.result ?? []))) || [];
          resolve(arr);
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Upload AASX file
   */
  async uploadAasx(systemId: number, file: File, selection?: any): Promise<void> {
    return new Promise((resolve, reject) => {
      const hasSelection = selection?.submodels?.some((s: any) => s.full || (s.elements && s.elements.length > 0));
      const req$ = hasSelection
        ? this.aasClient.attachSelectedAasx('target', systemId, file, selection)
        : this.aasClient.uploadAasx('target', systemId, file);
        
      req$.subscribe({
        next: () => {
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Upload accepted', 
            detail: 'AASX uploaded and attached' 
          });
          resolve();
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Refresh node elements
   */
  async refreshNodeElements(systemId: number, submodelId: string, parentPath: string): Promise<TreeNode[]> {
    return new Promise((resolve, reject) => {
      const smIdB64 = this.encodeIdToBase64Url(submodelId);
      this.aasClient.listElements('target', systemId, smIdB64, 'shallow', parentPath || undefined).subscribe({
        next: (resp: any) => {
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const mapped = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElementToNode(submodelId, el);
          });
          resolve(mapped);
        },
        error: (err) => reject(err)
      });
    });
  }

  /**
   * Parse value for type
   */
  parseValueForType(raw: string, valueType?: string): any {
    if (!valueType) return raw;
    const t = valueType.toLowerCase();
    
    if (t.includes('boolean')) {
      if (raw === 'true' || raw === 'false') return raw === 'true';
      return !!raw;
    }
    
    if (t.includes('int') || t.includes('integer') || t.includes('long')) {
      const n = parseInt(raw, 10);
      return isNaN(n) ? raw : n;
    }
    
    if (t.includes('float') || t.includes('double') || t.includes('decimal')) {
      const n = parseFloat(raw);
      return isNaN(n) ? raw : n;
    }
    
    return raw;
  }

  /**
   * Map submodel to tree node
   */
  private mapSubmodelToNode(sm: any): TreeNode {
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
   * Map element to tree node
   */
  private mapElementToNode(submodelId: string, el: any): TreeNode {
    const computedType = this.inferModelType(el);
    const label = el.idShort;
    const typeHasChildren = el?.modelType === 'SubmodelElementCollection' || 
                           el?.modelType === 'SubmodelElementList' || 
                           el?.modelType === 'Operation' || 
                           el?.modelType === 'Entity';
    const hasChildren = el?.hasChildren === true || typeHasChildren;
    
    return { 
      key: `${submodelId}::${el.idShortPath || el.idShort}`, 
      label, 
      data: { 
        type: 'element', 
        submodelId, 
        idShortPath: el.idShortPath || el.idShort, 
        modelType: computedType, 
        raw: el 
      }, 
      leaf: !hasChildren, 
      children: [] 
    } as TreeNode;
  }

  /**
   * Infer model type from element
   */
  private inferModelType(el: any): string | undefined {
    if (!el) return undefined;
    if (el.modelType) return el.modelType;
    
    if (el.min !== undefined || el.max !== undefined || el.minValue !== undefined || el.maxValue !== undefined) {
      return 'Range';
    }
    
    if (el.valueType) return 'Property';
    
    if (Array.isArray(el.inputVariables) || Array.isArray(el.outputVariables) || Array.isArray(el.inoutputVariables)) {
      return 'Operation';
    }
    
    if (Array.isArray(el.value)) {
      const isML = el.value.every((v: any) => v && (v.language !== undefined) && (v.text !== undefined));
      if (isML) return 'MultiLanguageProperty';
      if (el.typeValueListElement || el.orderRelevant !== undefined) return 'SubmodelElementList';
      return 'SubmodelElementCollection';
    }
    
    if (el.first || el.firstReference) {
      const ann = el.annotations || el.annotation;
      return Array.isArray(ann) ? 'AnnotatedRelationshipElement' : 'RelationshipElement';
    }
    
    if (Array.isArray(el.annotations) || Array.isArray(el.annotation)) {
      return 'AnnotatedRelationshipElement';
    }
    
    if (Array.isArray(el.statements)) return 'Entity';
    if (Array.isArray(el.keys)) return 'ReferenceElement';
    if (el.contentType && (el.fileName || el.path)) return 'File';
    
    return undefined;
  }

  /**
   * Encode ID to Base64 URL
   */
  private encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    try {
      const b64 = (window as any).btoa(unescape(encodeURIComponent(id)));
      return b64.replace(/=+$/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    } catch {
      return id;
    }
  }
}
