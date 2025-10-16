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

/**
 * Component responsible for managing AAS (Asset Administration Shell) interactions for a Target System.
 * Provides UI for viewing, creating, deleting, and editing submodels and elements.
 * Also supports AASX upload and preview functionality.
 */
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

  /**
   * Initializes the AAS Management component and triggers submodel discovery when a system is provided.
   */
  ngOnInit(): void {
    if (this.system) {
      this.discoverSnapshot();
    }
  }

  /**
   * Simulates testing the AAS connection for the selected Target System.
   * Displays a loading indicator during the test.
   */
  testAasConnection(): void {
    if (!this.system?.id) return;
    this.aasTestLoading = true;
    this.aasTestError = null;
    setTimeout(() => {
      this.aasTestLoading = false;
    }, 1000);
  }

  /**
   * Discovers all submodels for the selected Target System.
   * Fetches data from backend service and updates the tree structure.
   */
  async discoverSnapshot(): Promise<void> {
    if (!this.system?.id) return;
    this.isLoading = true;
    try {
      const newTreeNodes = await this.aasManagement.discoverSubmodels(this.system.id);
      this.treeNodes = newTreeNodes;
    } catch (error) {
      console.error('[TargetAasManage] discoverSnapshot: Error discovering submodels:', error);
    } finally {
      this.isLoading = false;
    }
  }

  /**
   * Repeatedly attempts to rediscover AAS submodels after a delay, up to a defined maximum number of retries.
   * @param maxRetries Maximum number of attempts.
   * @param initialDelay Delay in milliseconds before the first retry.
   */
  private async retryDiscoverWithDelay(maxRetries: number, initialDelay: number): Promise<void> {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      const delay = initialDelay * attempt;
      await new Promise(resolve => setTimeout(resolve, delay));
      const beforeCount = this.treeNodes.length;
      await this.discoverSnapshot();
      const afterCount = this.treeNodes.length;
      if (afterCount > beforeCount) {
        return;
      }
    }
    // No additional toasts; remain silent
  }

  /**
   * Handles expansion of a tree node and loads submodel or element children dynamically.
   * @param event PrimeNG Tree expansion event.
   */
  async onNodeExpand(event: any): Promise<void> {
    const node = event.node;
    if (node.data?.type === 'submodel' && (!node.children || node.children.length === 0)) {
      await this.loadSubmodelElements(node);
    } else if (node.data?.type === 'element' && (!node.children || node.children.length === 0)) {
      await this.loadElementChildren(node);
    }
  }

  /**
   * Handles selection of a node within the AAS tree.
   * Loads live element details when an element node is selected.
   * @param event PrimeNG Tree selection event.
   */
  async onNodeSelect(event: any): Promise<void> {
    this.selectedNode = event.node;
    if (event.node?.data?.type === 'element') {
      await this.loadElementDetails(event.node);
    }
  }

  /**
   * Loads all elements of a submodel from the backend and attaches them to the node.
   * @param node The submodel TreeNode to load elements for.
   */
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

  /**
   * Loads all child elements of an AAS element within a submodel and updates the tree structure.
   * @param node The TreeNode representing the parent element.
   */
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

  /**
   * Loads detailed information about an individual AAS element.
   * @param node The selected TreeNode whose details should be fetched.
   */
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
    this.detailsLoading = true;
    try {
      this.selectedLivePanel = await this.aasManagement.loadElementDetails(
        this.system.id,
        node.data.submodelId,
        node.data.idShortPath
      );
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
   * Fixes idShortPath values in the AAS tree after a live refresh or element creation.
   * @param elementData Data of the affected element used for path correction.
   */
  private fixTreeStructureAfterRefresh(elementData: any): void {
    if (!elementData) {
      return;
    }
    const elementIdShort = elementData.body?.idShort;
    if (!elementIdShort) {
      return;
    }
    let correctIdShortPath: string;
    if (elementData.parentPath) {
      correctIdShortPath = elementData.parentPath + '/' + elementIdShort;
    } else {
      correctIdShortPath = elementIdShort;
    }
    this.updateElementInTree(this.treeNodes, elementIdShort, correctIdShortPath);
  }

  /**
   * Updates a specific element's idShortPath within the AAS tree.
   * @param nodes The list of TreeNodes to search through.
   * @param elementIdShort The idShort of the element to update.
   * @param correctIdShortPath The corrected idShortPath value.
   */
  private updateElementInTree(nodes: TreeNode[], elementIdShort: string, correctIdShortPath: string): void {
    if (!nodes) return;
    for (const node of nodes) {
      if (node.data?.idShort === elementIdShort) {
        node.data.idShortPath = correctIdShortPath;
        return;
      }
      if (node.children) {
        this.updateElementInTree(node.children, elementIdShort, correctIdShortPath);
      }
    }
  }


  /**
   * Determines the display type of a node (e.g., Submodel, Property, Collection).
   * @param node TreeNode to evaluate.
   * @returns The readable node type string.
   */
  getNodeType(node: TreeNode): string {
    if (node.data?.type === 'submodel') {
      return node.data?.modelType || (node.data?.raw?.kind?.toLowerCase?.().includes('template') ? 'Submodel Template' : 'Submodel');
    }
    return node.data?.modelType || node.data?.raw?.modelType || (node.data?.raw?.valueType ? 'Property' : 'Element');
  }

  /**
   * Determines if a new child element can be added to the selected node.
   * @param node The TreeNode to check.
   * @returns True if the node supports children.
   */
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

  /**
   * Creates a new submodel using JSON data from the submodel creation dialog.
   * Displays a success or error message depending on the outcome.
   */
  async createSubmodel(): Promise<void> {
    if (!this.system?.id) return;
    try {
      const body = JSON.parse(this.newSubmodelJson);
      await this.aasManagement.createSubmodel(this.system.id, body);
      this.showSubmodelDialog = false;
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

  /**
   * Opens the dialog for creating a new AAS element under the specified submodel or parent path.
   * @param submodelId The ID of the target submodel.
   * @param parentPath Optional parent path for nested creation.
   */
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

  /**
   * Handles the result of the element creation dialog.
   * Displays success or error messages and triggers UI updates accordingly.
   * @param result Result object returned from the element dialog.
   */
  onElementDialogResult(result: AasElementDialogResult): void {
    if (result.success && result.element) {
      this.handleElementCreation(result.element);
    } else if (result.error) {
      console.error('[TargetAasManage] Element creation failed:', result.error);
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

  /**
   * Creates a new element in the AAS via the management service and updates the UI tree structure.
   * Handles duplicate element errors and refreshes data after creation.
   * @param elementData The element data payload to create.
   */
  private async handleElementCreation(elementData: any): Promise<void> {
    if (!this.system?.id) return;
    try {
      const encodedParentPath = elementData.parentPath ? 
        elementData.parentPath.replace(/\//g, '.') : 
        undefined;
      await this.aasManagement.createElement(
        this.system.id,
        elementData.submodelId,
        elementData.body,
        encodedParentPath
      );
      this.messageService.add({
        key: 'targetAAS',
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
      await this.discoverSnapshot();
      this.fixTreeStructureAfterRefresh(elementData);
      setTimeout(async () => {
        await this.discoverSnapshot();
        this.fixTreeStructureAfterRefresh(elementData);
      }, 1000);
    } catch (error) {
      console.error('[TargetAasManage] Error creating element:', error);
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

  /**
   * Deletes an AAS submodel and refreshes the tree structure.
   * Displays a success or error message based on the backend response.
   * @param submodelId ID of the submodel to delete.
   */
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

  /**
   * Deletes a specific AAS element and refreshes the AAS tree.
   * Displays confirmation messages after deletion.
   * @param submodelId ID of the submodel containing the element.
   * @param elementPath The element path identifying the item to delete.
   */
  async deleteElement(submodelId: string, elementPath: string): Promise<void> {
    if (!this.system?.id) return;
    try {
      await this.aasManagement.deleteElement(this.system.id, submodelId, elementPath);
      this.discoverSnapshot();
      this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Element Deleted', detail: 'Element has been successfully deleted.', life: 3000 });
    } catch (error: any) {
      console.error('Error deleting element:', error);
      this.messageService.add({ key: 'targetAAS', severity: 'error', summary: 'Error', detail: ((error as any)?.message || 'Failed to delete element') });
    }
  }

  openSetValue(submodelId: string, elementData: any): void {
    // TODO: Implement set value dialog
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

  /** Opens the dialog for uploading or attaching an AASX file. */
  openAasxUpload(): void {
    this.showAasxUpload = true;
    this.aasxSelectedFile = null;
  }

  /**
   * Handles AASX file selection and loads a preview of its content.
   * @param event File upload event from the file selector.
   */
  onAasxFileSelected(event: any): void {
    this.aasxSelectedFile = event.files?.[0] || null;
    if (this.aasxSelectedFile && this.system?.id) {
      this.aasManagement.previewAasx(this.system.id, this.aasxSelectedFile).then((resp: any) => {
        this.aasxPreview = resp?.submodels || (resp?.result ?? []);
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


  /**
   * Uploads or attaches an AASX file to the backend for integration with the AAS.
   * Displays notifications for upload progress and completion.
   */
  uploadAasx(): void {
    if (this.isUploadingAasx) return;
    if (!this.aasxSelectedFile || !this.system?.id) {
      this.messageService.add({ key: 'targetAAS', severity: 'warn', summary: 'No file selected', detail: 'Please choose an .aasx file.' });
      return;
    }
    this.messageService.add({ key: 'targetAAS', severity: 'info', summary: 'Uploading AASX', detail: `${this.aasxSelectedFile?.name} (${this.aasxSelectedFile?.size} bytes)` });
    this.isUploadingAasx = true;
    const hasSelection = (this.aasxSelection?.submodels?.some(s => s.full) ?? false);
    const req$ = hasSelection ? 
      this.aasManagement.attachSelectedAasx(this.system.id, this.aasxSelectedFile, this.aasxSelection) : 
      this.aasManagement.uploadAasx(this.system.id, this.aasxSelectedFile);
    req$.then(() => {
      this.isUploadingAasx = false;
      this.showAasxUpload = false;
      this.discoverSnapshot();
      this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Upload accepted', detail: 'AASX uploaded successfully.' });
    }).catch((error: any) => {
      this.isUploadingAasx = false;
      this.messageService.add({ key: 'targetAAS', severity: 'error', summary: 'Upload failed', detail: error?.message || 'See console for details' });
    });
  }
}
