import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { CheckboxModule } from 'primeng/checkbox';
import { TreeModule } from 'primeng/tree';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TreeNode } from 'primeng/api';
import { AasClientService, AasSystemType } from '../../features/source-system/services/aas-client.service';

@Component({
  standalone: true,
  selector: 'app-aas-panel',
  imports: [CommonModule, FormsModule, ButtonModule, FileUploadModule, TableModule, DialogModule, CheckboxModule, TreeModule, InputTextModule, TextareaModule],
  template: `
  <div class="p-d-flex p-ai-center p-jc-between" style="margin-bottom: .5rem; gap: .75rem; flex-wrap: wrap;">
    <div>
      <strong>AAS</strong>
      <span class="ml-2">Type: {{ systemType }}</span>
      <span class="ml-2">System ID: {{ systemId }}</span>
    </div>
    <div class="p-d-flex p-ai-center" style="gap: .5rem;">
      <button pButton type="button" label="Test" (click)="onTest()" [disabled]="loading"></button>
      <span *ngIf="testMessage" [style.color]="testOk ? '#2e7d32' : '#d32f2f'">{{ testMessage }}</span>
    </div>
  </div>

  <div class="p-d-flex p-ai-center" style="gap: .75rem; flex-wrap: wrap; margin-bottom: .5rem;">
    <p-fileupload mode="basic" [auto]="false" [showUploadButton]="false" [showCancelButton]="false" accept=".aasx,.json" (onSelect)="onFileSelected($event)"></p-fileupload>
    <button pButton type="button" label="Preview AASX" [disabled]="!selectedFile" (click)="onPreview()"></button>
    <button pButton type="button" label="Attach Selected" [disabled]="!selectedFile || !hasAnySelection()" (click)="onAttachSelected()"></button>
    <button pButton type="button" class="p-button-text ml-2" label="Discover Submodels" (click)="onDiscover()" [disabled]="discovering"></button>
    <button pButton type="button" class="p-button-text ml-2" label="+ Submodel" (click)="openCreateSubmodel()"></button>
  </div>

  <p-table *ngIf="preview?.submodels?.length" [value]="preview.submodels">
    <ng-template pTemplate="header">
      <tr>
        <th style="width: 3rem;"></th>
        <th>Submodel</th>
        <th>Elements</th>
      </tr>
    </ng-template>
    <ng-template pTemplate="body" let-row let-i="rowIndex">
      <tr>
        <td>
          <p-checkbox [binary]="true" [ngModel]="(selectionMap[row.id] && selectionMap[row.id].full) || false" (ngModelChange)="onToggleFullChange(row, $event)"></p-checkbox>
        </td>
        <td>
          <div>{{ row.idShort || row.id }}</div>
          <small>{{ row.kind || '-' }} • {{ row.from }}</small>
        </td>
        <td>
          <div class="p-d-flex p-ai-center" style="gap: .5rem; flex-wrap: wrap;">
            <ng-container *ngFor="let el of row.elements">
              <p-checkbox [binary]="true" [ngModel]="(selectionMap[row.id] && selectionMap[row.id].elementsMap && selectionMap[row.id].elementsMap[el.idShort]) || false" (ngModelChange)="onToggleElementChange(row, el, $event)"></p-checkbox>
              <span>{{ el.idShort }} ({{ el.modelType }})</span>
            </ng-container>
            <span *ngIf="!row.elements?.length">-</span>
          </div>
        </td>
      </tr>
    </ng-template>
  </p-table>

  <div style="display:flex; gap:1rem; align-items:flex-start; margin-top: .75rem;">
    <div style="flex: 1 1 65%; min-width: 0;">
      <p-tree [value]="treeNodes" (onNodeExpand)="onNodeExpand($event)" (onNodeSelect)="onNodeSelect($event)" selectionMode="single">
        <ng-template let-node pTemplate="default">
          <div style="display:flex;align-items:center;gap:.5rem;">
            <span>{{ node.label }}</span>
            <span style="font-size:.75rem;padding:.1rem .4rem;border-radius:999px;border:1px solid var(--surface-border);color:var(--text-color-secondary);">
              {{ node.data?.type==='submodel' ? (node.data?.modelType || (node.data?.raw?.kind?.toLowerCase?.().includes('template') ? 'Submodel Template' : 'Submodel')) : (node.data?.modelType || node.data?.raw?.modelType || (node.data?.raw?.valueType ? 'Property' : 'Element')) }}
            </span>
            <button *ngIf="node.data?.type==='submodel'" pButton type="button" class="p-button-text" label="Create element" (click)="openCreateElement(node.data.id)"></button>
            <button *ngIf="node.data?.type==='submodel'" pButton type="button" class="p-button-text p-button-danger" label="Delete submodel" (click)="deleteSubmodel(node.data.id)"></button>
            <button *ngIf="node.data?.type==='element' && (node.data?.modelType==='SubmodelElementCollection' || node.data?.modelType==='SubmodelElementList' || node.data?.modelType==='Entity')" pButton type="button" class="p-button-text" label="Add child" (click)="openCreateElement(node.data.submodelId, node.data.idShortPath)"></button>
            <button *ngIf="node.data?.type==='element' && (node.data?.modelType==='Property' || node.data?.raw?.valueType)" pButton type="button" class="p-button-text" label="Set value" (click)="openSetValue(node.data.submodelId, node.data)"></button>
            <button *ngIf="node.data?.type==='element'" pButton type="button" class="p-button-text p-button-danger" label="Delete" (click)="deleteElement(node.data.submodelId, node.data.idShortPath)"></button>
          </div>
        </ng-template>
      </p-tree>
    </div>
    <div id="aas-element-details" class="p-card" *ngIf="selectedNode && selectedNode.data?.type==='element'" style="flex: 0 0 35%; padding:1rem;border:1px solid var(--surface-border);border-radius:4px; position: sticky; top: .5rem; align-self: flex-start; max-height: calc(100vh - 1rem); overflow: auto;">
      <div class="p-d-flex p-ai-center p-jc-between">
        <h4 style="margin:0;">Details</h4>
        <span *ngIf="selectedLiveLoading">Loading...</span>
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

  <!-- Dialogs -->
  <p-dialog header="Create Submodel" [(visible)]="showSubmodelDialog" [modal]="true" [style]="{width:'40vw'}">
    <textarea pInputTextarea [(ngModel)]="newSubmodelJson" rows="10" style="width:100%"></textarea>
    <ng-template pTemplate="footer">
      <button pButton type="button" class="p-button-text" label="Cancel" (click)="showSubmodelDialog=false"></button>
      <button pButton type="button" label="Create" (click)="createSubmodel()"></button>
    </ng-template>
    <div class="p-mt-2" style="display:flex;gap:.5rem;flex-wrap:wrap;">
      <button pButton type="button" class="p-button-text" label="Template: Minimal" (click)="setSubmodelTemplate('minimal')"></button>
      <button pButton type="button" class="p-button-text" label="Template: With Property" (click)="setSubmodelTemplate('property')"></button>
      <button pButton type="button" class="p-button-text" label="Template: With Collection" (click)="setSubmodelTemplate('collection')"></button>
    </div>
  </p-dialog>

  <p-dialog header="Create Element" [(visible)]="showElementDialog" [modal]="true" [style]="{width:'40vw'}">
    <div class="p-field">
      <label>Submodel ID</label>
      <input pInputText [value]="targetSubmodelId" disabled />
    </div>
    <div class="p-field">
      <label>Parent Path</label>
      <input pInputText [(ngModel)]="parentPath" placeholder="e.g. address or address/street" />
      <small>Optional. Use idShort path with slashes for nesting.</small>
    </div>
    <div class="p-field">
      <label>Import JSON</label>
      <p-fileupload mode="basic" [auto]="false" [showUploadButton]="false" [showCancelButton]="false" accept=".json" (onSelect)="onElementJsonFileSelected($event)"></p-fileupload>
    </div>
    <textarea pInputTextarea [(ngModel)]="newElementJson" rows="10" style="width:100%"></textarea>
    <ng-template pTemplate="footer">
      <button pButton type="button" class="p-button-text" label="Cancel" (click)="showElementDialog=false"></button>
      <button pButton type="button" label="Create" (click)="createElement()"></button>
    </ng-template>
    <div class="p-mt-2" style="display:flex;gap:.5rem;flex-wrap:wrap;">
      <button pButton type="button" class="p-button-text" label="Template: Property" (click)="setElementTemplate('property')"></button>
      <button pButton type="button" class="p-button-text" label="Template: Range" (click)="setElementTemplate('range')"></button>
      <button pButton type="button" class="p-button-text" label="Template: MultiLanguageProperty" (click)="setElementTemplate('mlp')"></button>
      <button pButton type="button" class="p-button-text" label="Template: ReferenceElement" (click)="setElementTemplate('ref')"></button>
      <button pButton type="button" class="p-button-text" label="Template: RelationshipElement" (click)="setElementTemplate('rel')"></button>
      <button pButton type="button" class="p-button-text" label="Template: AnnotatedRelationshipElement" (click)="setElementTemplate('annrel')"></button>
      <button pButton type="button" class="p-button-text" label="Template: Collection" (click)="setElementTemplate('collection')"></button>
      <button pButton type="button" class="p-button-text" label="Template: List" (click)="setElementTemplate('list')"></button>
      <button pButton type="button" class="p-button-text" label="Template: File" (click)="setElementTemplate('file')"></button>
      <button pButton type="button" class="p-button-text" label="Template: Operation" (click)="setElementTemplate('operation')"></button>
      <button pButton type="button" class="p-button-text" label="Template: Entity" (click)="setElementTemplate('entity')"></button>
    </div>
  </p-dialog>

  <p-dialog header="Set Property Value" [(visible)]="showValueDialog" [modal]="true" [style]="{width:'35vw'}">
    <div class="p-field">
      <label>Submodel ID</label>
      <input pInputText [value]="valueSubmodelId" disabled />
    </div>
    <div class="p-field">
      <label>Element Path</label>
      <input pInputText [value]="valueElementPath" disabled />
    </div>
    <div class="p-field">
      <label>New Value</label>
      <input pInputText [(ngModel)]="valueNew" />
      <small>valueType: {{ valueTypeHint }}</small>
    </div>
    <ng-template pTemplate="footer">
      <button pButton type="button" class="p-button-text" label="Cancel" (click)="showValueDialog=false"></button>
      <button pButton type="button" label="Save" (click)="setValue()"></button>
    </ng-template>
  </p-dialog>
  `
})
export class AasPanelComponent {
  @Input() systemType: AasSystemType = 'source';
  @Input() systemId!: number;

  loading = false;
  testMessage = '';
  testOk = false;
  selectedFile: File | null = null;
  preview: any = null;

  selectionMap: Record<string, { full: boolean; elementsMap: Record<string, boolean> }> = {};

  // Tree state
  discovering = false;
  treeNodes: TreeNode[] = [];
  selectedNode?: TreeNode;
  selectedLivePanel: any = null;
  selectedLiveLoading = false;

  // Create dialogs
  showSubmodelDialog = false;
  newSubmodelJson = '{\n  "id": "https://example.com/ids/sm/new",\n  "idShort": "NewSubmodel"\n}';
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

  showElementDialog = false;
  targetSubmodelId = '';
  parentPath = '';
  newElementJson = '{\n  "modelType": "Property",\n  "idShort": "NewProp",\n  "valueType": "xs:string",\n  "value": "42"\n}';
  elementTemplateProperty: string = `{
  "modelType": "Property",
  "idShort": "NewProp",
  "valueType": "xs:string",
  "value": "Foo"
}`;
  elementTemplateRange: string = `{
  "modelType": "Range",
  "idShort": "NewRange",
  "valueType": "xs:double",
  "min": 0,
  "max": 100
}`;
  elementTemplateMLP: string = `{
  "modelType": "MultiLanguageProperty",
  "idShort": "Title",
  "value": [ { "language": "en", "text": "Example" } ]
}`;
  elementTemplateRef: string = `{
  "modelType": "ReferenceElement",
  "idShort": "Ref",
  "value": { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm" } ] }
}`;
  elementTemplateRel: string = `{
  "modelType": "RelationshipElement",
  "idShort": "Rel",
  "first":  { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm1" } ] },
  "second": { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm2" } ] }
}`;
  elementTemplateAnnRel: string = `{
  "modelType": "AnnotatedRelationshipElement",
  "idShort": "AnnRel",
  "first":  { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm1" } ] },
  "second": { "type": "ModelReference", "keys": [ { "type": "Submodel", "value": "https://example.com/ids/sm2" } ] },
  "annotations": [ { "modelType": "Property", "idShort": "note", "valueType": "xs:string", "value": "Hello" } ]
}`;
  elementTemplateCollection: string = `{
  "modelType": "SubmodelElementCollection",
  "idShort": "group",
  "value": []
}`;
  elementTemplateList: string = `{
  "modelType": "SubmodelElementList",
  "idShort": "items",
  "typeValueListElement": "Property",
  "valueTypeListElement": "xs:string",
  "value": []
}`;
  elementTemplateFile: string = `{
  "modelType": "File",
  "idShort": "file1",
  "contentType": "text/plain",
  "value": "path-or-url.txt"
}`;
  elementTemplateOperation: string = `{
  "modelType": "Operation",
  "idShort": "Op",
  "inputVariables": [ { "value": { "modelType": "Property", "idShort": "in", "valueType": "xs:string" } } ],
  "outputVariables": []
}`;
  elementTemplateEntity: string = `{
  "modelType": "Entity",
  "idShort": "Ent",
  "entityType": "SelfManagedEntity",
  "statements": []
}`;

  constructor(private aas: AasClientService) {}

  onTest(): void {
    this.loading = true;
    this.testMessage = '';
    this.aas.test(this.systemType, this.systemId).subscribe({
      next: () => { this.testOk = true; this.testMessage = 'OK'; this.loading = false; },
      error: (err) => { this.testOk = false; this.testMessage = err?.error || 'Error'; this.loading = false; }
    });
  }

  onFileSelected(evt: any): void {
    const files: File[] = evt?.files || [];
    this.selectedFile = files.length ? files[0] : null;
  }

  onPreview(): void {
    if (!this.selectedFile) return;
    this.aas.previewAasx(this.systemType, this.systemId, this.selectedFile).subscribe({
      next: (res) => {
        this.preview = typeof res === 'string' ? JSON.parse(res) : res;
        this.buildSelectionMap();
      },
      error: () => { this.preview = null; }
    });
  }

  onAttachSelected(): void {
    if (!this.selectedFile) return;
    const selection = this.buildSelectionPayload();
    this.aas.attachSelectedAasx(this.systemType, this.systemId, this.selectedFile, selection).subscribe({
      next: () => {
        // After attach, show latest LIVE structure immediately
        this.onDiscover('LIVE');
      },
      error: () => {}
    });
  }

  hasAnySelection(): boolean {
    return Object.values(this.selectionMap).some(s => s.full || Object.values(s.elementsMap).some(Boolean));
  }

  onToggleFullChange(row: any, checked: boolean): void {
    if (!this.selectionMap[row.id]) this.selectionMap[row.id] = { full: false, elementsMap: {} };
    this.selectionMap[row.id].full = !!checked;
    if (this.selectionMap[row.id].full) this.selectionMap[row.id].elementsMap = {};
  }

  onToggleElementChange(row: any, el: any, checked: boolean): void {
    if (!this.selectionMap[row.id]) this.selectionMap[row.id] = { full: false, elementsMap: {} };
    this.selectionMap[row.id].elementsMap[el.idShort] = !!checked;
    this.selectionMap[row.id].full = false;
  }

  private buildSelectionMap(): void {
    this.selectionMap = {};
    const subs: any[] = this.preview?.submodels || [];
    for (const sm of subs) {
      this.selectionMap[sm.id] = { full: false, elementsMap: {} };
    }
  }

  private buildSelectionPayload(): any {
    const subs: any[] = this.preview?.submodels || [];
    const out: any[] = [];
    for (const sm of subs) {
      const sel = this.selectionMap[sm.id];
      if (!sel) continue;
      if (sel.full) {
        out.push({ id: sm.id, full: true });
      } else {
        const elements = Object.entries(sel.elementsMap)
          .filter(([, v]) => !!v)
          .map(([k]) => k);
        if (elements.length) out.push({ id: sm.id, elements });
      }
    }
    return { submodels: out };
  }

  // Discovery and tree handling
  onDiscover(source: 'SNAPSHOT'|'LIVE' = (this.systemType === 'source' ? 'SNAPSHOT' : 'LIVE')): void {
    if (!this.systemId) return;
    this.discovering = true;
    const params: any = (this.systemType === 'source' && source === 'SNAPSHOT') ? { source } : {};
    this.aas.listSubmodels(this.systemType, this.systemId, params).subscribe({
      next: (resp) => {
        const submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
        this.treeNodes = submodels.map((sm: any) => this.mapSmToNode(sm));
        this.discovering = false;
      },
      error: () => { this.discovering = false; }
    });
  }

  onNodeExpand(event: any): void {
    const node: TreeNode = event.node;
    if (!node) return;
    if (node.data?.type === 'submodel') {
      this.loadChildren(node.data.id, undefined, node);
    } else if (node.data?.type === 'element') {
      this.loadChildren(node.data.submodelId, node.data.idShortPath, node);
    }
  }

  onNodeSelect(event: any): void {
    const node: TreeNode = event.node;
    this.selectedNode = node;
    this.selectedLivePanel = null;
    if (!node || node.data?.type !== 'element') return;
    const smId: string = node.data.submodelId;
    const idShortPath: string = node.data.idShortPath;
    this.loadLiveElementDetails(smId, idShortPath, node);
    setTimeout(() => {
      const el = document.getElementById('aas-element-details');
      if (el && el.scrollIntoView) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 0);
  }

  private loadChildren(submodelId: string, parentPath: string | undefined, attach: TreeNode): void {
    if (!this.systemId || !submodelId) return;
    const smIdEnc = this.encodeIdToBase64Url(submodelId);
    const params: any = { depth: 'shallow' as const };
    if (parentPath) params.parentPath = parentPath;
    if (this.systemType === 'source') params.source = 'SNAPSHOT';
    this.aas.listElements(this.systemType, this.systemId, smIdEnc, params.depth, params.parentPath, params.source).subscribe({
      next: (resp) => {
        const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
        attach.children = list.map((el: any) => this.mapElToNode(submodelId, el));
        this.treeNodes = [...this.treeNodes];
      },
      error: () => {}
    });
  }

  private loadLiveElementDetails(smId: string, idShortPath: string | undefined, node?: TreeNode): void {
    const systemId = this.systemId;
    if (!systemId) return;
    this.selectedLiveLoading = true;
    const smIdEnc = this.encodeIdToBase64Url(smId);
    const sourceParam = (this.systemType === 'source') ? 'LIVE' : undefined;
    this.aas.getElement(this.systemType, systemId, smIdEnc, idShortPath || '', sourceParam as any).subscribe({
      next: (found: any) => {
        this.selectedLiveLoading = false;
        const liveType = found?.modelType || (found?.valueType ? 'Property' : undefined);
        const minValue = (found as any).min ?? (found as any).minValue;
        const maxValue = (found as any).max ?? (found as any).maxValue;
        const inputVars = Array.isArray((found as any).inputVariables) ? (found as any).inputVariables : [];
        const outputVars = Array.isArray((found as any).outputVariables) ? (found as any).outputVariables : [];
        const inoutVars = Array.isArray((found as any).inoutputVariables) ? (found as any).inoutputVariables : [];
        const ann1 = (found as any).annotations;
        const ann2 = (found as any).annotation;
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
        const stringifyRef = (ref: any): string | undefined => {
          if (!ref) return undefined;
          const keys = ref?.keys;
          if (Array.isArray(keys) && keys.length) {
            try { return keys.map((k: any) => `${k?.type ?? ''}:${k?.value ?? ''}`).join(' / '); } catch { return JSON.stringify(ref); }
          }
          if (typeof ref === 'string') return ref;
          if (ref?.value) return String(ref.value);
          try { return JSON.stringify(ref); } catch { return String(ref); }
        };
        const firstRef = stringifyRef((found as any).first || (found as any).firstReference);
        const secondRef = stringifyRef((found as any).second || (found as any).secondReference);
        this.selectedLivePanel = {
          label: found.idShort,
          type: liveType || 'Unknown',
          value: (found as any).value,
          valueType: (found as any).valueType,
          min: minValue,
          max: maxValue,
          inputVariables: inputVars.map(mapVar).filter(Boolean),
          outputVariables: outputVars.map(mapVar).filter(Boolean),
          inoutputVariables: inoutVars.map(mapVar).filter(Boolean),
          firstRef,
          secondRef,
          annotations: annotationsRaw.map(mapAnnotation).filter(Boolean)
        } as any;
        if (node && node.data) {
          node.data.idShortPath = idShortPath || node.data.idShortPath;
          node.data.modelType = liveType || node.data.modelType;
          node.data.raw = { ...(node.data.raw || {}), idShortPath: (idShortPath || node.data.idShortPath), modelType: found.modelType, valueType: found.valueType };
          this.treeNodes = [...this.treeNodes];
        }
      },
      error: () => { this.selectedLiveLoading = false; }
    });
  }

  openCreateSubmodel(): void { this.showSubmodelDialog = true; }
  setSubmodelTemplate(kind: 'minimal'|'property'|'collection'): void {
    if (kind === 'minimal') this.newSubmodelJson = this.minimalSubmodelTemplate;
    if (kind === 'property') this.newSubmodelJson = this.propertySubmodelTemplate;
    if (kind === 'collection') this.newSubmodelJson = this.collectionSubmodelTemplate;
  }
  createSubmodel(): void {
    if (!this.systemId) return;
    try {
      const body = JSON.parse(this.newSubmodelJson);
      this.aas.createSubmodel(this.systemType, this.systemId, body).subscribe({
        next: () => { this.showSubmodelDialog = false; this.onDiscover(this.systemType === 'source' ? 'SNAPSHOT' : 'LIVE'); },
        error: () => {}
      });
    } catch {}
  }

  setElementTemplate(kind: string): void {
    switch (kind) {
      case 'property': this.newElementJson = this.elementTemplateProperty; break;
      case 'range': this.newElementJson = this.elementTemplateRange; break;
      case 'mlp': this.newElementJson = this.elementTemplateMLP; break;
      case 'ref': this.newElementJson = this.elementTemplateRef; break;
      case 'rel': this.newElementJson = this.elementTemplateRel; break;
      case 'annrel': this.newElementJson = this.elementTemplateAnnRel; break;
      case 'collection': this.newElementJson = this.elementTemplateCollection; break;
      case 'list': this.newElementJson = this.elementTemplateList; break;
      case 'file': this.newElementJson = this.elementTemplateFile; break;
      case 'operation': this.newElementJson = this.elementTemplateOperation; break;
      case 'entity': this.newElementJson = this.elementTemplateEntity; break;
      default: this.newElementJson = '{}';
    }
  }
  openCreateElement(smId: string, parent?: string): void {
    this.targetSubmodelId = smId;
    this.parentPath = parent || '';
    this.showElementDialog = true;
  }
  onElementJsonFileSelected(event: any): void {
    const file = event.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = String(reader.result || '').trim();
        if (text) { JSON.parse(text); this.newElementJson = text; }
      } catch {}
    };
    reader.readAsText(file);
  }
  createElement(): void {
    if (!this.systemId || !this.targetSubmodelId) return;
    try {
      const body = JSON.parse(this.newElementJson);
      const smIdB64 = this.encodeIdToBase64Url(this.targetSubmodelId);
      this.aas.createElement(this.systemType, this.systemId, smIdB64, body, this.parentPath || undefined)
        .subscribe({
          next: () => {
            this.showElementDialog = false;
            // refresh affected parent from LIVE
            this.refreshNodeLive(this.targetSubmodelId, this.parentPath, undefined);
          },
          error: () => {}
        });
    } catch {}
  }

  openSetValue(smId: string, element: any): void {
    this.valueSubmodelId = smId;
    this.valueElementPath = element.idShortPath || element.data?.idShortPath || element.raw?.idShortPath || element.idShort;
    this.valueTypeHint = element.valueType || 'xs:string';
    if (this.selectedLivePanel && this.selectedNode && this.selectedNode.data?.idShortPath === this.valueElementPath) {
      this.valueNew = (this.selectedLivePanel.value ?? '').toString();
    } else {
      this.valueNew = '';
    }
    this.showValueDialog = true;
  }
  setValue(): void {
    if (!this.systemId || !this.valueSubmodelId || !this.valueElementPath) return;
    const smIdB64 = this.encodeIdToBase64Url(this.valueSubmodelId);
    const parsedValue = this.parseValueForType(this.valueNew, this.valueTypeHint);
    this.aas.patchElementValue(this.systemType, this.systemId, smIdB64, this.valueElementPath, parsedValue as any)
      .subscribe({
        next: () => { this.showValueDialog = false; this.refreshNodeLive(this.valueSubmodelId, this.valueElementPath.includes('/') ? this.valueElementPath.substring(0, this.valueElementPath.lastIndexOf('/')) : '', undefined); },
        error: () => {}
      });
  }

  deleteSubmodel(submodelId: string): void {
    if (!this.systemId || !submodelId) return;
    const smIdB64 = this.encodeIdToBase64Url(submodelId);
    this.aas.deleteSubmodel(this.systemType, this.systemId, smIdB64).subscribe({
      next: () => { this.onDiscover(this.systemType === 'source' ? 'SNAPSHOT' : 'LIVE'); },
      error: () => {}
    });
  }
  deleteElement(submodelId: string, idShortPath: string): void {
    if (!this.systemId || !submodelId || !idShortPath) return;
    const smIdB64 = this.encodeIdToBase64Url(submodelId);
    this.aas.deleteElement(this.systemType, this.systemId, smIdB64, idShortPath).subscribe({
      next: () => { const parent = idShortPath.includes('/') ? idShortPath.substring(0, idShortPath.lastIndexOf('/')) : ''; this.refreshNodeLive(submodelId, parent, undefined); },
      error: () => {}
    });
  }

  private refreshNodeLive(submodelId: string, parentPath: string, node?: TreeNode): void {
    if (!this.systemId) return;
    const smIdB64 = this.encodeIdToBase64Url(submodelId);
    this.aas.listElements(this.systemType, this.systemId, smIdB64, 'shallow', parentPath || undefined, (this.systemType === 'source' ? 'LIVE' : undefined) as any)
      .subscribe({
        next: (resp) => {
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const mapped = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElToNode(submodelId, el);
          });
          if (node) { node.children = mapped; this.treeNodes = [...this.treeNodes]; }
          else {
            const attachNode = this.findNodeByKey(submodelId, this.treeNodes);
            if (attachNode) { attachNode.children = mapped; this.treeNodes = [...this.treeNodes]; }
          }
        },
        error: () => {}
      });
  }

  private findNodeByKey(key: string, nodes: TreeNode[] | undefined): TreeNode | null {
    if (!nodes) return null;
    for (const n of nodes) {
      if ((n.key as string) === key) return n;
      const found = this.findNodeByKey(key, n.children as TreeNode[]);
      if (found) return found;
    }
    return null;
  }

  private mapSmToNode(sm: any): TreeNode {
    const id = sm.submodelId || sm.id || (sm.keys && sm.keys[0]?.value);
    const label = (sm.submodelIdShort || sm.idShort) || id;
    const kindRaw = (sm.kind || sm.submodelKind || '').toString();
    const isTemplate = kindRaw && kindRaw.toLowerCase().includes('template');
    const modelType = isTemplate ? 'Submodel Template' : 'Submodel';
    return { key: id, label, data: { type: 'submodel', id, modelType, raw: sm }, leaf: false, children: [] } as TreeNode;
  }
  private mapElToNode(submodelId: string, el: any): TreeNode {
    const computedType = this.inferModelType(el);
    const label = el.idShort;
    const typeHasChildren = el?.modelType === 'SubmodelElementCollection' || el?.modelType === 'SubmodelElementList' || el?.modelType === 'Operation' || el?.modelType === 'Entity';
    const hasChildren = el?.hasChildren === true || typeHasChildren;
    return { key: `${submodelId}::${el.idShortPath || el.idShort}`, label, data: { type: 'element', submodelId, idShortPath: el.idShortPath || el.idShort, modelType: computedType, raw: el }, leaf: !hasChildren, children: [] } as TreeNode;
  }
  private inferModelType(el: any): string | undefined {
    if (!el) return undefined;
    if (el.modelType) return el.modelType;
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

  // Value helpers
  showValueDialog = false;
  valueSubmodelId = '';
  valueElementPath = '';
  valueNew = '';
  valueTypeHint = 'xs:string';
  private parseValueForType(raw: string, valueType?: string): any {
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

  // Utilities
  private encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    try {
      const b64 = (window as any).btoa(unescape(encodeURIComponent(id)));
      return b64.replace(/=+$/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    } catch { return id; }
  }
}
