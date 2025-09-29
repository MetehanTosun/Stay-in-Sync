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

import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { SourceSystemAasManagementService, AasElementLivePanel } from '../../services/source-system-aas-management.service';

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
    FormsModule
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

  // AAS Create dialogs
  showAasSubmodelDialog = false;
  aasNewSubmodelJson = '{\n  "id": "https://example.com/ids/sm/new",\n  "idShort": "NewSubmodel"\n}';
  
  showAasElementDialog = false;
  aasTargetSubmodelId = '';
  aasParentPath = '';
  aasNewElementJson = '{\n  "modelType": "Property",\n  "idShort": "NewProp",\n  "valueType": "xs:string",\n  "value": "42"\n}';

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
  aasElementTemplateProperty: string = `{
  "modelType": "Property",
  "idShort": "NewProp",
  "valueType": "xs:string",
  "value": "Foo"
}`;

  aasElementTemplateRange: string = `{
  "modelType": "Range",
  "idShort": "NewRange",
  "valueType": "xs:double",
  "min": 0,
  "max": 100
}`;

  aasElementTemplateMLP: string = `{
  "modelType": "MultiLanguageProperty",
  "idShort": "Title",
  "value": [ { "language": "en", "text": "Example" } ]
}`;

  aasElementTemplateRef: string = `{
  "modelType": "ReferenceElement",
  "idShort": "Ref",
  "value": { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm" } ] }
}`;

  aasElementTemplateRel: string = `{
  "modelType": "RelationshipElement",
  "idShort": "Rel",
  "first":  { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm1" } ] },
  "second": { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm2" } ] }
}`;

  aasElementTemplateAnnRel: string = `{
  "modelType": "AnnotatedRelationshipElement",
  "idShort": "AnnRel",
  "first":  { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm1" } ] },
  "second": { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm2" } ] },
  "annotations": [ { "modelType": "Property", "idShort": "note", "valueType": "xs:string", "value": "Hello" } ]
}`;

  aasElementTemplateCollection: string = `{
  "modelType": "SubmodelElementCollection",
  "idShort": "group",
  "value": []
}`;

  aasElementTemplateList: string = `{
  "modelType": "SubmodelElementList",
  "idShort": "items",
  "typeValueListElement": "Property",
  "valueTypeListElement": "xs:string",
  "value": []
}`;

  aasElementTemplateFile: string = `{
  "modelType": "File",
  "idShort": "file1",
  "contentType": "text/plain",
  "value": "path-or-url.txt"
}`;

  aasElementTemplateOperation: string = `{
  "modelType": "Operation",
  "idShort": "Op",
  "inputVariables": [ { "value": { "modelType": "Property", "idShort": "in", "valueType": "xs:string" } } ],
  "outputVariables": []
}`;

  aasElementTemplateEntity: string = `{
  "modelType": "Entity",
  "idShort": "Ent",
  "entityType": "SelfManagedEntity",
  "statements": []
}`;

  constructor(
    private aasManagementService: SourceSystemAasManagementService
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
    this.aasTargetSubmodelId = smId;
    this.aasParentPath = parent || '';
    this.showAasElementDialog = true;
  }

  /**
   * Set AAS element template
   */
  setAasElementTemplate(kind: string): void {
    switch (kind) {
      case 'property': this.aasNewElementJson = this.aasElementTemplateProperty; break;
      case 'range': this.aasNewElementJson = this.aasElementTemplateRange; break;
      case 'mlp': this.aasNewElementJson = this.aasElementTemplateMLP; break;
      case 'ref': this.aasNewElementJson = this.aasElementTemplateRef; break;
      case 'rel': this.aasNewElementJson = this.aasElementTemplateRel; break;
      case 'annrel': this.aasNewElementJson = this.aasElementTemplateAnnRel; break;
      case 'collection': this.aasNewElementJson = this.aasElementTemplateCollection; break;
      case 'list': this.aasNewElementJson = this.aasElementTemplateList; break;
      case 'file': this.aasNewElementJson = this.aasElementTemplateFile; break;
      case 'operation': this.aasNewElementJson = this.aasElementTemplateOperation; break;
      case 'entity': this.aasNewElementJson = this.aasElementTemplateEntity; break;
      default: this.aasNewElementJson = '{}';
    }
  }

  /**
   * Handle AAS element JSON file selection
   */
  onAasElementJsonFileSelected(event: any): void {
    const file = event.files?.[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = String(reader.result || '').trim();
        if (text) {
          JSON.parse(text);
          this.aasNewElementJson = text;
        }
      } catch {
        // ignore parse error and keep current JSON
      }
    };
    reader.readAsText(file);
  }

  /**
   * Create AAS element
   */
  aasCreateElement(): void {
    if (!this.system?.id || !this.aasTargetSubmodelId) return;
    
    try {
      const body = JSON.parse(this.aasNewElementJson);
      // Use the submodelId as-is (it's already Base64-encoded)
      this.aasManagementService.createElement(this.system.id, this.aasTargetSubmodelId, body, this.aasParentPath && this.aasParentPath.trim() ? this.aasParentPath : undefined)
        .subscribe({
          next: () => {
            this.showAasElementDialog = false;
            // Trigger full tree refresh after element creation
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
   * Refresh AAS node live
   */
  private refreshAasNodeLive(submodelId: string, parentPath: string, node?: TreeNode): void {
    if (!this.system?.id) return;
    
    const key = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    this.aasManagementService.loadChildren(this.system.id, submodelId, parentPath || undefined, node || ({} as TreeNode))
      .subscribe({
        next: () => {
          if (node) {
            this.aasTreeNodes = [...this.aasTreeNodes];
          } else {
            const attachNode = this.aasManagementService.findNodeByKey(submodelId, this.aasTreeNodes);
            if (attachNode) {
              this.aasTreeNodes = [...this.aasTreeNodes];
            }
          }
        },
        error: (err) => {
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
   * Delete AAS element
   */
  deleteAasElement(submodelId: string, idShortPath: string): void {
    if (!this.system?.id || !submodelId || !idShortPath) return;
    
    this.aasManagementService.deleteElement(this.system.id, submodelId, idShortPath).subscribe({
      next: () => {
        const parent = idShortPath.includes('/') ? idShortPath.substring(0, idShortPath.lastIndexOf('/')) : '';
        const parentNode = parent ? this.aasManagementService.findNodeByKey(`${submodelId}::${parent}`, this.aasTreeNodes) : this.aasManagementService.findNodeByKey(submodelId, this.aasTreeNodes);
        this.loadAasChildren(submodelId, parent || undefined, parentNode || ({} as TreeNode));
      },
      error: (err) => {
        // Error handling
      }
    });
  }
}
