import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { TreeModule } from 'primeng/tree';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { TextareaModule } from 'primeng/textarea';
import { FileUploadModule } from 'primeng/fileupload';
import { CheckboxModule } from 'primeng/checkbox';
import { TreeNode } from 'primeng/api';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { AasManagementService, AasElementLivePanel } from '../../services/aas-management.service';
import { AasUtilityService } from '../../services/aas-utility.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { AasElementDialogComponent, AasElementDialogData, AasElementDialogResult } from '../../../../shared/components/aas-element-dialog/aas-element-dialog.component';

@Component({
  standalone: true,
  selector: 'app-aas-management',
  templateUrl: './aas-management.component.html',
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    TreeModule,
    ButtonModule,
    TooltipModule,
    DialogModule,
    TextareaModule,
    FileUploadModule,
    CheckboxModule,
    ToastModule,
    AasElementDialogComponent
  ],
  styleUrls: ['./aas-management.component.css']
})
export class AasManagementComponent implements OnInit {
  @Input() system: TargetSystemDTO | null = null;
  @Output() refreshRequested = new EventEmitter<void>();

  treeNodes: TreeNode[] = [];
  selectedNode: TreeNode | null = null;
  selectedLivePanel: AasElementLivePanel | null = null;
  isLoading = false;
  detailsLoading = false;

  // AAS Test properties
  aasTestLoading = false;
  aasTestError: string | null = null;

  // Element creation dialog
  showElementDialog = false;
  elementDialogData: AasElementDialogData | null = null;

  // Submodel creation dialog
  showSubmodelDialog = false;
  newSubmodelJson = '{\n  "id": "https://example.com/ids/sm/new",\n  "idShort": "NewSubmodel"\n}';

  // AASX upload properties
  showAasxUpload = false;
  aasxSelectedFile: File | null = null;
  isUploadingAasx = false;
  aasxPreview: any = null;
  aasxSelection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> } = { submodels: [] };

  // Templates
  minimalSubmodelTemplate: string = `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "kind": "Instance"
}`;

  propertySubmodelTemplate: string = `{
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

  collectionSubmodelTemplate: string = `{
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

  constructor(
    private aasManagement: AasManagementService,
    private aasUtility: AasUtilityService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    if (this.system) {
      this.discoverSnapshot();
    }
  }

  /**
   * Test AAS connection
   */
  testAasConnection(): void {
    if (!this.system?.id) return;
    this.aasTestLoading = true;
    this.aasTestError = null;
    
    // TODO: Implement test connection for target system
    setTimeout(() => {
      this.aasTestLoading = false;
      // For now, just simulate success
    }, 1000);
  }

  async discoverSnapshot(): Promise<void> {
    if (!this.system?.id) return;
    
    console.log('[TargetAasManage] discoverSnapshot: Starting with systemId:', this.system.id);
    this.isLoading = true;
    try {
      const newTreeNodes = await this.aasManagement.discoverSubmodels(this.system.id);
      console.log('[TargetAasManage] discoverSnapshot: Got new treeNodes:', newTreeNodes.length, newTreeNodes);
      this.treeNodes = newTreeNodes;
      console.log('[TargetAasManage] discoverSnapshot: Updated this.treeNodes:', this.treeNodes.length, this.treeNodes);
    } catch (error) {
      console.error('[TargetAasManage] discoverSnapshot: Error discovering submodels:', error);
    } finally {
      this.isLoading = false;
    }
  }

  private async retryDiscoverWithDelay(maxRetries: number, initialDelay: number): Promise<void> {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      const delay = initialDelay * attempt; // 2s, 4s, 6s
      console.log(`[TargetAasManage] Attempt ${attempt}/${maxRetries}: Waiting ${delay}ms before checking for new submodel...`);
      
      await new Promise(resolve => setTimeout(resolve, delay));
      
      console.log(`[TargetAasManage] Attempt ${attempt}: Checking for new submodel...`);
      const beforeCount = this.treeNodes.length;
      await this.discoverSnapshot();
      const afterCount = this.treeNodes.length;
      
      console.log(`[TargetAasManage] Attempt ${attempt}: Tree count ${beforeCount} -> ${afterCount}`);
      
      if (afterCount > beforeCount) {
        console.log(`[TargetAasManage] Success! New submodel found on attempt ${attempt}`);
        return;
      }
    }
    
    console.log(`[TargetAasManage] All ${maxRetries} attempts failed. New submodel not yet available.`);
    // No additional toasts; remain silent
  }

  async onNodeExpand(event: any): Promise<void> {
    const node = event.node;
    if (node.data?.type === 'submodel' && (!node.children || node.children.length === 0)) {
      await this.loadSubmodelElements(node);
    } else if (node.data?.type === 'element' && (!node.children || node.children.length === 0)) {
      await this.loadElementChildren(node);
    }
  }

  async onNodeSelect(event: any): Promise<void> {
    this.selectedNode = event.node;
    if (event.node?.data?.type === 'element') {
      await this.loadElementDetails(event.node);
    }
  }

  private async loadSubmodelElements(node: TreeNode): Promise<void> {
    if (!this.system?.id || !node.data?.id) return;
    
    try {
      const elements = await this.aasManagement.loadSubmodelElements(this.system.id, node.data.id);
      node.children = elements;
      this.treeNodes = [...this.treeNodes];
    } catch (error) {
      console.error('Error loading submodel elements:', error);
    }
  }

  private async loadElementChildren(node: TreeNode): Promise<void> {
    if (!this.system?.id || !node.data?.submodelId || !node.data?.idShortPath) return;
    
    try {
      const children = await this.aasManagement.loadElementChildren(
        this.system.id, 
        node.data.submodelId, 
        node.data.idShortPath
      );
      node.children = children;
      this.treeNodes = [...this.treeNodes];
    } catch (error) {
      console.error('Error loading element children:', error);
    }
  }

  private async loadElementDetails(node: TreeNode): Promise<void> {
    if (!this.system?.id || !node.data?.submodelId || !node.data?.idShortPath) {
      console.error('[TargetAasManage] loadElementDetails: Missing required data', {
        systemId: this.system?.id,
        submodelId: node.data?.submodelId,
        idShortPath: node.data?.idShortPath,
        nodeData: node.data
      });
      return;
    }
    
    console.log('[TargetAasManage] loadElementDetails: Loading element details', {
      systemId: this.system.id,
      submodelId: node.data.submodelId,
      idShortPath: node.data.idShortPath,
      nodeKey: node.key,
      nodeLabel: node.label,
      fullNodeData: node.data
    });
    
    this.detailsLoading = true;
    try {
      console.log('[TargetAasManage] Calling aasManagement.loadElementDetails with:', {
        systemId: this.system.id,
        submodelId: node.data.submodelId,
        idShortPath: node.data.idShortPath
      });
      
      this.selectedLivePanel = await this.aasManagement.loadElementDetails(
        this.system.id,
        node.data.submodelId,
        node.data.idShortPath
      );
      
      console.log('[TargetAasManage] Element details loaded successfully:', this.selectedLivePanel);
    } catch (error) {
      console.error('[TargetAasManage] Error loading element details:', error);
      console.error('[TargetAasManage] Error details:', {
        error: error,
        systemId: this.system.id,
        submodelId: node.data.submodelId,
        idShortPath: node.data.idShortPath
      });
    } finally {
      this.detailsLoading = false;
    }
  }

  getAasId(): string {
    return this.aasUtility.getAasId(this.system);
  }

  /**
   * Fix tree structure after live refresh to ensure correct idShortPath
   */
  private fixTreeStructureAfterRefresh(elementData: any): void {
    console.log('[TargetAasManage] fixTreeStructureAfterRefresh called with:', elementData);
    
    if (!elementData) {
      console.log('[TargetAasManage] fixTreeStructureAfterRefresh: Missing elementData');
      return;
    }
    
    const elementIdShort = elementData.body?.idShort;
    if (!elementIdShort) {
      console.log('[TargetAasManage] fixTreeStructureAfterRefresh: Missing elementIdShort', {
        body: elementData.body
      });
      return;
    }
    
    // Build the correct idShortPath
    let correctIdShortPath: string;
    if (elementData.parentPath) {
      correctIdShortPath = elementData.parentPath + '/' + elementIdShort;
    } else {
      // For root elements, just use the idShort
      correctIdShortPath = elementIdShort;
    }
    
    console.log('[TargetAasManage] Fixing tree structure after live refresh', {
      parentPath: elementData.parentPath,
      elementIdShort: elementIdShort,
      correctIdShortPath: correctIdShortPath,
      fullElementData: elementData
    });
    
    // Find and update the element in the tree
    this.updateElementInTree(this.treeNodes, elementIdShort, correctIdShortPath);
    
    console.log('[TargetAasManage] Fixed tree structure', {
      elementIdShort,
      correctIdShortPath
    });
  }

  /**
   * Update element in tree structure with correct idShortPath
   */
  private updateElementInTree(nodes: TreeNode[], elementIdShort: string, correctIdShortPath: string): void {
    if (!nodes) return;
    
    for (const node of nodes) {
      if (node.data?.idShort === elementIdShort) {
        console.log('[TargetAasManage] Updating element in tree', {
          old: node.data.idShortPath,
          new: correctIdShortPath
        });
        node.data.idShortPath = correctIdShortPath;
        return;
      }
      
      if (node.children) {
        this.updateElementInTree(node.children, elementIdShort, correctIdShortPath);
      }
    }
  }


  getNodeType(node: TreeNode): string {
    if (node.data?.type === 'submodel') {
      return node.data?.modelType || (node.data?.raw?.kind?.toLowerCase?.().includes('template') ? 'Submodel Template' : 'Submodel');
    }
    return node.data?.modelType || node.data?.raw?.modelType || (node.data?.raw?.valueType ? 'Property' : 'Element');
  }

  canAddChild(node: TreeNode): boolean {
    const type = node.data?.modelType;
    return type === 'SubmodelElementCollection' || type === 'SubmodelElementList' || type === 'Entity';
  }

  canSetValue(node: TreeNode): boolean {
    return node.data?.modelType === 'Property' || !!node.data?.raw?.valueType;
  }

  isCollectionType(type: string): boolean {
    return type === 'SubmodelElementCollection' || type === 'SubmodelElementList';
  }

  // Event handlers for AAS management actions
  openCreateSubmodel(): void {
    this.showSubmodelDialog = true;
  }

  setSubmodelTemplate(kind: 'minimal'|'property'|'collection'): void {
    if (kind === 'minimal') this.newSubmodelJson = this.minimalSubmodelTemplate;
    if (kind === 'property') this.newSubmodelJson = this.propertySubmodelTemplate;
    if (kind === 'collection') this.newSubmodelJson = this.collectionSubmodelTemplate;
  }

  async createSubmodel(): Promise<void> {
    if (!this.system?.id) return;
    
    try {
      const body = JSON.parse(this.newSubmodelJson);
      await this.aasManagement.createSubmodel(this.system.id, body);
      this.showSubmodelDialog = false;
      
      // Refresh snapshot silently
      await this.discoverSnapshot();
      
      this.messageService.add({
        key: 'targetAAS',
        severity: 'success',
        summary: 'Submodel Created',
        detail: 'Submodel created successfully.',
        life: 3000
      });
    } catch (e: any) {
      console.error('Error creating submodel:', e);
      this.messageService.add({
        key: 'targetAAS',
        severity: 'error',
        summary: 'Error',
        detail: e?.message || 'Failed to create submodel. Please check the JSON format.',
        life: 5000
      });
    }
  }

  openCreateElement(submodelId: string, parentPath?: string): void {
    if (!this.system?.id) return;
    
    this.elementDialogData = {
      submodelId: submodelId,
      parentPath: parentPath,
      systemId: this.system.id,
      systemType: 'target'
    };
    this.showElementDialog = true;
  }

  onElementDialogResult(result: AasElementDialogResult): void {
    if (result.success && result.element) {
      this.handleElementCreation(result.element);
    } else if (result.error) {
      console.error('[TargetAasManage] Element creation failed:', result.error);
      // Show toast for duplicate idShort error
      if (result.error.includes('Duplicate entry') || result.error.includes('uk_element_submodel_idshortpath')) {
        this.messageService.add({
          key: 'targetAAS',
          severity: 'error',
          summary: 'Duplicate Element',
          detail: 'An element with this idShort already exists. Please use a different idShort.',
          life: 5000
        });
      } else {
        this.messageService.add({
          key: 'targetAAS',
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
      console.log('[TargetAasManage] Creating element:', elementData);
      
      // Encode parentPath for BaSyx (convert / to .)
      const encodedParentPath = elementData.parentPath ? 
        elementData.parentPath.replace(/\//g, '.') : 
        undefined;
      
      console.log('[TargetAasManage] Encoded parentPath:', encodedParentPath);
      
      // Use the AAS management service to create the element
      await this.aasManagement.createElement(
        this.system.id,
        elementData.submodelId,
        elementData.body,
        encodedParentPath
      );
      
      console.log('[TargetAasManage] Element created successfully');
      
      // Show success toast
      this.messageService.add({
        key: 'targetAAS',
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
      
      // Use live refresh like in create dialog
      console.log('[TargetAasManage] Triggering live refresh after element creation');
      await this.discoverSnapshot();
      
      // Fix tree structure immediately after element creation
      console.log('[TargetAasManage] Fixing tree structure immediately after element creation');
      this.fixTreeStructureAfterRefresh(elementData);
      
      // Force live refresh after a short delay to ensure backend is updated
      setTimeout(async () => {
        console.log('[TargetAasManage] Force live refresh for deep elements');
        await this.discoverSnapshot();
        
        // Fix tree structure again after live refresh
        this.fixTreeStructureAfterRefresh(elementData);
      }, 1000);
      
    } catch (error) {
      console.error('[TargetAasManage] Error creating element:', error);
      
      // Show error toast
      const errorMessage = String((error as any)?.error || (error as any)?.message || 'Failed to create element');
      if (errorMessage.includes('Duplicate entry')) {
        this.messageService.add({
          key: 'targetAAS',
          severity: 'error',
          summary: 'Duplicate Element',
          detail: 'An element with this idShort already exists. Please use a different idShort.',
          life: 5000
        });
      } else {
        this.messageService.add({
          key: 'targetAAS',
          severity: 'error',
          summary: 'Error',
          detail: errorMessage,
          life: 5000
        });
      }
    }
  }

  async deleteSubmodel(submodelId: string): Promise<void> {
    if (!this.system?.id) return;
    
    try {
      await this.aasManagement.deleteSubmodel(this.system.id, submodelId);
      this.refreshRequested.emit();
      await this.discoverSnapshot();
      this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Submodel deleted', detail: 'Submodel removed from shell' });
    } catch (error: any) {
      console.error('Error deleting submodel:', error);
      this.messageService.add({ key: 'targetAAS', severity: 'error', summary: 'Error', detail: ((error as any)?.message || 'Failed to delete submodel') });
    }
  }

  async deleteElement(submodelId: string, elementPath: string): Promise<void> {
    if (!this.system?.id) return;
    
    try {
      await this.aasManagement.deleteElement(this.system.id, submodelId, elementPath);
      
      // Trigger discover to refresh the entire tree (same as source system)
      console.log('[TargetAasManagement] deleteElement: Triggering discover to refresh tree');
      this.discoverSnapshot();
      this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Element Deleted', detail: 'Element has been successfully deleted.', life: 3000 });
    } catch (error: any) {
      console.error('Error deleting element:', error);
      this.messageService.add({ key: 'targetAAS', severity: 'error', summary: 'Error', detail: ((error as any)?.message || 'Failed to delete element') });
    }
  }

  openSetValue(submodelId: string, elementData: any): void {
    // TODO: Implement set value dialog
    console.log('Set value clicked', submodelId, elementData);
  }

  private findParentNode(submodelId: string, elementPath: string): TreeNode | null {
    const parentPath = this.aasUtility.getParentPath(elementPath);
    
    // Find the submodel node
    const submodelNode = this.treeNodes.find(node => node.data?.id === submodelId);
    if (!submodelNode) return null;
    
    // If no parent path, return the submodel node
    if (!parentPath) return submodelNode;
    
    // Find the parent element node
    return this.findNodeByPath(submodelNode, parentPath);
  }

  private findNodeByPath(parentNode: TreeNode, path: string): TreeNode | null {
    if (!parentNode.children) return null;
    
    // Use dot-separated paths for BaSyx compatibility
    const pathParts = path.split('.');
    let current = parentNode;
    
    for (const part of pathParts) {
      const child = current.children?.find(child => child.data?.idShortPath === part);
      if (!child) return null;
      current = child;
    }
    
    return current;
  }

  // AASX upload methods
  openAasxUpload(): void {
    this.showAasxUpload = true;
    this.aasxSelectedFile = null;
  }

  onAasxFileSelected(event: any): void {
    this.aasxSelectedFile = event.files?.[0] || null;
    if (this.aasxSelectedFile && this.system?.id) {
      // Load preview to enable selective attach
      this.aasManagement.previewAasx(this.system.id, this.aasxSelectedFile).then((resp: any) => {
        this.aasxPreview = resp?.submodels || (resp?.result ?? []);
        // Normalize to array of {id,idShort,kind}
        const arr = Array.isArray(this.aasxPreview) ? this.aasxPreview : (this.aasxPreview?.submodels ?? []);
        this.aasxSelection = { submodels: (arr || []).map((sm: any) => ({ id: sm.id || sm.submodelId, full: true, elements: [] })) };
      }).catch((err: any) => {
        this.aasxPreview = null;
        this.aasxSelection = { submodels: [] };
      });
    }
  }

  // AASX selective attach helpers
  private getSmId(sm: any): string {
    return sm?.id || sm?.submodelId || '';
  }

  getOrInitAasxSelFor(sm: any): { id: string; full: boolean; elements: string[] } {
    const id = this.getSmId(sm);
    let found = this.aasxSelection.submodels.find((s) => s.id === id);
    if (!found) {
      found = { id, full: true, elements: [] };
      this.aasxSelection.submodels.push(found);
    }
    return found;
  }

  toggleAasxSubmodelFull(sm: any, checked: boolean): void {
    const sel = this.getOrInitAasxSelFor(sm);
    sel.full = checked;
    if (checked) {
      sel.elements = [];
    }
  }


  uploadAasx(): void {
    if (this.isUploadingAasx) return;
    if (!this.aasxSelectedFile || !this.system?.id) {
      this.messageService.add({ key: 'targetAAS', severity: 'warn', summary: 'No file selected', detail: 'Please choose an .aasx file.' });
      return;
    }
    
    this.messageService.add({ key: 'targetAAS', severity: 'info', summary: 'Uploading AASX', detail: `${this.aasxSelectedFile?.name} (${this.aasxSelectedFile?.size} bytes)` });
    this.isUploadingAasx = true;
    
    // If preview is available and user made a selection, use selective attach; else default upload
    const hasSelection = (this.aasxSelection?.submodels?.some(s => s.full) ?? false);
    const req$ = hasSelection ? 
      this.aasManagement.attachSelectedAasx(this.system.id, this.aasxSelectedFile, this.aasxSelection) : 
      this.aasManagement.uploadAasx(this.system.id, this.aasxSelectedFile);
    
    req$.then(() => {
      this.isUploadingAasx = false;
      this.showAasxUpload = false;
      // Refresh the tree to show uploaded content
      this.discoverSnapshot();
      this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Upload accepted', detail: 'AASX uploaded successfully.' });
    }).catch((error: any) => {
      this.isUploadingAasx = false;
      this.messageService.add({ key: 'targetAAS', severity: 'error', summary: 'Upload failed', detail: error?.message || 'See console for details' });
    });
  }
}
