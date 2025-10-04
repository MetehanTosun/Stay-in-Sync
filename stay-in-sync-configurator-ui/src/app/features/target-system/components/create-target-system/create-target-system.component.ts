import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';
import { StepsModule } from 'primeng/steps';
import { FileUploadModule } from 'primeng/fileupload';
import { FormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { TreeModule } from 'primeng/tree';
import { ToastModule } from 'primeng/toast';
import { MessageService, TreeNode } from 'primeng/api';

import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { ManageTargetEndpointsComponent } from '../manage-target-endpoints/manage-target-endpoints.component';
import { ManageApiHeadersComponent } from '../../../source-system/components/manage-api-headers/manage-api-headers.component';
import { ApiHeaderResourceService } from '../../../source-system/service/apiHeaderResource.service';
import { ApiAuthType } from '../../../source-system/models/apiAuthType';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { AasClientService } from '../../../source-system/services/aas-client.service';
import { CreateTargetSystemFormService } from '../../services/create-target-system-form.service';
import { CreateTargetSystemAasService } from '../../services/create-target-system-aas.service';
import { CreateTargetSystemDialogService } from '../../services/create-target-system-dialog.service';
import { AasElementDialogComponent, AasElementDialogData, AasElementDialogResult } from '../../../../shared/components/aas-element-dialog/aas-element-dialog.component';

@Component({
  standalone: true,
  selector: 'app-create-target-system',
  templateUrl: './create-target-system.component.html',
  styleUrls: ['./create-target-system.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    ButtonModule,
    TextareaModule,
    StepsModule,
    FileUploadModule,
    ManageTargetEndpointsComponent,
    ManageApiHeadersComponent,
    TreeModule,
    FormsModule,
    CheckboxModule,
    ToastModule,
    AasElementDialogComponent
  ]
})
export class CreateTargetSystemComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() targetSystem: TargetSystemDTO | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() created = new EventEmitter<TargetSystemDTO>();

  steps = [
    { label: 'Metadaten' },
    { label: 'Api Header' },
    { label: 'Endpoints' },
  ];
  currentStep = 0;
  createdTargetSystemId!: number;

  form!: FormGroup;
  selectedFile: File | null = null;
  fileSelected = false;
  private isCreating = false;

  typeOptions = [
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' },
    { label: 'AAS', value: 'AAS' }
  ];
  authTypeOptions = [
    { label: 'Basic', value: ApiAuthType.Basic },
    { label: 'API Key', value: ApiAuthType.ApiKey }
  ];
  public readonly ApiAuthType = ApiAuthType;

  constructor(
    private fb: FormBuilder,
    private api: TargetSystemResourceService,
    private headersApi: ApiHeaderResourceService,
    protected errorService: HttpErrorService,
    private aasClient: AasClientService,
    private messageService: MessageService,
    private formService: CreateTargetSystemFormService,
    private aasService: CreateTargetSystemAasService,
    private dialogService: CreateTargetSystemDialogService
  ) {}

  ngOnInit(): void {
    this.form = this.formService.createForm();
    this.formService.setupFormSubscriptions(this.form);
    
    // Update steps when API type changes
    this.form.get('apiType')!.valueChanges.subscribe((apiType: string) => {
      this.steps = this.formService.getSteps(apiType);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['targetSystem'] && this.targetSystem) {
      this.form.patchValue({
        name: this.targetSystem.name,
        apiUrl: this.targetSystem.apiUrl,
        description: this.targetSystem.description,
        apiType: this.targetSystem.apiType
      });
      this.currentStep = 0;
    }
  }

  onFileSelected(event: any): void {
    this.selectedFile = event.files?.[0] ?? null;
    this.fileSelected = true;
    this.form.get('openApiSpec')!.disable();
  }

  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.formService.resetForm(this.form);
    this.selectedFile = null;
    this.fileSelected = false;
    this.currentStep = 0;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.isCreating || this.createdTargetSystemId) { return; }
    this.isCreating = true;

    const { base, hasFile } = this.formService.getFormDataForSubmission(this.form, this.selectedFile);

    const post = (payload: TargetSystemDTO) => {
      this.api.create(payload).subscribe({
        next: (resp) => {
          this.createdTargetSystemId = resp.id!;
          this.currentStep = 1;
          this.messageService.add({ severity: 'success', summary: 'Created', detail: 'Target system created', life: 2500 });
          console.log('[CreateTargetSystem] Emitting created event', { id: resp.id, name: resp.name });
          this.created.emit(resp);
          console.log('[CreateTargetSystem] Created event emitted successfully');
          this.isCreating = false;
        },
        error: (err) => {
          this.errorService.handleError(err);
          this.messageService.add({ severity: 'error', summary: 'Create failed', detail: err?.message || 'See console for details', life: 4000 });
          this.isCreating = false;
        }
      });
    };

    if (hasFile && this.selectedFile) {
      const reader = new FileReader();
      reader.onload = () => {
        const fileContent = reader.result as string;
        (base as any).openAPI = fileContent;
        post(base);
      };
      reader.readAsText(this.selectedFile);
    } else {
      post(base as TargetSystemDTO);
    }
  }

  goNext(): void {
    if (this.currentStep === 0) {
      if (this.createdTargetSystemId) { this.currentStep = 1; return; }
      this.save();
    } else if (this.currentStep === 1) {
      this.currentStep = 2;
    }
  }

  goBack(): void {
    if (this.currentStep > 0) this.currentStep -= 1;
  }

  // Helpers matching Source Create behaviour
  isAas(): boolean { return this.form.get('apiType')!.value === 'AAS'; }
  isRest(): boolean { return this.form.get('apiType')!.value === 'REST_OPENAPI'; }

  // AAS Test
  isTesting = false;
  aasTestOk: boolean | null = null;
  aasError: string | null = null;
  aasPreview: { idShort?: string; assetKind?: string } | null = null;
  async testAasConnection(): Promise<void> {
    if (this.isTesting) return;
    this.isTesting = true;
    
    if (!this.createdTargetSystemId) {
      if (this.isCreating) { return; }
      const { base } = this.formService.getFormDataForSubmission(this.form);
      this.isCreating = true;
      this.api.create(base).subscribe({
        next: (resp) => { 
          this.createdTargetSystemId = resp.id!; 
          this.isCreating = false; 
          this.isTesting = false; 
          this.testAasConnection(); 
        },
        error: (err) => { 
          this.isCreating = false; 
          this.isTesting = false; 
          this.errorService.handleError(err); 
        }
      });
      return;
    }
    
    this.aasError = null;
    this.aasPreview = null;
    this.aasTestOk = null;
    
    try {
      const result = await this.aasService.testConnection(this.createdTargetSystemId);
      this.isTesting = false;
      
      if (result.success) {
        if (result.data && result.data.idShort) {
          this.aasPreview = { idShort: result.data.idShort, assetKind: result.data.assetKind };
        }
        this.aasTestOk = true;
        this.messageService.add({ 
          severity: 'success', 
          summary: 'Connection successful', 
          detail: 'Shell reachable', 
          life: 3000 
        });
      } else {
        this.aasPreview = null;
        this.aasTestOk = false;
        this.aasError = result.error || 'Connection failed';
        this.messageService.add({ 
          severity: 'error', 
          summary: 'Connection failed', 
          detail: 'Please verify Base URL, AAS ID and auth.', 
          life: 5000 
        });
      }
    } catch (err) {
      this.isTesting = false;
      this.aasPreview = null;
      this.aasTestOk = false;
      this.aasError = 'Connection failed';
      this.errorService.handleError(err as any);
      this.messageService.add({ 
        severity: 'error', 
        summary: 'Connection failed', 
        detail: 'Please verify Base URL, AAS ID and auth.', 
        life: 5000 
      });
    }
  }

  canProceedFromStep1(): boolean {
    return this.formService.isFormValidForStep(this.form, 0, this.form.get('apiType')!.value, this.aasTestOk);
  }

  // AASX upload state & handlers (target)
  showAasxUpload = false;
  aasxSelectedFile: File | null = null;
  isUploadingAasx = false;
  aasxPreview: any = null;
  aasxSelection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> } = { submodels: [] };

  openAasxUpload(): void {
    this.showAasxUpload = true;
    this.aasxSelectedFile = null;
  }
  onAasxFileSelected(event: any): void {
    this.aasxSelectedFile = event.files?.[0] || null;
    if (!this.aasxSelectedFile || !this.createdTargetSystemId) {
      this.aasxPreview = null;
      this.aasxSelection = { submodels: [] };
      return;
    }
    this.aasClient.previewAasx('target', this.createdTargetSystemId, this.aasxSelectedFile).subscribe({
      next: (resp) => {
        const arr = (resp && (Array.isArray(resp.submodels) ? resp.submodels : (resp.result ?? []))) || [];
        this.aasxPreview = arr;
        this.aasxSelection = { submodels: arr.map((sm: any) => ({ id: sm.id || sm.submodelId, full: true, elements: (sm.elements || []).map((e: any) => e.idShort) })) };
        
        // Check for empty collections/lists and show toast
        const emptySubmodels = arr.filter((sm: any) => !sm.elements || sm.elements.length === 0);
        if (emptySubmodels.length > 0) {
          this.messageService.add({
            severity: 'info',
            summary: 'AASX Preview',
            detail: `${emptySubmodels.length} submodel(s) have no collections or lists available.`,
            life: 4000
          });
        }
      },
      error: () => { this.aasxPreview = null; this.aasxSelection = { submodels: [] }; }
    });
  }
  private getSmId(sm: any): string { return sm?.id || sm?.submodelId || ''; }
  getOrInitAasxSelFor(sm: any): { id: string; full: boolean; elements: string[] } {
    const id = this.getSmId(sm);
    let found = this.aasxSelection.submodels.find(s => s.id === id);
    if (!found) { found = { id, full: true, elements: [] }; this.aasxSelection.submodels.push(found); }
    return found;
  }
  toggleAasxSubmodelFull(sm: any, checked: boolean): void {
    const sel = this.getOrInitAasxSelFor(sm);
    sel.full = !!checked;
    if (sel.full) sel.elements = [];
  }
  isAasxElementSelected(sm: any, idShort: string): boolean {
    const sel = this.getOrInitAasxSelFor(sm);
    return sel.elements.includes(idShort);
  }
  toggleAasxElement(sm: any, idShort: string, checked: boolean): void {
    const sel = this.getOrInitAasxSelFor(sm);
    sel.full = false;
    const exists = sel.elements.includes(idShort);
    if (checked) { if (!exists) sel.elements.push(idShort); }
    else { if (exists) sel.elements = sel.elements.filter(x => x !== idShort); }
  }
  uploadAasx(): void {
    if (this.isUploadingAasx) return;
    if (!this.aasxSelectedFile || !this.createdTargetSystemId) return;
    this.isUploadingAasx = true;
    this.messageService.add({ severity: 'info', summary: 'Uploading AASX', detail: `${this.aasxSelectedFile?.name} (${this.aasxSelectedFile?.size} bytes)` });
    const hasSelection = this.aasxSelection?.submodels?.some(s => s.full || (s.elements && s.elements.length > 0));
    const selectedIds: string[] = hasSelection ? this.aasxSelection.submodels.map(s => s.id).filter(Boolean) : [];
    const req$ = hasSelection
      ? this.aasClient.attachSelectedAasx('target', this.createdTargetSystemId, this.aasxSelectedFile, this.aasxSelection)
      : this.aasClient.uploadAasx('target', this.createdTargetSystemId, this.aasxSelectedFile);
    req$.subscribe({
      next: () => {
        this.isUploadingAasx = false;
        this.showAasxUpload = false;
        // Reflect immediately: list LIVE and refresh roots for selected IDs
        this.onDiscover('LIVE', selectedIds);
        // Short retries in case upstream processing is async
        setTimeout(() => this.onDiscover('LIVE', selectedIds), 1200);
        setTimeout(() => this.onDiscover('LIVE', selectedIds), 2500);
        setTimeout(() => this.onDiscover('LIVE', selectedIds), 5000);
        this.messageService.add({ severity: 'success', summary: 'Upload accepted', detail: 'AASX uploaded and attached', life: 3000 });
      },
      error: (err) => {
        this.isUploadingAasx = false;
        const detail = err?.error || err?.message || 'See console for details';
        this.messageService.add({ severity: 'error', summary: 'Upload failed', detail });
      }
    });
  }

  // AAS tree/discovery & details (target)
  isDiscovering = false;
  treeNodes: TreeNode[] = [];
  submodels: any[] = [];
  selectedNode?: TreeNode;
  selectedLivePanel: any = null;
  selectedLiveLoading = false;

  discoverSubmodels(): void {
    // Ensure system exists before discovery (mirror Source behavior)
    if (!this.createdTargetSystemId) { if (!this.isCreating) { this.save(); } return; }
    this.isDiscovering = true;
    console.log('[TargetCreate] discoverSubmodels -> targetId=', this.createdTargetSystemId);
    this.aasClient.listSubmodels('target', this.createdTargetSystemId, {}).subscribe({
      next: (resp) => {
        this.isDiscovering = false;
        this.submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
        console.log('[TargetCreate] submodels resp=', resp, 'parsedCount=', this.submodels.length);
        this.treeNodes = this.submodels.map((sm: any) => this.mapSubmodelToNode(sm));
      },
      error: (_err) => { this.isDiscovering = false; console.error('[TargetCreate] discover error', _err); this.errorService.handleError(_err); }
    });
  }

  onDiscover(source: 'LIVE' = 'LIVE', refreshIds: string[] = []): void {
    if (!this.createdTargetSystemId) return;
    this.isDiscovering = true;
    const params = {}; // no source param for target
    this.aasClient.listSubmodels('target', this.createdTargetSystemId, params).subscribe({
      next: (resp) => {
        this.isDiscovering = false;
        this.submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
        this.treeNodes = this.submodels.map((sm: any) => this.mapSubmodelToNode(sm));
        for (const smId of refreshIds) {
          const node = this.findNodeByKey(smId, this.treeNodes);
          this.refreshNodeLive(smId, '', node || undefined);
        }
      },
      error: () => { this.isDiscovering = false; }
    });
  }
  // duplicate removed; see method above
  onNodeExpand(event: any): void {
    const node: TreeNode = event.node; if (!node) return; console.log('[TargetCreate] onNodeExpand node=', node);
    
    // Don't expand if node is a leaf (no children)
    if (node.leaf) {
      console.log('[TargetCreate] onNodeExpand - Node is leaf, skipping expansion');
      return;
    }
    
    if (node.data?.type === 'submodel') { this.loadRootElements(node.data.id, node); }
    else if (node.data?.type === 'element') { this.loadChildren(node.data.submodelId, node.data.idShortPath, node); }
  }
  onNodeSelect(event: any): void {
    const node: TreeNode = event.node; this.selectedNode = node; this.selectedLivePanel = null;
    if (!node || node.data?.type !== 'element') return;
    const smId: string = node.data.submodelId; const idShortPath: string = node.data.idShortPath;
    this.loadLiveElementDetails(smId, idShortPath, node);
    setTimeout(() => { const el = document.getElementById('element-details'); el?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }, 0);
  }
  private loadRootElements(submodelId: string, attach?: TreeNode): void {
    if (!this.createdTargetSystemId) return;
    const smIdEnc = this.encodeIdToBase64Url(submodelId); console.log('[TargetCreate] loadRootElements smIdRaw=', submodelId, 'smIdB64=', smIdEnc);
    // Use shallow; backend will deep-fallback when needed
    this.aasClient.listElements('target', this.createdTargetSystemId, smIdEnc, 'shallow').subscribe({
      next: (resp) => { const list = Array.isArray(resp) ? resp : (resp?.result ?? []); console.log('[TargetCreate] loadRootElements respCount=', list.length, 'rawResp=', resp);
        const roots = list.filter((el: any) => {
          const p = el?.idShortPath || el?.idShort;
          return p && !String(p).includes('/');
        });
        if (attach) { attach.children = roots.map((el: any) => this.mapElementToNode(submodelId, el)); this.treeNodes = [...this.treeNodes]; }
      },
      error: (e) => {
        console.error('[TargetCreate] loadRootElements error', e);
        const status = e?.status;
        if (status === 400 || status === 404) {
          // Frontend fallback: try deep and filter to roots
          this.aasClient.listElements('target', this.createdTargetSystemId!, smIdEnc, 'all').subscribe({
            next: (resp2) => {
              const arr: any[] = Array.isArray(resp2) ? resp2 : (resp2?.result ?? []);
              const roots = arr.filter((el: any) => {
                const p = el?.idShortPath || el?.idShort;
                return p && !String(p).includes('/');
              });
              if (attach) {
                attach.children = roots.map((el: any) => this.mapElementToNode(submodelId, el));
                this.treeNodes = [...this.treeNodes];
              }
            },
            error: (e2) => { console.error('[TargetCreate] loadRootElements deep-fallback error', e2); }
          });
        }
      }
    });
  }
  private loadChildren(submodelId: string, parentPath: string, node: TreeNode): void {
    if (!this.createdTargetSystemId) return;
    const smIdEnc = this.encodeIdToBase64Url(submodelId); console.log('[TargetCreate] loadChildren smIdRaw=', submodelId, 'smIdB64=', smIdEnc, 'parentPath=', parentPath);
    // Use shallow; backend will deep-fallback when needed
    this.aasClient.listElements('target', this.createdTargetSystemId, smIdEnc, 'shallow', parentPath).subscribe({
      next: (resp) => { const list = Array.isArray(resp) ? resp : (resp?.result ?? []); console.log('[TargetCreate] loadChildren respCount=', list.length, 'rawResp=', resp);
        // Backend already returns correct direct children, no filtering needed
        node.children = list.map((el: any) => {
          if (!el.idShortPath && el.idShort) { el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort; }
          return this.mapElementToNode(submodelId, el);
        });
        this.treeNodes = [...this.treeNodes];
      },
      error: (e) => {
        console.error('[TargetCreate] loadChildren error', e);
        const status = e?.status;
        if (status === 400 || status === 404) {
          // Frontend fallback: deep and filter direct children of parentPath
          this.aasClient.listElements('target', this.createdTargetSystemId!, smIdEnc, 'all').subscribe({
            next: (resp2) => {
              const arr: any[] = Array.isArray(resp2) ? resp2 : (resp2?.result ?? []);
              // Backend filtering should handle this, but keep minimal direct child filtering for fallback
              const prefix = parentPath ? (parentPath.endsWith('/') ? parentPath : parentPath + '/') : '';
              const children = arr.filter((el: any) => {
                const p = el?.idShortPath || el?.idShort;
                if (!p) return false;
                if (!parentPath) return !String(p).includes('/');
                
                // FIX: Only include direct children, not nested elements
                if (!String(p).startsWith(prefix)) return false;
                
                // Check if this is a direct child (no additional '/' after the prefix)
                const relativePath = String(p).substring(prefix.length);
                return relativePath && !relativePath.includes('/');
              });
              node.children = children.map((el: any) => {
                if (!el.idShortPath && el.idShort) { el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort; }
                return this.mapElementToNode(submodelId, el);
              });
              this.treeNodes = [...this.treeNodes];
            },
            error: (e2) => { console.error('[TargetCreate] loadChildren deep-fallback error', e2); }
          });
        }
      }
    });
  }
  private loadLiveElementDetails(smId: string, idShortPath: string | undefined, node?: TreeNode): void {
    const systemId = this.createdTargetSystemId; if (!systemId) return;
    this.selectedLiveLoading = true;
    const smIdEnc = this.encodeIdToBase64Url(smId); console.log('[TargetCreate] loadLiveElementDetails smIdRaw=', smId, 'smIdB64=', smIdEnc, 'path=', idShortPath);
    this.aasClient.getElement('target', systemId, smIdEnc, idShortPath || '').subscribe({
      next: (found: any) => { this.selectedLiveLoading = false; console.log('[TargetCreate] loadLiveElementDetails resp=', found);
        const liveType = found?.modelType || (found?.valueType ? 'Property' : undefined);
        const minValue = (found as any).min ?? (found as any).minValue;
        const maxValue = (found as any).max ?? (found as any).maxValue;
        const inputVars = Array.isArray((found as any).inputVariables) ? (found as any).inputVariables : [];
        const outputVars = Array.isArray((found as any).outputVariables) ? (found as any).outputVariables : [];
        const inoutVars = Array.isArray((found as any).inoutputVariables) ? (found as any).inoutputVariables : [];
        const ann1 = (found as any).annotations; const ann2 = (found as any).annotation;
        const annotationsRaw = Array.isArray(ann1) ? ann1 : (Array.isArray(ann2) ? ann2 : []);
        const mapVar = (v: any): any | null => { const val = v?.value ?? v; const idShort = val?.idShort; if (!idShort) return null; return { idShort, modelType: val?.modelType, valueType: val?.valueType }; };
        const mapAnnotation = (a: any): any | null => { const val = a?.value ?? a; const idShort = val?.idShort; if (!idShort) return null; return { idShort, modelType: val?.modelType, valueType: val?.valueType, value: val?.value }; };
        
        // Handle relationship references
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
        
        this.selectedLivePanel = {
          label: found.idShort,
          type: liveType || 'Unknown',
          value: (found as any).value,
          valueType: (found as any).valueType,
          min: minValue,
          max: maxValue,
          firstRef,
          secondRef,
          inputVariables: inputVars.map(mapVar).filter(Boolean),
          outputVariables: outputVars.map(mapVar).filter(Boolean),
          inoutputVariables: inoutVars.map(mapVar).filter(Boolean),
          annotations: annotationsRaw.map(mapAnnotation).filter(Boolean)
        } as any;
        if (node && node.data) {
          node.data.idShortPath = idShortPath || node.data.idShortPath;
          node.data.modelType = liveType || node.data.modelType;
          node.data.raw = { ...(node.data.raw || {}), idShortPath: (idShortPath || node.data.idShortPath), modelType: found.modelType, valueType: found.valueType };
          this.treeNodes = [...this.treeNodes];
        }
      },
      error: (e) => { this.selectedLiveLoading = false; console.error('[TargetCreate] loadLiveElementDetails error', e); }
    });
  }

  // Create dialogs & actions (target)
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
    { "modelType": "Property", "idShort": "Name", "valueType": "xs:string", "value": "Foo" }
  ]
}`;
  collectionSubmodelTemplate: string = `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "submodelElements": [
    { "modelType": "SubmodelElementCollection", "idShort": "address", "value": [ { "modelType": "Property", "idShort": "street", "valueType": "xs:string", "value": "Main St" } ] }
  ]
}`;
  setSubmodelTemplate(kind: 'minimal'|'property'|'collection'): void {
    if (kind === 'minimal') this.newSubmodelJson = this.minimalSubmodelTemplate;
    if (kind === 'property') this.newSubmodelJson = this.propertySubmodelTemplate;
    if (kind === 'collection') this.newSubmodelJson = this.collectionSubmodelTemplate;
  }
  openCreateSubmodel(): void { this.showSubmodelDialog = true; }
  createSubmodel(): void {
    if (!this.createdTargetSystemId) return;
    try {
      const body = JSON.parse(this.newSubmodelJson);
      this.aasClient.createSubmodel('target', this.createdTargetSystemId, body).subscribe({
        next: () => { this.showSubmodelDialog = false; this.discoverSubmodels(); },
        error: () => {}
      });
    } catch {}
  }

  // Element creation dialog
  showElementDialog = false;
  elementDialogData: AasElementDialogData | null = null;
  openCreateElement(smId: string, parent?: string): void {
    if (!this.createdTargetSystemId) return;
    
    this.elementDialogData = {
      submodelId: smId,
      parentPath: parent,
      systemId: this.createdTargetSystemId,
      systemType: 'target'
    };
    this.showElementDialog = true;
  }
  onElementDialogResult(result: AasElementDialogResult): void {
    if (result.success && result.element) {
      this.handleElementCreation(result.element);
    } else if (result.error) {
      console.error('[TargetCreate] Element creation failed:', result.error);
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
    if (!this.createdTargetSystemId) return;
    
    try {
      console.log('[TargetCreate] Creating element:', elementData);
      
      // Encode parentPath for BaSyx (convert / to .)
      const encodedParentPath = elementData.parentPath ? 
        elementData.parentPath.replace(/\//g, '.') : 
        undefined;
      
      console.log('[TargetCreate] Encoded parentPath:', encodedParentPath);
      
      // Use the AAS client to create the element
      const smIdB64 = this.encodeIdToBase64Url(elementData.submodelId);
      await this.aasClient.createElement('target', this.createdTargetSystemId, smIdB64, elementData.body, encodedParentPath).toPromise();
      
      console.log('[TargetCreate] Element created successfully');
      
      // Show success toast
      this.messageService.add({
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
      
      // Refresh the tree
      this.discoverSubmodels();
      
    } catch (error) {
      console.error('[TargetCreate] Error creating element:', error);
      
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

  // Delete actions & value set (target)
  showDeleteSubmodelDialog = false;
  deleteSubmodelId: string | null = null;
  deleteSubmodel(submodelId: string): void { this.deleteSubmodelId = submodelId; this.showDeleteSubmodelDialog = true; }
  proceedDeleteSubmodel(): void {
    if (!this.createdTargetSystemId || !this.deleteSubmodelId) { this.showDeleteSubmodelDialog = false; return; }
    const smIdB64 = this.encodeIdToBase64Url(this.deleteSubmodelId);
    this.aasClient.deleteSubmodel('target', this.createdTargetSystemId, smIdB64).subscribe({
      next: () => { this.showDeleteSubmodelDialog = false; this.deleteSubmodelId = null; this.discoverSubmodels(); this.messageService.add({ severity: 'success', summary: 'Submodel deleted', detail: 'Submodel removed from shell' }); },
      error: () => { this.showDeleteSubmodelDialog = false; }
    });
  }
  deleteElement(submodelId: string, idShortPath: string): void {
    if (!this.createdTargetSystemId || !submodelId || !idShortPath) {
      console.log('[TargetCreate] deleteElement: Missing required data', {
        createdTargetSystemId: this.createdTargetSystemId,
        submodelId,
        idShortPath
      });
      return;
    }
    
    // Use the original submodelId, not base64 encoded
    // The backend will handle the encoding internally
    
    console.log('[TargetCreate] deleteElement: Deleting element', {
      systemId: this.createdTargetSystemId,
      submodelId,
      idShortPath
    });
    
    // Skip existence check for deep elements and proceed directly with deletion
    console.log('[TargetCreate] deleteElement: Proceeding directly with deletion (skipping existence check for deep elements)');
    this.performDelete(submodelId, idShortPath);
  }
  
  private performDelete(submodelId: string, idShortPath: string): void {
    this.aasClient.deleteElement('target', this.createdTargetSystemId, submodelId, idShortPath).subscribe({
      next: () => {
        console.log('[TargetCreate] deleteElement: Element deleted successfully');
        
        // Trigger discover to refresh the entire tree
        console.log('[TargetCreate] deleteElement: Triggering discover to refresh tree');
        this.discoverSubmodels();
        
        // Show success message
        this.messageService.add({
          severity: 'success',
          summary: 'Element Deleted',
          detail: 'Element has been successfully deleted.',
          life: 3000
        });
      },
      error: (err) => {
        console.error('[TargetCreate] deleteElement: Error deleting element', err);
        console.log('[TargetCreate] deleteElement: Error details', {
          status: err.status,
          statusText: err.statusText,
          url: err.url,
          message: err.message,
          idShortPath,
          submodelId
        });
        
        // Check if it's a 404 error (element not found)
        if (err.status === 404) {
          console.log('[TargetCreate] deleteElement: Element not found, removing from tree anyway');
          
          // Remove the element from the tree directly
          this.removeElementFromTree(submodelId, idShortPath);
          
          this.messageService.add({
            severity: 'success',
            summary: 'Element Removed',
            detail: `Element '${idShortPath.split('.').pop()}' has been removed from the tree.`,
            life: 3000
          });
        } else {
          console.error('[TargetCreate] deleteElement: Unexpected error', err);
        }
      }
    });
  }

  private removeElementFromTree(submodelId: string, idShortPath: string): void {
    console.log('[TargetCreate] removeElementFromTree: Removing element from tree', {
      submodelId,
      idShortPath
    });
    
    const elementKey = `${submodelId}::${idShortPath}`;
    const parentPath = idShortPath.includes('.') ? idShortPath.substring(0, idShortPath.lastIndexOf('.')) : '';
    const parentKey = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    
    console.log('[TargetCreate] removeElementFromTree: Keys', {
      elementKey,
      parentKey,
      parentPath
    });
    
    // Try to find and remove the element from the tree
    let elementRemoved = false;
    
    // Find the parent node
    const parentNode = this.findNodeByKey(parentKey, this.treeNodes);
    if (parentNode && parentNode.children) {
      console.log('[TargetCreate] removeElementFromTree: Parent node found', {
        parentLabel: parentNode.label,
        childrenCount: parentNode.children.length
      });
      
      // Remove the element from parent's children
      const initialLength = parentNode.children.length;
      parentNode.children = parentNode.children.filter(child => {
        const shouldKeep = child.key !== elementKey;
        if (!shouldKeep) {
          console.log('[TargetCreate] removeElementFromTree: Removing child', {
            childKey: child.key,
            childLabel: child.label
          });
        }
        return shouldKeep;
      });
      
      elementRemoved = initialLength > parentNode.children.length;
      console.log('[TargetCreate] removeElementFromTree: Element removed', {
        initialLength,
        finalLength: parentNode.children.length,
        removed: elementRemoved
      });
    }
    
    // Force tree update
    this.treeNodes = [...this.treeNodes];
    
    // If element wasn't found in the tree, refresh the parent node
    if (!elementRemoved) {
      console.log('[TargetCreate] removeElementFromTree: Element not found in tree, refreshing parent node');
      this.refreshNodeLive(submodelId, parentPath, parentNode || undefined);
    }
  }

  showValueDialog = false;
  valueSubmodelId = '';
  valueElementPath = '';
  valueNew = '';
  valueTypeHint = 'xs:string';
  openSetValue(smId: string, element: any): void {
    this.valueSubmodelId = smId;
    this.valueElementPath = element.idShortPath || element.data?.idShortPath || element.raw?.idShortPath || element.idShort;
    this.valueTypeHint = element.valueType || 'xs:string';
    this.valueNew = '';
    this.showValueDialog = true;
  }
  setValue(): void {
    if (!this.createdTargetSystemId || !this.valueSubmodelId || !this.valueElementPath) return;
    const smIdB64 = this.encodeIdToBase64Url(this.valueSubmodelId); // send plain ID for LIVE upstream calls
    const parsedValue = this.parseValueForType(this.valueNew, this.valueTypeHint);
    this.aasClient.patchElementValue('target', this.createdTargetSystemId, smIdB64, this.valueElementPath, parsedValue as any).subscribe({
      next: () => { this.showValueDialog = false; const parent = this.valueElementPath.includes('/') ? this.valueElementPath.substring(0, this.valueElementPath.lastIndexOf('/')) : ''; this.refreshNodeLive(this.valueSubmodelId, parent, undefined); this.messageService.add({ severity: 'success', summary: 'Value updated', detail: 'Property value saved', life: 2500 }); },
      error: () => {}
    });
  }
  private parseValueForType(raw: string, valueType?: string): any {
    if (!valueType) return raw; const t = valueType.toLowerCase();
    if (t.includes('boolean')) { if (raw === 'true' || raw === 'false') return raw === 'true'; return !!raw; }
    if (t.includes('int') || t.includes('integer') || t.includes('long')) { const n = parseInt(raw, 10); return isNaN(n) ? raw : n; }
    if (t.includes('float') || t.includes('double') || t.includes('decimal')) { const n = parseFloat(raw); return isNaN(n) ? raw : n; }
    return raw;
  }

  private refreshNodeLive(submodelId: string, parentPath: string, node?: TreeNode): void {
    if (!this.createdTargetSystemId) return;
    const smIdB64 = this.encodeIdToBase64Url(submodelId); // send plain ID for LIVE upstream calls
    this.aasClient.listElements('target', this.createdTargetSystemId, smIdB64, 'shallow', parentPath || undefined).subscribe({
      next: (resp) => {
        const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
        const mapped = list.map((el: any) => {
          if (!el.idShortPath && el.idShort) { el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort; }
          return this.mapElementToNode(submodelId, el);
        });
        if (node) { node.children = mapped; this.treeNodes = [...this.treeNodes]; }
        else { const attachNode = this.findNodeByKey(submodelId, this.treeNodes); if (attachNode) { attachNode.children = mapped; this.treeNodes = [...this.treeNodes]; } }
      },
      error: () => {}
    });
  }

  private findNodeByKey(key: string, nodes: TreeNode[] | undefined): TreeNode | null {
    if (!nodes) return null; for (const n of nodes) { if (n.key === key) return n; const found = this.findNodeByKey(key, n.children as TreeNode[]); if (found) return found; } return null;
  }
  private mapSubmodelToNode(sm: any): TreeNode {
    const id = sm.submodelId || sm.id || (sm.keys && sm.keys[0]?.value);
    const label = (sm.submodelIdShort || sm.idShort) || id;
    const kindRaw = (sm.kind || sm.submodelKind || '').toString();
    const isTemplate = kindRaw && kindRaw.toLowerCase().includes('template');
    const modelType = isTemplate ? 'Submodel Template' : 'Submodel';
    return { key: id, label, data: { type: 'submodel', id, modelType, raw: sm }, leaf: false, children: [] } as TreeNode;
  }
  private mapElementToNode(submodelId: string, el: any): TreeNode {
    const computedType = this.inferModelType(el);
    const label = el.idShort;
    const typeHasChildren = el?.modelType === 'SubmodelElementCollection' || el?.modelType === 'SubmodelElementList' || el?.modelType === 'Operation' || el?.modelType === 'Entity';
    
    // RADIKALE LÖSUNG: Prüfe explizit auf leere Collections/Lists
    let hasChildren = false;
    
    if (typeHasChildren) {
      // Für Collections/Lists: Prüfe ob sie tatsächlich Items haben
      if (el?.modelType === 'SubmodelElementCollection' || el?.modelType === 'SubmodelElementList') {
        // Prüfe verschiedene mögliche Felder für Items
        const hasItems = (el?.value && Array.isArray(el.value) && el.value.length > 0) ||
                        (el?.submodelElements && Array.isArray(el.submodelElements) && el.submodelElements.length > 0) ||
                        (el?.items && Array.isArray(el.items) && el.items.length > 0) ||
                        (el?.hasChildren === true);
        
        hasChildren = hasItems;
        console.log('[TargetCreate] mapElementToNode - Collection/List:', label, 'hasItems:', hasItems, 'value:', el?.value, 'submodelElements:', el?.submodelElements);
      } else {
        // Für andere Typen (Operation, Entity): Standard-Logik
        hasChildren = el?.hasChildren === true || typeHasChildren;
      }
    } else {
      hasChildren = el?.hasChildren === true;
    }
    
    return { key: `${submodelId}::${el.idShortPath || el.idShort}`, label, data: { type: 'element', submodelId, idShortPath: el.idShortPath || el.idShort, modelType: computedType, raw: el }, leaf: !hasChildren, children: [] } as TreeNode;
  }
  private inferModelType(el: any): string | undefined {
    if (!el) return undefined; if (el.modelType) return el.modelType;
    if (el.min !== undefined || el.max !== undefined || el.minValue !== undefined || el.maxValue !== undefined) return 'Range';
    if (el.valueType) return 'Property';
    if (Array.isArray(el.inputVariables) || Array.isArray(el.outputVariables) || Array.isArray(el.inoutputVariables)) return 'Operation';
    if (Array.isArray(el.value)) {
      const isML = el.value.every((v: any) => v && (v.language !== undefined) && (v.text !== undefined));
      if (isML) return 'MultiLanguageProperty';
      if (el.typeValueListElement || el.orderRelevant !== undefined) return 'SubmodelElementList';
      return 'SubmodelElementCollection';
    }
    if (el.first || el.firstReference) { const ann = el.annotations || el.annotation; return Array.isArray(ann) ? 'AnnotatedRelationshipElement' : 'RelationshipElement'; }
    if (Array.isArray(el.annotations) || Array.isArray(el.annotation)) return 'AnnotatedRelationshipElement';
    if (Array.isArray(el.statements)) return 'Entity';
    if (Array.isArray(el.keys)) return 'ReferenceElement';
    if (el.contentType && (el.fileName || el.path)) return 'File';
    return undefined;
  }

  private encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    
    // Use the same encoding as the backend
    const b64 = btoa(id);
    return b64
      .replace(/=+$/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  }

  /**
   * Handle header creation event
   */
  onHeaderCreated(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Created',
      detail: 'Header has been successfully created.',
      life: 3000
    });
  }

  /**
   * Handle header deletion event
   */
  onHeaderDeleted(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Header Deleted',
      detail: 'Header has been successfully deleted.',
      life: 3000
    });
  }


}


