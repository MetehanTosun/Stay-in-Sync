import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TreeModule } from 'primeng/tree';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { TreeNode } from 'primeng/api';
import { MessageService } from 'primeng/api';
import { AasManagementService, AasElementLivePanel } from '../../services/aas-management.service';
import { AasUtilityService } from '../../services/aas-utility.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { AasElementDialogComponent, AasElementDialogData, AasElementDialogResult } from '../../../../shared/components/aas-element-dialog/aas-element-dialog.component';

@Component({
  standalone: true,
  selector: 'app-aas-management',
  template: `
    <!-- AAS Management Component for Target Systems -->
    <div class="p-d-flex p-ai-center p-jc-between" style="margin-bottom: .5rem;">
      <div>
        <strong>Base URL:</strong> {{ system?.apiUrl }}
        <span class="ml-3"><strong>AAS ID:</strong> {{ getAasId() }}</span>
      </div>
      <div>
        <button pButton type="button" label="Test Connection" class="p-button-text" (click)="testAasConnection()" [disabled]="aasTestLoading">
          <span *ngIf="aasTestLoading">Testing...</span>
          <span *ngIf="!aasTestLoading">Test Connection</span>
        </button>
        <button pButton type="button" label="Refresh Snapshot" class="p-button-text" (click)="discoverSnapshot()" [disabled]="isLoading"></button>
        <button pButton type="button" label="+ Submodel" class="p-button-text" (click)="openCreateSubmodel()"></button>
      </div>
    </div>

    <div *ngIf="aasTestError" class="p-message p-message-error" style="margin-bottom: 1rem;">
      <div class="p-message-wrapper">
        <span class="p-message-icon pi pi-exclamation-triangle"></span>
        <div class="p-message-text">{{ aasTestError }}</div>
      </div>
    </div>

    <button pButton type="button" label="Load Snapshot" (click)="discoverSnapshot()" [disabled]="isLoading"></button>
    <span *ngIf="isLoading" class="ml-2">Loading...</span>

    <div style="display:flex; gap:1rem; align-items:flex-start;">
      <div style="flex: 1 1 65%; min-width: 0;">
        <p-tree [value]="treeNodes" (onNodeExpand)="onNodeExpand($event)" (onNodeSelect)="onNodeSelect($event)" selectionMode="single" class="custom-tree">
          <ng-template let-node pTemplate="default">
            <div style="display:flex;align-items:center;gap:.5rem;">
              <i [class]="node.expanded ? 'pi pi-folder-open' : 'pi pi-folder'" style="color: var(--primary-color);"></i>
              <span>{{ node.label }}</span>
              <span style="font-size:.75rem;padding:.1rem .4rem;border-radius:999px;border:1px solid var(--surface-border);color:var(--text-color-secondary);">
                {{ getNodeType(node) }}
              </span>
              <button *ngIf="node.data?.type==='submodel'" pButton type="button" class="p-button-text" label="Create element" (click)="openCreateElement(node.data.id)"></button>
              <button *ngIf="node.data?.type==='element' && canAddChild(node)" pButton type="button" class="p-button-text" label="Add child" (click)="openCreateElement(node.data.submodelId, node.data.idShortPath)"></button>
              <button *ngIf="node.data?.type==='submodel'" pButton type="button" class="p-button-text p-button-danger" label="Delete submodel" (click)="deleteSubmodel(node.data.id)"></button>
              <button *ngIf="node.data?.type==='element'" pButton type="button" class="p-button-text p-button-danger" label="Delete" (click)="deleteElement(node.data.submodelId, node.data.idShortPath)"></button>
              <button *ngIf="node.data?.type==='element' && canSetValue(node)" pButton type="button" class="p-button-text" label="Set value" (click)="openSetValue(node.data.submodelId, node.data)"></button>
            </div>
          </ng-template>
        </p-tree>
      </div>
      <div id="aas-element-details" class="p-card" *ngIf="selectedNode && selectedNode.data?.type==='element'" style="flex: 0 0 35%; padding:1rem;border:1px solid var(--surface-border);border-radius:4px; position: sticky; top: .5rem; align-self: flex-start; max-height: calc(100vh - 1rem); overflow: auto;">
        <div class="p-d-flex p-ai-center p-jc-between">
          <h4 style="margin:0;">Details</h4>
          <span *ngIf="detailsLoading">Loading...</span>
        </div>
        <div *ngIf="selectedLivePanel">
          <div><strong>Label:</strong> {{ selectedLivePanel.label }}</div>
          <div><strong>Type:</strong> {{ selectedLivePanel.type }}</div>
          <div *ngIf="selectedLivePanel.valueType"><strong>valueType:</strong> {{ selectedLivePanel.valueType }}</div>
          <div *ngIf="selectedLivePanel.type==='MultiLanguageProperty' && (selectedLivePanel.value?.length || 0) > 0">
            <strong>values:</strong>
            <div style="margin:.25rem 0 .5rem 0;">
              <div *ngFor="let v of selectedLivePanel.value" style="display:flex;gap:.5rem;align-items:baseline;">
                <span style="font-size:.75rem;padding:.1rem .4rem;border:1px solid var(--surface-border);border-radius:999px;color:var(--text-color-secondary);min-width:2.5rem;text-align:center;">{{ v?.language || '-' }}</span>
                <span>{{ v?.text || '' }}</span>
              </div>
            </div>
          </div>
          <div *ngIf="selectedLivePanel.value !== undefined && selectedLivePanel.type!=='SubmodelElementCollection' && selectedLivePanel.type!=='SubmodelElementList' && selectedLivePanel.type!=='MultiLanguageProperty'">
            <strong>value:</strong> {{ (selectedLivePanel.value | json) }}
          </div>
          <div *ngIf="selectedLivePanel.type==='SubmodelElementCollection'">
            <div><strong>Items:</strong> {{ selectedLivePanel.value?.length || 0 }}</div>
          </div>
          <div *ngIf="selectedLivePanel.type==='SubmodelElementList'">
            <div><strong>Count:</strong> {{ selectedLivePanel.value?.length || 0 }}</div>
          </div>
          <div *ngIf="selectedLivePanel.type==='Range' || selectedLivePanel.min !== undefined || selectedLivePanel.max !== undefined">
            <div><strong>min:</strong> {{ selectedLivePanel.min }}</div>
            <div><strong>max:</strong> {{ selectedLivePanel.max }}</div>
          </div>
          <div *ngIf="selectedLivePanel.type==='Operation'">
            <div *ngIf="selectedLivePanel.inputVariables?.length">
              <strong>Inputs:</strong>
              <ul style="margin:.25rem 0 .5rem 1rem;">
                <li *ngFor="let v of selectedLivePanel.inputVariables">{{ v.idShort }} <span *ngIf="v.valueType">({{ v.valueType }})</span></li>
              </ul>
            </div>
            <div *ngIf="selectedLivePanel.inoutputVariables?.length">
              <strong>In/Out:</strong>
              <ul style="margin:.25rem 0 .5rem 1rem;">
                <li *ngFor="let v of selectedLivePanel.inoutputVariables">{{ v.idShort }} <span *ngIf="v.valueType">({{ v.valueType }})</span></li>
              </ul>
            </div>
            <div *ngIf="selectedLivePanel.outputVariables?.length">
              <strong>Outputs:</strong>
              <ul style="margin:.25rem 0 .5rem 1rem;">
                <li *ngFor="let v of selectedLivePanel.outputVariables">{{ v.idShort }} <span *ngIf="v.valueType">({{ v.valueType }})</span></li>
              </ul>
            </div>
          </div>
          <div *ngIf="selectedLivePanel.type==='RelationshipElement' || selectedLivePanel.type==='AnnotatedRelationshipElement'">
            <div *ngIf="selectedLivePanel.firstRef"><strong>First:</strong> {{ selectedLivePanel.firstRef }}</div>
            <div *ngIf="selectedLivePanel.secondRef"><strong>Second:</strong> {{ selectedLivePanel.secondRef }}</div>
            <div *ngIf="selectedLivePanel.type==='AnnotatedRelationshipElement' && selectedLivePanel.annotations?.length">
              <strong>Annotations:</strong>
              <ul style="margin:.25rem 0 .5rem 1rem;">
                <li *ngFor="let a of selectedLivePanel.annotations; let i = index">
                  <div><strong>{{ a.idShort || ('Annotation ' + (i+1)) }}</strong></div>
                  <div *ngIf="a.valueType"><em>valueType:</em> {{ a.valueType }}</div>
                  <div *ngIf="a.value !== undefined"><em>value:</em> {{ (a.value | json) }}</div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Element Creation Dialog -->
    <app-aas-element-dialog
      [(visible)]="showElementDialog"
      [data]="elementDialogData"
      (result)="onElementDialogResult($event)">
    </app-aas-element-dialog>
  `,
  imports: [
    CommonModule,
    CardModule,
    TreeModule,
    ButtonModule,
    TooltipModule,
    AasElementDialogComponent
  ],
  styles: [`
    .custom-tree ::ng-deep .p-tree-node-icon {
      display: none !important;
    }
  `]
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
    
    this.isLoading = true;
    try {
      this.treeNodes = await this.aasManagement.discoverSubmodels(this.system.id);
    } catch (error) {
      console.error('Error discovering submodels:', error);
    } finally {
      this.isLoading = false;
    }
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
    if (!this.system?.id || !node.data?.submodelId || !node.data?.idShortPath) return;
    
    this.detailsLoading = true;
    try {
      this.selectedLivePanel = await this.aasManagement.loadElementDetails(
        this.system.id,
        node.data.submodelId,
        node.data.idShortPath
      );
    } catch (error) {
      console.error('Error loading element details:', error);
    } finally {
      this.detailsLoading = false;
    }
  }

  getAasId(): string {
    return this.aasUtility.getAasId(this.system);
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
    // TODO: Implement create submodel dialog
    console.log('Create submodel clicked');
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
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
      
      // Refresh the tree with full discover and force tree rebuild
      console.log('[TargetAasManage] Triggering full snapshot refresh');
      await this.discoverSnapshot();
      
      // Force tree refresh after a short delay to ensure backend is updated
      setTimeout(async () => {
        console.log('[TargetAasManage] Force refreshing tree after element creation');
        await this.discoverSnapshot();
      }, 1000);
      
    } catch (error) {
      console.error('[TargetAasManage] Error creating element:', error);
      
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

  async deleteSubmodel(submodelId: string): Promise<void> {
    if (!this.system?.id) return;
    
    try {
      await this.aasManagement.deleteSubmodel(this.system.id, submodelId);
      this.refreshRequested.emit();
      await this.discoverSnapshot();
    } catch (error) {
      console.error('Error deleting submodel:', error);
    }
  }

  async deleteElement(submodelId: string, elementPath: string): Promise<void> {
    if (!this.system?.id) return;
    
    try {
      await this.aasManagement.deleteElement(this.system.id, submodelId, elementPath);
      
      // Trigger discover to refresh the entire tree (same as source system)
      console.log('[TargetAasManagement] deleteElement: Triggering discover to refresh tree');
      this.discoverSnapshot();
      
    } catch (error) {
      console.error('Error deleting element:', error);
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
}
