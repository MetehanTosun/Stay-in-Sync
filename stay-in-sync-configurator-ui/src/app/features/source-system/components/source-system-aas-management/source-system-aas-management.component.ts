import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TreeModule } from 'primeng/tree';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { TreeNode } from 'primeng/api';
import { MessageService } from 'primeng/api';

import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { SourceSystemAasManagementService, AasElementLivePanel } from '../../services/source-system-aas-management.service';
import { AasService } from '../../services/aas.service';
import { AasElementDialogComponent, AasElementDialogData, AasElementDialogResult } from '../../../../shared/components/aas-element-dialog/aas-element-dialog.component';

@Component({
  standalone: true,
  selector: 'app-source-system-aas-management',
  templateUrl: './source-system-aas-management.component.html',
  styleUrls: ['./source-system-aas-management.component.css'],
  imports: [
    CommonModule,
    TreeModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    FileUploadModule,
    MessageModule,
    FormsModule,
    AasElementDialogComponent
  ]
})
export class SourceSystemAasManagementComponent implements OnInit {
  @Input() system: SourceSystemDTO | null = null;
  @Output() refreshRequested = new EventEmitter<void>();

  // AAS Tree properties
  aasTreeNodes: TreeNode[] = [];
  aasTreeLoading = false;
  selectedAasNode: TreeNode | null = null;
  aasSelectedLivePanel: AasElementLivePanel | null = null;
  aasSelectedLiveLoading = false;

  // AAS Test properties
  aasTestLoading = false;
  aasTestError: string | null = null;

  // Element creation dialog
  showElementDialog = false;
  elementDialogData: AasElementDialogData | null = null;

  // AAS Create dialogs
  showAasSubmodelDialog = false;
  aasNewSubmodelJson = '{\n  "id": "https://example.com/ids/sm/new",\n  "idShort": "NewSubmodel"\n}';
  

  // AAS Value dialog
  showAasValueDialog = false;
  aasValueSubmodelId = '';
  aasValueElementPath = '';
  aasValueTypeHint = 'xs:string';
  aasValueNew = '';

  // Templates
  aasMinimalSubmodelTemplate: string = `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "kind": "Instance"
}`;

  aasPropertySubmodelTemplate: string = `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "submodelElements": [
    {
      "modelType": "Property",
      "idShort": "Name",
      "valueType": "xs:string",
      "value": "Foo"
    }
  ]
}`;

  aasCollectionSubmodelTemplate: string = `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "submodelElements": [
    {
      "modelType": "SubmodelElementCollection",
      "idShort": "address",
      "value": [
        { "modelType": "Property", "idShort": "street", "valueType": "xs:string", "value": "Main St" }
      ]
    }
  ]
}`;

  // Element templates

  constructor(
    private aasManagementService: SourceSystemAasManagementService,
    private aasService: AasService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    // Component initialization
  }

  /**
   * Check if AAS is selected
   */
  isAasSelected(): boolean {
    return (this.system?.apiType || '').toUpperCase().includes('AAS');
  }

  /**
   * Get AAS ID with fallback logic
   */
  getAasId(): string {
    if (!this.system) return '-';
    
    // If aasId is explicitly set, use it
    if (this.system.aasId && this.system.aasId.trim() !== '') {
      return this.system.aasId;
    }
    
    // Fallback: try to extract from API URL or use a default
    if (this.system.apiUrl) {
      try {
        const url = new URL(this.system.apiUrl);
        
        // For localhost, try to extract AAS ID from path or use system name
        if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
          // Try to extract AAS ID from path (e.g., /aas/12345/...)
          const pathMatch = url.pathname.match(/\/aas\/([^\/]+)/);
          if (pathMatch && pathMatch[1]) {
            return pathMatch[1];
          }
          
          // Try to extract from query parameters
          const aasIdParam = url.searchParams.get('aasId') || url.searchParams.get('id');
          if (aasIdParam) {
            return aasIdParam;
          }
          
          // Use system name as AAS ID if available
          if (this.system.name && this.system.name.trim() !== '') {
            return this.system.name;
          }
          
          // Final localhost fallback
          return 'Default AAS';
        }
        
        // For other hosts, use hostname as AAS ID
        return url.hostname;
      } catch (e) {
        return 'Unknown';
      }
    }
    
    return '-';
  }

  /**
   * Test AAS connection
   */
  testAasConnection(): void {
    if (!this.system?.id) return;
    this.aasTestLoading = true;
    this.aasTestError = null;
    
    this.aasManagementService.testConnection(this.system.id).subscribe({
      next: () => {
        this.aasTestLoading = false;
      },
      error: (err) => {
        this.aasTestLoading = false;
        this.aasTestError = 'Connection failed. Please verify Base URL, AAS ID and auth.';
      }
    });
  }

  /**
   * Discover AAS snapshot
   */
  discoverAasSnapshot(): void {
    if (!this.system?.id) return;
    this.aasTreeLoading = true;
    
    this.aasManagementService.discoverSnapshot(this.system.id).subscribe({
      next: (treeNodes) => {
        this.aasTreeNodes = treeNodes;
        this.aasTreeLoading = false;
      },
      error: (err) => {
        this.aasTreeLoading = false;
      }
    });
  }

  /**
   * Handle AAS node expand
   */
  onAasNodeExpand(event: any): void {
    const node: TreeNode = event.node;
    if (!node || !this.system?.id) return;
    
    if (node.data?.type === 'submodel') {
      this.loadAasChildren(node.data.id, undefined, node);
    } else if (node.data?.type === 'element') {
      this.loadAasChildren(node.data.submodelId, node.data.idShortPath, node);
    }
  }

  /**
   * Handle AAS node select
   */
  onAasNodeSelect(event: any): void {
    const node: TreeNode = event.node;
    this.selectedAasNode = node;
    this.aasSelectedLivePanel = null;
    
    if (!node || node.data?.type !== 'element') return;
    
    const smId: string = node.data.submodelId;
    const idShortPath: string = node.data.idShortPath;
    this.loadAasLiveElementDetails(smId, idShortPath, node);
    
    setTimeout(() => {
      const el = document.getElementById('aas-element-details');
      if (el && el.scrollIntoView) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 0);
  }

  /**
   * Load AAS children
   */
  private loadAasChildren(submodelId: string, parentPath: string | undefined, attach: TreeNode): void {
    if (!this.system?.id) return;
    
    this.aasManagementService.loadChildren(this.system.id, submodelId, parentPath, attach).subscribe({
      next: () => {
        this.aasTreeNodes = [...this.aasTreeNodes];
      },
      error: (err) => {
        // Error handling
      }
    });
  }

  /**
   * Load AAS live element details
   */
  private loadAasLiveElementDetails(smId: string, idShortPath: string | undefined, node?: TreeNode): void {
    if (!this.system?.id) return;
    
    this.aasSelectedLiveLoading = true;
    
    this.aasManagementService.loadElementDetails(this.system.id, smId, idShortPath, node).subscribe({
      next: (livePanel) => {
        this.aasSelectedLiveLoading = false;
        this.aasSelectedLivePanel = livePanel;
        
        if (node && node.data) {
          const computedPath = idShortPath || (node.key as string).split('::')[1] || '';
          node.data.idShortPath = computedPath;
          node.data.modelType = livePanel.type || node.data.modelType;
          node.data.raw = { ...(node.data.raw || {}), idShortPath: computedPath, modelType: livePanel.type };
          this.aasTreeNodes = [...this.aasTreeNodes];
        }
      },
      error: (err) => {
        this.aasSelectedLiveLoading = false;
      }
    });
  }

  /**
   * Open AAS create submodel dialog
   */
  openAasCreateSubmodel(): void {
    this.showAasSubmodelDialog = true;
  }

  /**
   * Set AAS submodel template
   */
  setAasSubmodelTemplate(kind: 'minimal'|'property'|'collection'): void {
    if (kind === 'minimal') this.aasNewSubmodelJson = this.aasMinimalSubmodelTemplate;
    if (kind === 'property') this.aasNewSubmodelJson = this.aasPropertySubmodelTemplate;
    if (kind === 'collection') this.aasNewSubmodelJson = this.aasCollectionSubmodelTemplate;
  }

  /**
   * Create AAS submodel
   */
  aasCreateSubmodel(): void {
    if (!this.system?.id) return;
    
    try {
      const body = JSON.parse(this.aasNewSubmodelJson);
      this.aasManagementService.createSubmodel(this.system.id, body).subscribe({
        next: () => {
          this.showAasSubmodelDialog = false;
          this.discoverAasSnapshot();
        },
        error: (err) => {
          // Error handling
        }
      });
    } catch (e) {
      // Error handling
    }
  }

  /**
   * Open AAS create element dialog
   */
  openAasCreateElement(smId: string, parent?: string): void {
    if (!this.system?.id) return;
    
    this.elementDialogData = {
      submodelId: smId,
      parentPath: parent,
      systemId: this.system.id,
      systemType: 'source'
    };
    this.showElementDialog = true;
  }

  /**
   * Handle element dialog result
   */
  onElementDialogResult(result: AasElementDialogResult): void {
    if (result.success && result.element) {
      this.handleElementCreation(result.element);
    } else if (result.error) {
      console.error('[SourceAasManage] Element creation failed:', result.error);
      // Show toast for duplicate idShort error
      if (result.error.includes('Duplicate entry') || result.error.includes('uk_element_submodel_idshortpath')) {
        this.messageService.add({
          severity: 'error',
          summary: 'Duplicate Element',
          detail: 'An element with this idShort already exists. Please use a different idShort.',
          life: 5000
        });
      } else {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: result.error,
          life: 5000
        });
      }
    }
  }

  private async handleElementCreation(elementData: any): Promise<void> {
    if (!this.system?.id) return;
    
    try {
      console.log('[SourceAasManage] Creating element:', elementData);
      
      // Use the AAS service to create the element
      const smIdB64 = this.aasService.encodeIdToBase64Url(elementData.submodelId);
      await this.aasService.createElement(
        this.system.id,
        smIdB64,
        elementData.body,
        elementData.parentPath
      ).toPromise();
      
      console.log('[SourceAasManage] Element created successfully');
      
      // Show success toast
      this.messageService.add({
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
      
      // Use live refresh like in create dialog
      console.log('[SourceAasManage] Triggering live refresh after element creation');
      this.discoverAasSnapshot();
      
      // Force live refresh after a short delay to ensure backend is updated
      setTimeout(() => {
        console.log('[SourceAasManage] Force live refresh for deep elements');
        this.discoverAasSnapshot();
      }, 1000);
      
    } catch (error) {
      console.error('[SourceAasManage] Error creating element:', error);
      
      // Show error toast
      const errorMessage = String((error as any)?.error || (error as any)?.message || 'Failed to create element');
      if (errorMessage.includes('Duplicate entry')) {
        this.messageService.add({
          severity: 'error',
          summary: 'Duplicate Element',
          detail: 'An element with this idShort already exists. Please use a different idShort.',
          life: 5000
        });
      } else {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: errorMessage,
          life: 5000
        });
      }
    }
  }



  /**
   * Create AAS element (same logic as create dialog)
   */


  private findAasNodeByKey(key: string, nodes: TreeNode[] | undefined): TreeNode | null {
    if (!nodes) return null;
    for (const n of nodes) {
      if (n.key === key) return n;
      const found = this.findAasNodeByKey(key, n.children as TreeNode[]);
      if (found) return found;
    }
    return null;
  }

  private mapElementToNode(submodelId: string, element: any): TreeNode {
    const idShortPath = element.idShortPath || element.idShort;
    const key = `${submodelId}::${idShortPath}`;
    
    return {
      key: key,
      label: element.idShort || element.idShortPath || 'Unknown',
      data: {
        type: 'element',
        submodelId: submodelId,
        idShortPath: idShortPath,
        modelType: element.modelType,
        raw: element
      },
      children: []
    };
  }

  /**
   * Refresh AAS node live (same logic as create dialog)
   */
  private refreshAasNodeLive(submodelId: string, parentPath: string, node?: TreeNode): void {
    if (!this.system?.id) {
      console.log('[SourceAasManage] refreshAasNodeLive: No system ID');
      return;
    }
    
    const key = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    console.log('[SourceAasManage] refreshAasNodeLive: Starting refresh', {
      submodelId,
      parentPath,
      key,
      node: node?.label
    });
    
    // Use the same service call as create dialog
    this.aasService.listElements(this.system.id, submodelId, { depth: 'shallow', parentPath: parentPath || undefined, source: 'LIVE' })
      .subscribe({
        next: (resp) => {
          console.log('[SourceAasManage] refreshAasNodeLive: Response received', {
            key,
            response: resp,
            node: node?.label
          });
          
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const mapped = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElementToNode(submodelId, el);
          });
          
          console.log('[SourceAasManage] refreshAasNodeLive: Mapped elements', {
            key,
            mappedCount: mapped.length,
            mapped: mapped.map((m: any) => m.label)
          });
          
          if (node) {
            node.children = mapped;
            this.aasTreeNodes = [...this.aasTreeNodes];
          } else {
            const attachNode = this.findAasNodeByKey(submodelId, this.aasTreeNodes);
            if (attachNode) {
              attachNode.children = mapped;
              this.aasTreeNodes = [...this.aasTreeNodes];
            }
          }
        },
        error: (err) => {
          console.error('[SourceAasManage] refreshAasNodeLive: Error', err);
          // Error handling
        }
      });
  }

  /**
   * Open AAS set value dialog
   */
  openAasSetValue(smId: string, element: any): void {
    this.aasValueSubmodelId = smId;
    this.aasValueElementPath = element.idShortPath || element.data?.idShortPath || element.raw?.idShortPath || element.idShort;
    this.aasValueTypeHint = element.valueType || 'xs:string';
    
    if (this.aasSelectedLivePanel && this.selectedAasNode && this.selectedAasNode.data?.idShortPath === this.aasValueElementPath) {
      this.aasValueNew = (this.aasSelectedLivePanel.value ?? '').toString();
    } else {
      this.aasValueNew = '';
    }
    this.showAasValueDialog = true;
  }

  /**
   * Set AAS value
   */
  aasSetValue(): void {
    if (!this.system?.id || !this.aasValueSubmodelId || !this.aasValueElementPath) return;
    
    const parsedValue = this.aasManagementService.parseValueForType(this.aasValueNew, this.aasValueTypeHint);
    this.aasManagementService.setPropertyValue(this.system.id, this.aasValueSubmodelId, this.aasValueElementPath, parsedValue)
      .subscribe({
        next: () => {
          this.showAasValueDialog = false;
          // refresh live details
          const node = this.selectedAasNode;
          if (node?.data) {
            this.loadAasLiveElementDetails(node.data.submodelId, node.data.idShortPath, node);
          } else {
            const parent = this.aasValueElementPath.includes('/') ? this.aasValueElementPath.substring(0, this.aasValueElementPath.lastIndexOf('/')) : '';
            const parentNode = parent ? this.aasManagementService.findNodeByKey(`${this.aasValueSubmodelId}::${parent}`, this.aasTreeNodes) : this.aasManagementService.findNodeByKey(this.aasValueSubmodelId, this.aasTreeNodes);
            this.refreshAasNodeLive(this.aasValueSubmodelId, parent, parentNode || undefined);
          }
        },
        error: (err) => {
          // Error handling
        }
      });
  }

  /**
   * Delete AAS submodel
   */
  deleteAasSubmodel(submodelId: string): void {
    if (!this.system?.id || !submodelId) return;
    
    this.aasManagementService.deleteSubmodel(this.system.id, submodelId).subscribe({
      next: () => {
        this.discoverAasSnapshot();
      },
      error: (err) => {
        // Error handling
      }
    });
  }

  /**
   * Delete AAS element (same logic as create dialog)
   */
  deleteAasElement(submodelId: string, idShortPath: string): void {
    if (!this.system?.id || !submodelId || !idShortPath) return;
    
    console.log('[SourceAasManage] deleteAasElement: Starting deletion', {
      systemId: this.system.id,
      submodelId,
      idShortPath
    });
    
    this.aasManagementService.deleteElement(this.system.id, submodelId, idShortPath).subscribe({
      next: () => {
        console.log('[SourceAasManage] deleteAasElement: Element deleted successfully');
        // Use the same refresh logic as create dialog
        this.refreshAasTreeAfterDelete(submodelId, idShortPath);
      },
      error: (err) => {
        console.error('[SourceAasManage] deleteAasElement: Error deleting element', err);
        // Error handling
      }
    });
  }

  /**
   * Refresh AAS tree after delete (same logic as create dialog)
   */
  private refreshAasTreeAfterDelete(submodelId: string, idShortPath: string): void {
    console.log('[SourceAasManage] refreshAasTreeAfterDelete: Starting refresh', {
      submodelId,
      idShortPath
    });
    
    const parent = idShortPath.includes('.') ? idShortPath.substring(0, idShortPath.lastIndexOf('.')) : '';
    console.log('[SourceAasManage] refreshAasTreeAfterDelete: Parent path', {
      idShortPath,
      parent
    });
    
    if (parent) {
      const key = `${submodelId}::${parent}`;
      const parentNode = this.findAasNodeByKey(key, this.aasTreeNodes);
      console.log('[SourceAasManage] refreshAasTreeAfterDelete: Parent node found', {
        key,
        parentNode: parentNode,
        found: !!parentNode
      });
      
      if (parentNode) {
        this.refreshAasNodeLive(submodelId, parent, parentNode);
      } else {
        console.log('[SourceAasManage] refreshAasTreeAfterDelete: Parent node not found, refreshing root');
        this.refreshAasNodeLive(submodelId, '', undefined);
      }
    } else {
      console.log('[SourceAasManage] refreshAasTreeAfterDelete: No parent path, refreshing root');
      this.refreshAasNodeLive(submodelId, '', undefined);
    }
  }
}
