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
  private createdEventEmitted = false;

  typeOptions = [
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' },
    { label: 'AAS', value: 'AAS' }
  ];
  authTypeOptions = [
    { label: 'None', value: ApiAuthType.None },
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

  /**
   * Initializes the component form and subscribes to API type changes
   * to dynamically update the stepper sequence.
   */
  ngOnInit(): void {
    this.form = this.formService.createForm();
    this.formService.setupFormSubscriptions(this.form);
    this.form.get('apiType')!.valueChanges.subscribe((apiType: string) => {
      this.steps = this.formService.getSteps(apiType);
    });
  }

  /**
   * Updates the form fields when a target system input changes.
   * @param changes Object containing the changed input values.
   */
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

  /**
   * Handles file selection for OpenAPI specifications.
   * Disables the OpenAPI URL field once a file is chosen.
   * @param event File selection event from the file upload component.
   */
  onFileSelected(event: any): void {
    this.selectedFile = event.files?.[0] ?? null;
    this.fileSelected = true;
    this.form.get('openApiSpec')!.disable();
  }

  /**
   * Cancels the current dialog and resets all form and state variables.
   */
  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.formService.resetForm(this.form);
    this.selectedFile = null;
    this.fileSelected = false;
    this.currentStep = 0;
    this.createdEventEmitted = false;
  }

  /**
   * Submits the form to create a new target system.
   * Handles both file-based and URL-based OpenAPI submissions.
   */
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
          if (!this.createdEventEmitted) {
            this.created.emit(resp);
            this.createdEventEmitted = true;
          }
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

  /**
   * Navigates to the next step in the creation wizard.
   * Automatically triggers save if necessary.
   */
  goNext(): void {
    if (this.currentStep === 0) {
      if (this.createdTargetSystemId) { this.currentStep = 1; return; }
      this.save();
    } else if (this.currentStep === 1) {
      this.currentStep = 2;
    }
  }

  /** Navigates back to the previous step in the wizard. */
  goBack(): void {
    if (this.currentStep > 0) this.currentStep -= 1;
  }

  /**
   * Completes the wizard, emits the created event,
   * and resets all temporary data and dialog states.
   */
  finish(): void {
    if (this.createdTargetSystemId && !this.createdEventEmitted) {
      const createdSystem = { id: this.createdTargetSystemId, name: this.form.get('name')?.value || 'Unknown' } as TargetSystemDTO;
      this.created.emit(createdSystem);
      this.createdEventEmitted = true;
    }
    this.visible = false;
    this.visibleChange.emit(false);
    this.formService.resetForm(this.form);
    this.selectedFile = null;
    this.fileSelected = false;
    this.currentStep = 0;
  }

  
  isAas(): boolean { return this.form.get('apiType')!.value === 'AAS'; }
  isRest(): boolean { return this.form.get('apiType')!.value === 'REST_OPENAPI'; }

  
  isTesting = false;
  aasTestOk: boolean | null = null;
  aasError: string | null = null;
  aasPreview: { idShort?: string; assetKind?: string } | null = null;
  /**
   * Tests connectivity to the AAS backend and displays the connection result.
   * Creates the target system if it doesn't exist before testing.
   */
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
          if (!this.createdEventEmitted) { this.created.emit(resp); this.createdEventEmitted = true; }
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
          key: 'targetAAS',
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
          key: 'targetAAS',
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
        key: 'targetAAS',
        severity: 'error', 
        summary: 'Connection failed', 
        detail: 'Please verify Base URL, AAS ID and auth.', 
        life: 5000 
      });
    }
  }

  /**
   * Checks if all required form fields and AAS connection validations are met
   * before allowing navigation to the next wizard step.
   * @returns True if navigation to the next step is allowed.
   */
  canProceedFromStep1(): boolean {
    return this.formService.isFormValidForStep(this.form, 0, this.form.get('apiType')!.value, this.aasTestOk);
  }

  
  showAasxUpload = false;
  aasxSelectedFile: File | null = null;
  isUploadingAasx = false;
  aasxPreview: any = null;
  aasxSelection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> } = { submodels: [] };

  /** Opens the dialog for uploading an AASX package. */
  openAasxUpload(): void {
    this.showAasxUpload = true;
    this.aasxSelectedFile = null;
  }
  /**
   * Handles the selection of an AASX file and previews its content.
   * @param event File upload event containing the selected file.
   */
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
        this.aasxSelection = { submodels: arr.map((sm: any) => ({ id: sm.id || sm.submodelId, full: true, elements: [] })) };
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
  /**
   * Uploads the selected AASX file and triggers tree refreshes
   * to reflect updated submodels.
   */
  uploadAasx(): void {
    if (this.isUploadingAasx) return;
    if (!this.aasxSelectedFile || !this.createdTargetSystemId) return;
    this.isUploadingAasx = true;
    this.messageService.add({ key: 'targetAAS', severity: 'info', summary: 'Uploading AASX', detail: `${this.aasxSelectedFile?.name} (${this.aasxSelectedFile?.size} bytes)` });
    const hasSelection = this.aasxSelection?.submodels?.some(s => s.full);
    const selectedIds: string[] = hasSelection ? this.aasxSelection.submodels.map(s => s.id).filter(Boolean) : [];
    const req$ = hasSelection
      ? this.aasClient.attachSelectedAasx('target', this.createdTargetSystemId, this.aasxSelectedFile, this.aasxSelection)
      : this.aasClient.uploadAasx('target', this.createdTargetSystemId, this.aasxSelectedFile);
    req$.subscribe({
      next: () => {
        this.isUploadingAasx = false;
        this.showAasxUpload = false;
        this.onDiscover('LIVE', selectedIds);
        setTimeout(() => this.onDiscover('LIVE', selectedIds), 1200);
        setTimeout(() => this.onDiscover('LIVE', selectedIds), 2500);
        setTimeout(() => this.onDiscover('LIVE', selectedIds), 5000);
        this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Upload accepted', detail: 'AASX uploaded and attached', life: 3000 });
      },
      error: (err) => {
        this.isUploadingAasx = false;
        const detail = err?.error || err?.message || 'See console for details';
        this.messageService.add({ key: 'targetAAS', severity: 'error', summary: 'Upload failed', detail });
      }
    });
  }

  
  isDiscovering = false;
  treeNodes: TreeNode[] = [];
  submodels: any[] = [];
  selectedNode?: TreeNode;
  selectedLivePanel: any = null;
  selectedLiveLoading = false;

  /**
   * Loads available submodels for the target AAS system.
   * Refreshes the tree representation in the UI.
   */
  discoverSubmodels(): void {
    if (!this.createdTargetSystemId) { if (!this.isCreating) { this.save(); } return; }
    this.isDiscovering = true;
    this.aasClient.listSubmodels('target', this.createdTargetSystemId, {}).subscribe({
      next: (resp) => {
        this.isDiscovering = false;
        this.submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
        this.treeNodes = this.submodels.map((sm: any) => this.mapSubmodelToNode(sm));
      },
      error: (_err) => { this.isDiscovering = false; console.error('[TargetCreate] discover error', _err); this.errorService.handleError(_err); }
    });
  }

  /**
   * Reloads AAS submodels and optionally refreshes specific nodes.
   * @param source Indicates whether discovery is from 'LIVE' or cached data.
   * @param refreshIds List of submodel IDs to refresh after discovery.
   */
  onDiscover(source: 'LIVE' = 'LIVE', refreshIds: string[] = []): void {
    if (!this.createdTargetSystemId) return;
    this.isDiscovering = true;
    const params = {};
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
  
  /**
   * Handles expansion of a tree node by dynamically loading its children.
   * @param event Node expansion event.
   */
  onNodeExpand(event: any): void {
    const node: TreeNode = event.node; if (!node) return;
    if (node.leaf) {
      return;
    }
    if (node.data?.type === 'submodel') { this.loadRootElements(node.data.id, node); }
    else if (node.data?.type === 'element') { this.loadChildren(node.data.submodelId, node.data.idShortPath, node); }
  }
  /**
   * Handles selection of a tree node and displays its details.
   * @param event Node selection event.
   */
  onNodeSelect(event: any): void {
    const node: TreeNode = event.node; this.selectedNode = node; this.selectedLivePanel = null;
    if (!node || node.data?.type !== 'element') return;
    const smId: string = node.data.submodelId; const idShortPath: string = node.data.idShortPath;
    this.loadLiveElementDetails(smId, idShortPath, node);
    setTimeout(() => { const el = document.getElementById('element-details'); el?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }, 0);
  }
  /**
   * Loads all root-level elements for a given submodel and attaches them to the tree.
   * @param submodelId The submodel ID whose root elements should be loaded.
   * @param attach Optional TreeNode to attach the results to.
   */
  private loadRootElements(submodelId: string, attach?: TreeNode): void {
    if (!this.createdTargetSystemId) return;
    const smIdEnc = this.encodeIdToBase64Url(submodelId);
    this.aasClient.listElements('target', this.createdTargetSystemId, smIdEnc, 'shallow').subscribe({
      next: (resp) => { const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
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
  /**
   * Loads all direct child elements of a given AAS parent element.
   * @param submodelId The submodel ID.
   * @param parentPath The hierarchical path of the parent element.
   * @param node The tree node to which children will be attached.
   */
  private loadChildren(submodelId: string, parentPath: string, node: TreeNode): void {
    if (!this.createdTargetSystemId) return;
    const smIdEnc = this.encodeIdToBase64Url(submodelId);
    this.aasClient.listElements('target', this.createdTargetSystemId, smIdEnc, 'shallow', parentPath).subscribe({
      next: (resp) => { const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
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
          this.aasClient.listElements('target', this.createdTargetSystemId!, smIdEnc, 'all').subscribe({
            next: (resp2) => {
              const arr: any[] = Array.isArray(resp2) ? resp2 : (resp2?.result ?? []);
              const prefix = parentPath ? (parentPath.endsWith('/') ? parentPath : parentPath + '/') : '';
              const children = arr.filter((el: any) => {
                const p = el?.idShortPath || el?.idShort;
                if (!p) return false;
                if (!parentPath) return !String(p).includes('/');
                if (!String(p).startsWith(prefix)) return false;
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
  /**
   * Loads detailed information about a live AAS element and updates the detail view.
   * @param smId Submodel ID of the element.
   * @param idShortPath Path of the selected element.
   * @param node Optional TreeNode to attach updates to.
   */
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
        const mapAnnotation = (a: any): any | null => { 
          
          const idShort = a?.idShort; 
          if (!idShort) return null; 
          return { idShort, modelType: a?.modelType, valueType: a?.valueType, value: a?.value };
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
        
        const mappedAnnotations = annotationsRaw.map(mapAnnotation).filter(Boolean);
        
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
          annotations: mappedAnnotations
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
        next: () => { 
          this.showSubmodelDialog = false; 
          this.discoverSubmodels(); 
          this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Submodel created', detail: 'Submodel was created successfully.', life: 3000 });
        },
        error: () => {}
      });
    } catch {}
  }

  
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
   * Handles the creation of new AAS elements and updates the AAS tree view.
   * @param elementData Data of the newly created element.
   */
  private async handleElementCreation(elementData: any): Promise<void> {
    if (!this.createdTargetSystemId) return;
    
    try {
      console.log('[TargetCreate] Creating element:', elementData);
      
      
      const encodedParentPath = elementData.parentPath ? 
        elementData.parentPath.replace(/\//g, '.') : 
        undefined;
      
      console.log('[TargetCreate] Encoded parentPath:', encodedParentPath);
      
      
      const smIdB64 = this.encodeIdToBase64Url(elementData.submodelId);
      await this.aasClient.createElement('target', this.createdTargetSystemId, smIdB64, elementData.body, encodedParentPath).toPromise();
      
      console.log('[TargetCreate] Element created successfully');
      
      
      this.messageService.add({
        key: 'targetAAS',
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
      
      
      this.discoverSubmodels();
      
    } catch (error) {
      console.error('[TargetCreate] Error creating element:', error);
      
      
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

  
  showDeleteSubmodelDialog = false;
  deleteSubmodelId: string | null = null;
  deleteSubmodel(submodelId: string): void { this.deleteSubmodelId = submodelId; this.showDeleteSubmodelDialog = true; }
  proceedDeleteSubmodel(): void {
    if (!this.createdTargetSystemId || !this.deleteSubmodelId) { this.showDeleteSubmodelDialog = false; return; }
    const smIdB64 = this.encodeIdToBase64Url(this.deleteSubmodelId);
    this.aasClient.deleteSubmodel('target', this.createdTargetSystemId, smIdB64).subscribe({
      next: () => { this.showDeleteSubmodelDialog = false; this.deleteSubmodelId = null; this.discoverSubmodels(); this.messageService.add({ key: 'targetAAS', severity: 'success', summary: 'Submodel deleted', detail: 'Submodel removed from shell' }); },
      error: () => { this.showDeleteSubmodelDialog = false; }
    });
  }
  /**
   * Deletes an AAS element and removes it from the tree structure.
   * @param submodelId Submodel ID containing the element.
   * @param idShortPath The path of the element to delete.
   */
  deleteElement(submodelId: string, idShortPath: string): void {
    if (!this.createdTargetSystemId || !submodelId || !idShortPath) {
      return;
    }
    this.performDelete(submodelId, idShortPath);
  }
  private performDelete(submodelId: string, idShortPath: string): void {
    this.aasClient.deleteElement('target', this.createdTargetSystemId, submodelId, idShortPath).subscribe({
      next: () => {
        this.discoverSubmodels();
        this.messageService.add({
          key: 'targetAAS',
          severity: 'success',
          summary: 'Element Deleted',
          detail: 'Element has been successfully deleted.',
          life: 3000
        });
      },
      error: (err) => {
        console.error('[TargetCreate] deleteElement: Error deleting element', err);
        if (err.status === 404) {
          this.removeElementFromTree(submodelId, idShortPath);
          this.messageService.add({
            key: 'targetAAS',
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
  /**
   * Removes an element from the AAS tree locally without backend call.
   * @param submodelId ID of the submodel.
   * @param idShortPath Path of the element to remove.
   */
  private removeElementFromTree(submodelId: string, idShortPath: string): void {
    const elementKey = `${submodelId}::${idShortPath}`;
    const parentPath = idShortPath.includes('.') ? idShortPath.substring(0, idShortPath.lastIndexOf('.')) : '';
    const parentKey = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    let elementRemoved = false;
    const parentNode = this.findNodeByKey(parentKey, this.treeNodes);
    if (parentNode && parentNode.children) {
      const initialLength = parentNode.children.length;
      parentNode.children = parentNode.children.filter(child => child.key !== elementKey);
      elementRemoved = initialLength > parentNode.children.length;
    }
    this.treeNodes = [...this.treeNodes];
    if (!elementRemoved) {
      this.refreshNodeLive(submodelId, parentPath, parentNode || undefined);
    }
  }

  showValueDialog = false;
  valueSubmodelId = '';
  valueElementPath = '';
  valueNew = '';
  valueTypeHint = 'xs:string';
  /**
   * Opens the dialog to update the value of a property element.
   * @param smId Submodel ID of the property.
   * @param element The selected AAS element.
   */
  openSetValue(smId: string, element: any): void {
    this.valueSubmodelId = smId;
    this.valueElementPath = element.idShortPath || element.data?.idShortPath || element.raw?.idShortPath || element.idShort;
    this.valueTypeHint = element.valueType || 'xs:string';
    this.valueNew = '';
    this.showValueDialog = true;
  }
  /**
   * Saves the updated property value to the backend and refreshes the UI.
   */
  setValue(): void {
    if (!this.createdTargetSystemId || !this.valueSubmodelId || !this.valueElementPath) return;
    const smIdB64 = this.encodeIdToBase64Url(this.valueSubmodelId);
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
    const smIdB64 = this.encodeIdToBase64Url(submodelId); 
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
    
    
    let hasChildren = false;
    
    if (typeHasChildren) {
      
      if (el?.modelType === 'SubmodelElementCollection' || el?.modelType === 'SubmodelElementList') {
        
        const hasItems = (el?.value && Array.isArray(el.value) && el.value.length > 0) ||
                        (el?.submodelElements && Array.isArray(el.submodelElements) && el.submodelElements.length > 0) ||
                        (el?.items && Array.isArray(el.items) && el.items.length > 0) ||
                        (el?.hasChildren === true);
        
        hasChildren = hasItems;
        console.log('[TargetCreate] mapElementToNode - Collection/List:', label, 'hasItems:', hasItems, 'value:', el?.value, 'submodelElements:', el?.submodelElements);
      } else {
        
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

  /**
   * Encodes an ID into Base64 URL-safe format for API calls.
   * @param id The ID string to encode.
   * @returns Encoded Base64 URL-safe string.
   */
  private encodeIdToBase64Url(id: string): string {
    if (!id) return id;
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


