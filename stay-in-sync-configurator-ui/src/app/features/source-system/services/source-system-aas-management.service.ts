import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TreeNode } from 'primeng/api';
import { AasService } from '../services/aas.service';
import { AasClientService } from '../services/aas-client.service';
import { HttpErrorService } from '../../../core/services/http-error.service';

export interface AasOperationVarView { 
  idShort: string; 
  modelType?: string; 
  valueType?: string 
}

export interface AasAnnotationView { 
  idShort: string; 
  modelType?: string; 
  valueType?: string; 
  value?: any 
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
export class SourceSystemAasManagementService {

  constructor(
    private aasService: AasService,
    private aasClient: AasClientService,
    private errorService: HttpErrorService
  ) {}

  /**
   * Test AAS connection
   */
  testConnection(systemId: number): Observable<any> {
    return this.aasService.aasTest(systemId);
  }

  /**
   * Discover AAS snapshot and return tree nodes
   */
  discoverSnapshot(systemId: number): Observable<TreeNode[]> {
    return new Observable(observer => {
      this.aasService.listSubmodels(systemId, 'SNAPSHOT').subscribe({
        next: (resp) => {
          const submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const treeNodes = submodels.map((sm: any) => this.mapSubmodelToNode(sm));
          observer.next(treeNodes);
          observer.complete();
        },
        error: (err) => {
          this.errorService.handleError(err);
          observer.error(err);
        }
      });
    });
  }

  /**
   * Load AAS children for a node
   */
  loadChildren(systemId: number, submodelId: string, parentPath: string | undefined, attach: TreeNode): Observable<void> {
    return new Observable(observer => {
      this.aasService.listElements(systemId, submodelId, { depth: 'shallow', parentPath, source: 'SNAPSHOT' }).subscribe({
        next: (resp) => {
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          attach.children = list.map((el: any) => this.mapElementToNode(submodelId, el));
          observer.next();
          observer.complete();
        },
        error: (err) => {
          this.errorService.handleError(err);
          observer.error(err);
        }
      });
    });
  }

  /**
   * Load live element details
   */
  loadElementDetails(systemId: number, smId: string, idShortPath: string | undefined, node?: TreeNode): Observable<AasElementLivePanel> {
    return new Observable(observer => {
      const keyStr = (node && typeof node.key === 'string') ? (node.key as string) : '';
      const keyPath = keyStr.includes('::') ? keyStr.split('::')[1] : '';
      const safePath = idShortPath || keyPath || (node?.data?.raw?.idShort || '');
      const last = safePath.split('/').pop() as string;
      const parent = safePath.includes('/') ? safePath.substring(0, safePath.lastIndexOf('/')) : '';

      this.aasService.getElement(systemId, smId, safePath, 'LIVE').subscribe({
        next: (found: any) => {
          const livePanel = this.mapElementToLivePanel(found);
          observer.next(livePanel);
          observer.complete();
        },
        error: (_err: any) => {
          // Fallback: list under parent shallow and pick child
          this.aasService.listElements(systemId, smId, { depth: 'shallow', parentPath: parent || undefined, source: 'LIVE' })
            .subscribe({
              next: (resp: any) => {
                const list: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
                const found2 = list.find((el: any) => el.idShort === last);
                if (found2) {
                  const livePanel = this.mapElementToLivePanel(found2);
                  observer.next(livePanel);
                } else {
                  observer.next({ label: last, type: 'Unknown' } as AasElementLivePanel);
                }
                observer.complete();
              },
              error: (err2: any) => {
                this.errorService.handleError(err2);
                observer.error(err2);
              }
            });
        }
      });
    });
  }

  /**
   * Create submodel
   */
  createSubmodel(systemId: number, submodelData: any): Observable<any> {
    return this.aasService.createSubmodel(systemId, submodelData);
  }

  /**
   * Create element
   */
  createElement(systemId: number, submodelId: string, elementData: any, parentPath?: string): Observable<any> {
    // Use aasClientService like target system does
    return this.aasClient.createElement('source', systemId, submodelId, elementData, parentPath);
  }

  /**
   * Delete submodel
   */
  deleteSubmodel(systemId: number, submodelId: string): Observable<any> {
    const smIdB64 = this.aasService.encodeIdToBase64Url(submodelId);
    return this.aasService.deleteSubmodel(systemId, smIdB64);
  }

  /**
   * Delete element
   */
  deleteElement(systemId: number, submodelId: string, idShortPath: string): Observable<any> {
    const smIdB64 = this.aasService.encodeIdToBase64Url(submodelId);
    return this.aasService.deleteElement(systemId, smIdB64, idShortPath);
  }

  /**
   * Set property value
   */
  setPropertyValue(systemId: number, submodelId: string, elementPath: string, value: any): Observable<any> {
    const smIdB64 = this.aasService.encodeIdToBase64Url(submodelId);
    return this.aasService.setPropertyValue(systemId, smIdB64, elementPath, value);
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
    const typeHasChildren = el?.modelType === 'SubmodelElementCollection' || el?.modelType === 'SubmodelElementList' || el?.modelType === 'Operation' || el?.modelType === 'Entity';
    const hasChildren = el?.hasChildren === true || typeHasChildren;
    return {
      key: `${submodelId}::${el.idShortPath || el.idShort}`,
      label,
      data: { type: 'element', submodelId, idShortPath: el.idShortPath || el.idShort, modelType: computedType, raw: el },
      leaf: !hasChildren,
      children: []
    } as TreeNode;
  }

  /**
   * Map element to live panel
   */
  private mapElementToLivePanel(found: any): AasElementLivePanel {
    // Use the same logic as inferModelType for consistent type detection
    let liveType = found?.modelType || found?.type || (found?.valueType ? 'Property' : undefined);
    
    // Fallback: Use inferModelType method for consistent type detection
    if (!liveType) {
      liveType = this.inferModelType(found);
    }
    const minValue = (found as any).min ?? (found as any).minValue;
    const maxValue = (found as any).max ?? (found as any).maxValue;
    const inputVars = Array.isArray((found as any).inputVariables) ? (found as any).inputVariables : [];
    const outputVars = Array.isArray((found as any).outputVariables) ? (found as any).outputVariables : [];
    const inoutVars = Array.isArray((found as any).inoutputVariables) ? (found as any).inoutputVariables : [];
    const ann1 = (found as any).annotations;
    const ann2 = (found as any).annotation;
    const annotationsRaw = Array.isArray(ann1) ? ann1 : (Array.isArray(ann2) ? ann2 : []);

    const mapVar = (v: any): AasOperationVarView | null => {
      const val = v?.value ?? v;
      const idShort = val?.idShort;
      if (!idShort) return null;
      return { idShort, modelType: val?.modelType, valueType: val?.valueType };
    };

    const mapAnnotation = (a: any): AasAnnotationView | null => {
      const val = a?.value ?? a;
      const idShort = val?.idShort;
      if (!idShort) return null;
      return { idShort, modelType: val?.modelType, valueType: val?.valueType, value: val?.value };
    };

    const stringifyRef = (ref: any): string | undefined => {
      if (!ref) return undefined;
      const keys = ref?.keys;
      if (Array.isArray(keys) && keys.length) {
        try {
          return keys.map((k: any) => `${k?.type ?? ''}:${k?.value ?? ''}`).join(' / ');
        } catch {
          return JSON.stringify(ref);
        }
      }
      if (typeof ref === 'string') return ref;
      if (ref?.value) return String(ref.value);
      try { return JSON.stringify(ref); } catch { return String(ref); }
    };

    const firstRef = stringifyRef((found as any).first || (found as any).firstReference);
    const secondRef = stringifyRef((found as any).second || (found as any).secondReference);

    return {
      label: found.idShort,
      type: liveType || 'Unknown',
      value: (found as any).value,
      valueType: (found as any).valueType,
      min: minValue,
      max: maxValue,
      inputVariables: inputVars.map(mapVar).filter(Boolean) as AasOperationVarView[],
      outputVariables: outputVars.map(mapVar).filter(Boolean) as AasOperationVarView[],
      inoutputVariables: inoutVars.map(mapVar).filter(Boolean) as AasOperationVarView[],
      firstRef,
      secondRef,
      annotations: annotationsRaw.map(mapAnnotation).filter(Boolean) as AasAnnotationView[]
    };
  }

  /**
   * Infer model type from element
   */
  private inferModelType(el: any): string | undefined {
    if (!el) return undefined;
    if (el.modelType) return el.modelType;
    // Detect Range before Property
    if (el.min !== undefined || el.max !== undefined || el.minValue !== undefined || el.maxValue !== undefined) return 'Range';
    if (el.valueType) return 'Property';
    if (Array.isArray(el.inputVariables) || Array.isArray(el.outputVariables) || Array.isArray(el.inoutputVariables)) return 'Operation';
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
    if (Array.isArray(el.annotations) || Array.isArray(el.annotation)) return 'AnnotatedRelationshipElement';
    if (Array.isArray(el.statements)) return 'Entity';
    if (Array.isArray(el.keys)) return 'ReferenceElement';
    if (el.contentType && (el.fileName || el.path)) return 'File';
    return undefined;
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
   * Find node by key
   */
  findNodeByKey(key: string, nodes: TreeNode[] | undefined): TreeNode | null {
    if (!nodes) return null;
    for (const n of nodes) {
      if ((n.key as string) === key) return n;
      const found = this.findNodeByKey(key, n.children as TreeNode[]);
      if (found) return found;
    }
    return null;
  }

  /**
   * Check if element is leaf
   */
  isLeafElement(element: any): boolean {
    const typeHasChildren = element?.modelType === 'SubmodelElementCollection' || 
                           element?.modelType === 'SubmodelElementList' || 
                           element?.modelType === 'Operation' || 
                           element?.modelType === 'Entity';
    return !(element?.hasChildren === true || typeHasChildren);
  }
}
