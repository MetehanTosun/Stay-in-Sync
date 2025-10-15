import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TreeNode, MessageService } from 'primeng/api';
import { AasService } from './aas.service';
import { HttpErrorService } from '../../../core/services/http-error.service';

interface OperationVarView { 
  idShort: string; 
  modelType?: string; 
  valueType?: string 
}

interface AnnotationView { 
  idShort: string; 
  modelType?: string; 
  valueType?: string; 
  value?: any 
}

interface ElementLivePanel {
  label: string;
  type: string;
  value?: any;
  valueType?: string;
  min?: any;
  max?: any;
  inputVariables?: OperationVarView[];
  outputVariables?: OperationVarView[];
  inoutputVariables?: OperationVarView[];
  firstRef?: string;
  secondRef?: string;
  annotations?: AnnotationView[];
}

@Injectable({ providedIn: 'root' })
export class CreateSourceSystemAasService {

  private typeCache: Record<string, Record<string, string>> = {};

  constructor(
    private aasService: AasService,
    private messageService: MessageService,
    private errorService: HttpErrorService
  ) {}

  /** Test the AAS connection for a source system and toast the outcome. */
  async testConnection(sourceSystemId: number): Promise<any> {
    try {
      const result = await this.aasService.aasTest(sourceSystemId).toPromise();
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'AAS connection test successful!' });
      return result;
    } catch (error) {
      this.errorService.handleError(error as any);
      throw error;
    }
  }

  /** Discover submodels and return TreeNode structures. */
  async discoverSubmodels(sourceSystemId: number): Promise<TreeNode[]> {
    try {
      const resp = await this.aasService.listSubmodels(sourceSystemId, 'SNAPSHOT').toPromise();
      const submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
      return submodels.map((sm: any) => this.mapSubmodelToNode(sm));
    } catch (error) {
      this.errorService.handleError(error as any);
      throw error;
    }
  }

  /** Perform discovery and return tree nodes, raw submodels and elements map. */
  async onDiscover(sourceSystemId: number): Promise<{ 
    treeNodes: TreeNode[], 
    submodels: any[], 
    elementsBySubmodel: Record<string, any[]> 
  }> {
    try {
      const treeNodes = await this.discoverSubmodels(sourceSystemId);
      const submodels = await this.aasService.listSubmodels(sourceSystemId, 'SNAPSHOT').toPromise();
      const elementsBySubmodel: Record<string, any[]> = {};

      for (const node of treeNodes) {
        if (node.data?.type === 'submodel') {
          try {
            const elements = await this.loadRootElements(sourceSystemId, node.data.id);
            elementsBySubmodel[node.data.id] = elements;
            node.children = elements.map(el => this.mapElementToNode(el, node.data.id));
          } catch (err) {
            
          }
        }
      }

      return { treeNodes, submodels: Array.isArray(submodels) ? submodels : [], elementsBySubmodel };
    } catch (error) {
      this.errorService.handleError(error as any);
      throw error;
    }
  }

  /** Load root elements for a submodel. */
  async loadRootElements(sourceSystemId: number, submodelId: string): Promise<any[]> {
    try {
      const smIdB64 = this.encodeIdToBase64Url(submodelId);
      const resp = await this.aasService.listElements(sourceSystemId, smIdB64, { source: 'SNAPSHOT' }).toPromise();
      return Array.isArray(resp) ? resp : (resp?.result ?? []);
    } catch (error) {
      return [];
    }
  }

  /** Load children elements for expansion and attach to the given node. */
  async loadChildren(
    sourceSystemId: number, 
    submodelId: string, 
    parentPath: string | undefined, 
    attach: TreeNode
  ): Promise<void> {
    try {
      const smIdB64 = this.encodeIdToBase64Url(submodelId);
      const resp = await this.aasService.listElements(sourceSystemId, smIdB64, { source: 'SNAPSHOT', parentPath }).toPromise();
      const elements = Array.isArray(resp) ? resp : (resp?.result ?? []);
      
      attach.children = elements.map((el: any) => this.mapElementToNode(el, submodelId, parentPath));
      attach.loading = false;
    } catch (error) {
      attach.loading = false;
    }
  }

  /** Load element details for the live panel. */
  async loadLiveElementDetails(
    sourceSystemId: number, 
    smId: string, 
    idShortPath: string | undefined, 
    node?: TreeNode
  ): Promise<ElementLivePanel> {
    try {
      const smIdB64 = this.encodeIdToBase64Url(smId);
      const resp = await this.aasService.getElement(sourceSystemId, smIdB64, idShortPath || '').toPromise();
      
      return this.mapElementToLivePanel(resp, node);
    } catch (error) {
      throw error;
    }
  }

  /** Map a submodel object to a TreeNode. */
  mapSubmodelToNode(sm: any): TreeNode {
    return {
      label: sm.idShort || sm.id || "Submodel",
      data: {
        type: "submodel",
        id: sm.id,
        raw: sm,
        modelType: sm.modelType
      },
      leaf: false,
      children: [],
      expandedIcon: "pi pi-folder-open",
      collapsedIcon: "pi pi-folder"
    };
  }

  /** Map a submodel element to a TreeNode. */
  mapElementToNode(el: any, submodelId: string, parentPath?: string): TreeNode {
    const currentPath = parentPath ? `${parentPath}.${el.idShort}` : el.idShort;
    const modelType = this.inferModelType(el);
    
    return {
      label: `${el.idShort} (${modelType})`,
      data: {
        type: "element",
        submodelId: submodelId,
        idShortPath: currentPath,
        raw: el,
        modelType: modelType
      },
      leaf: this.isLeafElement(el),
      children: [],
      expandedIcon: "pi pi-folder-open",
      collapsedIcon: "pi pi-folder"
    };
  }

  /** Infer a model type string from an element payload. */
  inferModelType(el: any): string {
    if (el.modelType) return el.modelType;
    if (el.valueType) return 'Property';
    if (el.value !== undefined) return 'Property';
    if (el.statements) return 'SubmodelElementCollection';
    if (el.typeValueListElement) return 'SubmodelElementList';
    if (el.first || el.second) return 'RelationshipElement';
    if (el.inputVariables || el.outputVariables) return 'Operation';
    return 'Unknown';
  }

  /** Determine whether a mapped element should be a leaf. */
  isLeafElement(el: any): boolean {
    const modelType = this.inferModelType(el);
    const nonLeafTypes = [
      'SubmodelElementCollection', 
      'SubmodelElementList', 
      'Entity'
    ];
    return !nonLeafTypes.includes(modelType);
  }

  /** Encode an identifier to Base64 URL-safe form. */
  encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    const b64 = typeof window !== 'undefined' && (window as any).btoa
      ? (window as any).btoa(unescape(encodeURIComponent(id)))
      : id;
    return b64
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');
  }

  /** Find a node by its key within a tree. */
  findNodeByKey(key: string, nodes: TreeNode[] | undefined): TreeNode | null {
    if (!nodes) return null;
    for (const n of nodes) {
      if (n.key === key) return n;
      const found = this.findNodeByKey(key, n.children as TreeNode[]);
      if (found) return found;
    }
    return null;
  }

  /** Refresh a node's live data if it represents an element. */
  async refreshNodeLive(
    sourceSystemId: number, 
    node: TreeNode, 
    elementsBySubmodel: Record<string, any[]>
  ): Promise<ElementLivePanel | null> {
    if (!node.data) return null;

    try {
      if (node.data.type === 'element') {
        return await this.loadLiveElementDetails(
          sourceSystemId, 
          node.data.submodelId, 
          node.data.idShortPath, 
          node
        );
      }
    } catch (error) {
    }
    return null;
  }

  /** Map an element API response to a live panel view model. */
  private mapElementToLivePanel(resp: any, node?: TreeNode): ElementLivePanel {
    const modelType = resp.modelType || this.inferModelType(resp);
    
    const panel: ElementLivePanel = {
      label: resp.idShort || 'Element',
      type: modelType
    };

    if (resp.value !== undefined) {
      panel.value = resp.value;
      panel.valueType = resp.valueType;
    }

    if (resp.min !== undefined) panel.min = resp.min;
    if (resp.max !== undefined) panel.max = resp.max;

    if (resp.inputVariables) {
      panel.inputVariables = resp.inputVariables.map((v: any) => ({
        idShort: v.idShort || 'Input',
        modelType: v.modelType,
        valueType: v.valueType
      }));
    }
    
    if (resp.outputVariables) {
      panel.outputVariables = resp.outputVariables.map((v: any) => ({
        idShort: v.idShort || 'Output',
        modelType: v.modelType,
        valueType: v.valueType
      }));
    }
    
    if (resp.inoutputVariables) {
      panel.inoutputVariables = resp.inoutputVariables.map((v: any) => ({
        idShort: v.idShort || 'InOutput',
        modelType: v.modelType,
        valueType: v.valueType
      }));
    }

    if (resp.first) panel.firstRef = resp.first;
    if (resp.second) panel.secondRef = resp.second;

    if (resp.annotations) {
      panel.annotations = resp.annotations.map((ann: any) => ({
        idShort: ann.idShort || 'Annotation',
        modelType: ann.modelType,
        valueType: ann.valueType,
        value: ann.value
      }));
    }

    return panel;
  }
}
