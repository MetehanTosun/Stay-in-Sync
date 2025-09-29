import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';


import {DialogModule} from 'primeng/dialog';
import {CheckboxModule} from 'primeng/checkbox';
import {DropdownModule} from 'primeng/dropdown';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {TextareaModule} from 'primeng/textarea';
import {StepsModule} from 'primeng/steps';
import {FileSelectEvent, FileUploadEvent, FileUploadModule} from 'primeng/fileupload';
import {TreeModule} from 'primeng/tree';
import {MessageService, TreeNode} from 'primeng/api';
import { AasElementDialogComponent, AasElementDialogData, AasElementDialogResult } from '../../../../shared/components/aas-element-dialog/aas-element-dialog.component';



import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {CreateSourceSystemDTO} from '../../models/createSourceSystemDTO';
import {ApiAuthType} from '../../models/apiAuthType';
import {BasicAuthDTO} from '../../models/basicAuthDTO';
import {ApiKeyAuthDTO} from '../../models/apiKeyAuthDTO';
import {ManageEndpointsComponent} from '../manage-endpoints/manage-endpoints.component';
import {ManageApiHeadersComponent} from '../manage-api-headers/manage-api-headers.component';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {AasService} from '../../services/aas.service';
import {CreateSourceSystemFormService} from '../../services/create-source-system-form.service';
import {CreateSourceSystemAasService} from '../../services/create-source-system-aas.service';
import {CreateSourceSystemDialogService} from '../../services/create-source-system-dialog.service';

interface OperationVarView { idShort: string; modelType?: string; valueType?: string }
interface AnnotationView { idShort: string; modelType?: string; valueType?: string; value?: any }
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

/**
 * Component for creating or editing a Source System.
 * Provides a stepper UI for metadata, API headers, and endpoints configuration.
 */
@Component({
  standalone: true,
  selector: 'app-create-source-system',
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    ButtonModule,
    TextareaModule,
    ManageApiHeadersComponent,
    ManageEndpointsComponent,
    StepsModule,
    FileUploadModule,
    TreeModule,
    CheckboxModule,
    AasElementDialogComponent
  ]
})
export class CreateSourceSystemComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() sourceSystem: SourceSystemDTO | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();



  steps: Array<{label: string}> = [];
  currentStep = 0;
  createdSourceSystemId!: number;

  form!: FormGroup;
  selectedFile: File | null = null;
  fileSelected = false;

  // AASX upload state
  showAasxUpload = false;
  aasxSelectedFile: File | null = null;
  isUploadingAasx = false;
  aasxPreview: any = null;
  aasxSelection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> } = { submodels: [] };

  typeOptions: Array<{label: string, value: string}> = [];
  authTypeOptions: Array<{label: string, value: ApiAuthType}> = [];
  public readonly ApiAuthType = ApiAuthType;

  /**
   * @param fb FormBuilder for reactive form creation.
   * @param sourceSystemService Service to communicate with the SourceSystem backend API.
   */
  constructor(
    private fb: FormBuilder,
    private sourceSystemService: SourceSystemResourceService,
    protected errorService: HttpErrorService,
    private aasService: AasService,
    private messageService: MessageService,
    private formService: CreateSourceSystemFormService,
    private aasManagementService: CreateSourceSystemAasService,
    private dialogService: CreateSourceSystemDialogService
  ) {
    // Initialize after services are injected
    this.steps = this.formService.getSteps();
    this.typeOptions = this.formService.getTypeOptions();
    this.authTypeOptions = this.formService.getAuthTypeOptions();
  }

  /**
   * Initialize reactive form with metadata fields and validators.
   * Subscribes to authentication type changes to adjust validators dynamically.
   */
  ngOnInit(): void {
    this.form = this.formService.createForm();
    this.formService.setupFormSubscriptions(this.form);

    // Custom step management for AAS vs REST
    this.form.get('apiType')!.valueChanges.subscribe((apiType: string) => {
      if (apiType === 'AAS') {
        this.steps = [
          {label: 'Metadaten & Test'},
          {label: 'Api Header'},
          {label: 'AAS Submodels'}
        ];
      } else {
        this.steps = this.formService.getSteps();
      }
    });

    // If editing, populate form
    if (this.sourceSystem) {
      this.formService.populateForm(this.form, this.sourceSystem);
    }
  }

  /**
   * Reacts to incoming changes of the sourceSystem Input.
   * If editing an existing system, patches form values and resets to first step.
   *
   * @param changes Object containing changed @Input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sourceSystem'] && this.sourceSystem && this.form) {
      this.formService.populateForm(this.form, this.sourceSystem);
      this.currentStep = 0;
    }
  }

  /**
   * Handles file selection for OpenAPI spec upload.
   * Disables the URL input when a file is chosen.
   *
   * @param ev File input change event.
   */
  onFileSelected(event: FileSelectEvent): void {
    this.selectedFile = event.files[0];
    this.fileSelected = true;
    this.form.get('openApiSpec')!.disable();
  }

  // AASX upload handlers
  openAasxUpload(): void {
    this.showAasxUpload = true;
    this.aasxSelectedFile = null;
  }
  onAasxFileSelected(event: FileSelectEvent): void {
    this.aasxSelectedFile = event.files?.[0] || null;
    if (this.aasxSelectedFile) {
      
      // Load preview to enable selective attach
      if (this.createdSourceSystemId) {
        this.aasService.previewAasx(this.createdSourceSystemId, this.aasxSelectedFile).subscribe({
          next: (resp) => {
            this.aasxPreview = resp?.submodels || (resp?.result ?? []);
            // Normalize to array of {id,idShort,kind,elements:[{idShort,modelType}]}
            const arr = Array.isArray(this.aasxPreview) ? this.aasxPreview : (this.aasxPreview?.submodels ?? []);
            this.aasxSelection = { submodels: (arr || []).map((sm: any) => ({ id: sm.id || sm.submodelId, full: true, elements: (sm.elements || []).map((e: any) => e.idShort) })) };
            
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
          error: (err) => {
            
            this.aasxPreview = null;
            this.aasxSelection = { submodels: [] };
          }
        });
      }
    } else {
      
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
    if (checked) {
      if (!exists) sel.elements.push(idShort);
    } else {
      if (exists) sel.elements = sel.elements.filter((x) => x !== idShort);
    }
  }
  uploadAasx(): void {
    if (this.isUploadingAasx) return;
    if (!this.aasxSelectedFile) {
      this.messageService.add({ severity: 'warn', summary: 'No file selected', detail: 'Please choose an .aasx file.' });
      return;
    }
    const proceed = () => {
      if (!this.createdSourceSystemId) return;
      
      this.messageService.add({ severity: 'info', summary: 'Uploading AASX', detail: `${this.aasxSelectedFile?.name} (${this.aasxSelectedFile?.size} bytes)` });
      this.isUploadingAasx = true;
      // If preview is available and user made a selection, use selective attach; else default upload
      const hasSelection = (this.aasxSelection?.submodels?.some(s => s.full || (s.elements && s.elements.length > 0)) ?? false);
      const req$ = hasSelection ? this.aasService.attachSelectedAasx(this.createdSourceSystemId, this.aasxSelectedFile!, this.aasxSelection) : this.aasService.uploadAasx(this.createdSourceSystemId, this.aasxSelectedFile!);
      req$
        .subscribe({
          next: (resp) => {
            
            this.isUploadingAasx = false;
            this.showAasxUpload = false;
            // Directly rediscover from snapshot (do not refresh: would wipe imported AASX structures)
            
            this.discoverSubmodels();
            this.messageService.add({ severity: 'success', summary: 'Upload accepted', detail: 'AASX uploaded. Snapshot refresh started.' });
          },
          error: (err) => {
            
            this.isUploadingAasx = false;
            this.messageService.add({ severity: 'error', summary: 'Upload failed', detail: (err?.message || 'See console for details') });
            this.errorService.handleError(err);
          }
        });
    };
    if (!this.createdSourceSystemId) {
      const base = { ...this.form.getRawValue() } as CreateSourceSystemDTO;
      delete (base as any).authConfig;
      const authType = this.form.get('apiAuthType')!.value as ApiAuthType;
      const cfg = this.form.get('authConfig')!.value;
      if (authType === ApiAuthType.Basic) {
        base.authConfig = { authType, username: cfg.username, password: cfg.password } as BasicAuthDTO;
      } else if (authType === ApiAuthType.ApiKey) {
        base.authConfig = { authType, apiKey: cfg.apiKey, headerName: cfg.headerName } as ApiKeyAuthDTO;
      }
      delete (base as any).openApiSpec;
      this.postDto(base, { advanceStep: false, onSuccess: () => proceed() });
    } else {
      proceed();
    }
  }

  /**
   * Cancels creation/edit, resets form and component state, and closes the dialog.
   */
  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.form.reset({apiType: 'REST_OPENAPI', apiAuthType: null});
    this.selectedFile = null;
    this.fileSelected = false;
    this.form.get('openApiSpec')!.enable();
    this.currentStep = 0;
  }

/**
 * Validates form, constructs DTO including authConfig and optional file blob or URL,
 * and triggers POST to create the Source System.
 */
save(): void {
  if (this.form.invalid) {
    this.form.markAllAsTouched();
    return;
  }

  const base = {...this.form.getRawValue()} as CreateSourceSystemDTO;
  delete base.authConfig;

  const authType = this.form.get('apiAuthType')!.value as ApiAuthType;
  const cfg = this.form.get('authConfig')!.value;
  if (authType === ApiAuthType.Basic) {
    base.authConfig = {authType, username: cfg.username, password: cfg.password} as BasicAuthDTO;
  } else if (authType === ApiAuthType.ApiKey) {
    base.authConfig = {authType, apiKey: cfg.apiKey, headerName: cfg.headerName} as ApiKeyAuthDTO;
  }

  const post = () => {
    this.postDto(base);
  };

  if (this.selectedFile) {
    delete base.openApiSpec;
    const reader = new FileReader();
    reader.onload = () => {
      const fileContent = reader.result as string;
      base.openApiSpec = fileContent;
      post();
    };
    reader.readAsText(this.selectedFile);

  } else {
    const openApiSpecValue = base.openApiSpec;
    if (openApiSpecValue && typeof openApiSpecValue === 'string' && openApiSpecValue.trim()) {
      base.openApiSpec = openApiSpecValue;
      post();
    } else {
      delete base.openApiSpec;
      post();
    }
  }
}

  /**
   * Performs HTTP POST to persist the Source System and handles Location header parsing.
   *
   * @param dto Prepared DTO for creation.
   */
  private postDto(dto: CreateSourceSystemDTO, opts?: { advanceStep?: boolean, onSuccess?: (resp: SourceSystemDTO) => void }): void {
    
    
    this.sourceSystemService
      .apiConfigSourceSystemPost(dto)
      .subscribe({
        next: (resp: SourceSystemDTO) => {
          
          
          this.createdSourceSystemId = resp.id!;
          if (opts?.advanceStep !== false) {
            this.currentStep = 1;
          }
          if (opts?.onSuccess) {
            opts.onSuccess(resp);
          }
        },
        error: (err) => {
          
          this.errorService.handleError(err);
        }
      });
  }

  isAas(): boolean {
    return this.form.get('apiType')!.value === 'AAS';
  }

  isRest(): boolean {
    return this.form.get('apiType')!.value === 'REST_OPENAPI';
  }

  // AAS Test Preview State
  isTesting = false;
  aasPreview: { idShort?: string; assetKind?: string } | null = null;
  aasTestOk: boolean | null = null;
  aasError: string | null = null;

  testAasConnection(): void {
    if (this.isTesting) return;
    this.isTesting = true;
    if (!this.createdSourceSystemId) {
      // create silently (no step advance), then re-run test
      const base = { ...this.form.getRawValue() } as CreateSourceSystemDTO;
      delete (base as any).authConfig;
      const authType = this.form.get('apiAuthType')!.value as ApiAuthType;
      const cfg = this.form.get('authConfig')!.value;
      if (authType === ApiAuthType.Basic) {
        base.authConfig = { authType, username: cfg.username, password: cfg.password } as BasicAuthDTO;
      } else if (authType === ApiAuthType.ApiKey) {
        base.authConfig = { authType, apiKey: cfg.apiKey, headerName: cfg.headerName } as ApiKeyAuthDTO;
      }
      delete (base as any).openApiSpec;
      this.postDto(base, { advanceStep: false, onSuccess: () => { this.isTesting = false; this.testAasConnection(); } });
      return;
    }
    this.aasError = null;
    this.aasPreview = null;
    this.aasTestOk = null;
    this.aasService.aasTest(this.createdSourceSystemId).subscribe({
      next: (data) => {
        this.isTesting = false;
        if (data && data.idShort) {
          this.aasPreview = { idShort: data.idShort, assetKind: data.assetKind };
          this.aasTestOk = true;
          this.messageService.add({ severity: 'success', summary: 'Connection successful', detail: `Shell reachable (${data.idShort})`, life: 3000 });
        } else {
          this.aasPreview = null;
          this.aasTestOk = true;
          this.messageService.add({ severity: 'success', summary: 'Connection successful', detail: 'Shell reachable', life: 3000 });
        }
        // ensure snapshot contains idShort for submodels by refreshing once after a successful test
        this.aasService.refreshSnapshot(this.createdSourceSystemId).subscribe({ next: () => {}, error: () => {} });
      },
      error: (err) => {
        this.isTesting = false;
        const status = err?.status;
        let detail = 'Please verify: Base URL, AAS ID (original, not Base64 in metadata), auth configuration, and that the BaSyx server is reachable.';
        switch (status) {
          case 401:
            detail = '401 Unauthorized – check Basic/API Key credentials.';
            break;
          case 403:
            detail = '403 Forbidden – check permissions/token.';
            break;
          case 404:
            detail = '404 Not Found – verify Base URL or AAS ID (AAS does not exist or wrong ID).';
            break;
          case 405:
            detail = '405 Method Not Allowed – verify HTTP method (expected: POST /test).';
            break;
          case 409:
            detail = '409 Conflict – conflicting input or already exists.';
            break;
          case 504:
            detail = '504 Gateway Timeout – upstream not reachable (is BaSyx running?).';
            break;
          default:
            break;
        }
        this.aasError = `Connection failed. ${detail}`;
        this.messageService.add({ severity: 'error', summary: 'Connection failed', detail, life: 5000 });
        this.aasTestOk = false;
      }
    });
  }

  canProceedFromStep1(): boolean {
    if (this.isRest()) {
      return !this.form.invalid;
    }
    // For AAS: require valid form and successful test
    return !this.form.invalid && this.aasTestOk === true;
  }

  // AAS Step 3 rich tree
  submodels: any[] = [];
  treeNodes: TreeNode[] = [];
  isDiscovering = false;
  discoverSubmodels(): void {
    if (!this.createdSourceSystemId) {
      // Ensure we have an ID (if user skipped save and went straight to discover)
      const base = { ...this.form.getRawValue() } as CreateSourceSystemDTO;
      delete (base as any).authConfig;
      const authType = this.form.get('apiAuthType')!.value as ApiAuthType;
      const cfg = this.form.get('authConfig')!.value;
      if (authType === ApiAuthType.Basic) {
        base.authConfig = { authType, username: cfg.username, password: cfg.password } as BasicAuthDTO;
      } else if (authType === ApiAuthType.ApiKey) {
        base.authConfig = { authType, apiKey: cfg.apiKey, headerName: cfg.headerName } as ApiKeyAuthDTO;
      }
      delete (base as any).openApiSpec;
      this.postDto(base, { advanceStep: false, onSuccess: () => this.discoverSubmodels() });
      return;
    }
    this.isDiscovering = true;
    // Ensure snapshot is fresh so idShorts are available
    this.aasService.refreshSnapshot(this.createdSourceSystemId).subscribe({ next: () => {}, error: () => {} });
    this.aasService.listSubmodels(this.createdSourceSystemId, 'SNAPSHOT').subscribe({
      next: (resp) => {
        this.isDiscovering = false;
        // handle both list and paged result
        this.submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
        this.treeNodes = this.submodels.map((sm: any) => this.mapSubmodelToNode(sm));
      },
      error: (err) => {
        this.isDiscovering = false;
        this.errorService.handleError(err);
      }
    });
  }

  // Lazy tree state for elements
  elementsBySubmodel: Record<string, any[]> = {};
  childrenLoading: Record<string, boolean> = {};
  selectedLivePanel: ElementLivePanel | null = null;
  selectedLiveLoading = false;
  selectedNode?: TreeNode;

  // Cache: submodelId -> (idShortPath -> modelType)
  private typeCache: Record<string, Record<string, string>> = {};
  private ensureTypeMap(submodelId: string): void {
    if (!this.createdSourceSystemId) return;
    if (this.typeCache[submodelId]) {
      this.applyTypeMapToTree(submodelId);
      return;
    }
    this.aasService
      .listElements(this.createdSourceSystemId, submodelId, { depth: 'all', source: 'LIVE' })
      .subscribe({
        next: (resp) => {
          const list: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const map: Record<string, string> = {};
          for (const el of list) {
            const p = el?.idShortPath || el?.idShort;
            const t = this.inferModelType(el);
            if (p && t) map[p] = t;
          }
          this.typeCache[submodelId] = map;
          this.applyTypeMapToTree(submodelId);
        },
        error: () => {}
      });
  }
  private applyTypeMapToTree(submodelId: string): void {
    const map = this.typeCache[submodelId];
    if (!map) return;
    const applyMap = (nodes?: TreeNode[]) => {
      if (!nodes) return;
      for (const n of nodes) {
        if (n?.data?.type === 'element' && n.data?.submodelId === submodelId) {
          const p: string = n.data.idShortPath || n.data.raw?.idShortPath || n.data.raw?.idShort;
          const mt = map[p];
          if (mt) n.data.modelType = mt;
        }
        if (n.children && n.children.length) applyMap(n.children as TreeNode[]);
      }
    };
    applyMap(this.treeNodes);
    this.treeNodes = [...this.treeNodes];
  }

  private hydrateNodeTypesForNodes(submodelId: string, nodes: TreeNode[] | undefined): void {
    if (!this.createdSourceSystemId || !nodes || nodes.length === 0) return;
    for (const n of nodes) {
      if (n?.data?.type === 'element' && n.data?.submodelId === submodelId) {
        const path: string = n.data.idShortPath || n.data.raw?.idShortPath || n.data.raw?.idShort;
        if (!path) continue;
        this.aasService.getElement(this.createdSourceSystemId, submodelId, path, 'LIVE').subscribe({
          next: (el: any) => {
            const liveType = el?.modelType || this.inferModelType(el);
            if (liveType) {
              n.data.modelType = liveType;
              this.treeNodes = [...this.treeNodes];
            }
          },
          error: () => {}
        });
      }
    }
  }

  loadRootElements(submodelId: string, attachToNode?: TreeNode): void {
    if (!this.createdSourceSystemId) return;
    this.childrenLoading[submodelId] = true;
    this.aasService.listElements(this.createdSourceSystemId, submodelId, { depth: 'shallow', source: 'SNAPSHOT' })
      .subscribe({
        next: (resp) => {
          this.childrenLoading[submodelId] = false;
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          this.elementsBySubmodel[submodelId] = list;
          if (attachToNode) {
            attachToNode.children = list.map((el: any) => this.mapElementToNode(submodelId, el));
            this.treeNodes = [...this.treeNodes];
            // Background: hydrate precise types via LIVE element details
            this.hydrateNodeTypesForNodes(submodelId, attachToNode.children as TreeNode[]);
          }
          // hydrate types in background
          this.ensureTypeMap(submodelId);
        },
        error: (err) => {
          this.childrenLoading[submodelId] = false;
          this.errorService.handleError(err);
        }
      });
  }

  loadChildren(submodelId: string, parentPath: string, node: TreeNode): void {
    if (!this.createdSourceSystemId) return;
    const key = `${submodelId}::${parentPath}`;
    this.childrenLoading[key] = true;
    this.aasService.listElements(this.createdSourceSystemId, submodelId, { depth: 'shallow', parentPath, source: 'SNAPSHOT' })
      .subscribe({
        next: (resp) => {
          this.childrenLoading[key] = false;
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          
          // Filter for direct children only to prevent duplicates
          const prefix = parentPath ? (parentPath.endsWith('/') ? parentPath : parentPath + '/') : '';
          const filteredList = list.filter((el: any) => {
            const p = el?.idShortPath || el?.idShort;
            if (!p) return false;
            
            // If no parentPath, include all root elements
            if (!parentPath) {
              return !String(p).includes('/');
            }
            
            // For elements with parentPath, check if they are direct children
            if (!String(p).startsWith(prefix)) return false;
            const relativePath = String(p).substring(prefix.length);
            
            // Include if it's a direct child (no additional slashes)
            return relativePath && !relativePath.includes('/');
          });
          
          console.log('[SourceCreate] loadChildren: Filtered children', {
            submodelId,
            parentPath,
            prefix,
            originalCount: list.length,
            filteredCount: filteredList.length,
            original: list.map((el: any) => ({
              idShort: el.idShort,
              idShortPath: el.idShortPath,
              hasChildren: el.hasChildren
            })),
            filtered: filteredList.map((el: any) => ({
              idShort: el.idShort,
              idShortPath: el.idShortPath,
              hasChildren: el.hasChildren
            }))
          });
          
          const mapped = filteredList.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElementToNode(submodelId, el);
          });
          node.children = mapped;
          this.treeNodes = [...this.treeNodes];
          // Background: hydrate precise types for these children
          this.hydrateNodeTypesForNodes(submodelId, node.children as TreeNode[]);
          // hydrate types in background
          this.ensureTypeMap(submodelId);
        },
        error: (err) => {
          this.childrenLoading[key] = false;
          this.errorService.handleError(err);
        }
      });
  }

  // Tree mapping helpers
  private inferModelType(el: any): string | undefined {
    if (!el) return undefined;
    
    // Direct modelType is most reliable
    if (el.modelType) return el.modelType;
    
    // Check for 'type' field (common in AAS data)
    if (el.type) {
      console.log('[SourceCreate] inferModelType - Using type field for:', el.idShort, 'type:', el.type);
      return el.type;
    }
    
    console.log('[SourceCreate] inferModelType - Analyzing element:', el.idShort, 'Raw data:', el);
    
    // Detect Range before Property (Range often also has valueType for endpoint types)
    if (el.min !== undefined || el.max !== undefined || el.minValue !== undefined || el.maxValue !== undefined) {
      console.log('[SourceCreate] inferModelType - Detected Range for:', el.idShort);
      return 'Range';
    }
    
    // Property detection - most common type
    if (el.valueType) {
      console.log('[SourceCreate] inferModelType - Detected Property for:', el.idShort, 'valueType:', el.valueType);
      return 'Property';
    }
    
    // Operation detection
    if (Array.isArray(el.inputVariables) || Array.isArray(el.outputVariables) || Array.isArray(el.inoutputVariables)) {
      console.log('[SourceCreate] inferModelType - Detected Operation for:', el.idShort);
      return 'Operation';
    }
    
    // Array-based types
    if (Array.isArray(el.value)) {
      const isML = el.value.every((v: any) => v && (v.language !== undefined) && (v.text !== undefined));
      if (isML) {
        console.log('[SourceCreate] inferModelType - Detected MultiLanguageProperty for:', el.idShort);
        return 'MultiLanguageProperty';
      }
      if (el.typeValueListElement || el.orderRelevant !== undefined) {
        console.log('[SourceCreate] inferModelType - Detected SubmodelElementList for:', el.idShort);
        return 'SubmodelElementList';
      }
      console.log('[SourceCreate] inferModelType - Detected SubmodelElementCollection for:', el.idShort);
      return 'SubmodelElementCollection';
    }
    
    // Relationship elements
    if (el.first || el.firstReference) {
      const ann = el.annotations || el.annotation;
      const isAnnotated = Array.isArray(ann);
      console.log('[SourceCreate] inferModelType - Detected RelationshipElement for:', el.idShort, 'annotated:', isAnnotated);
      return isAnnotated ? 'AnnotatedRelationshipElement' : 'RelationshipElement';
    }
    
    // Annotated relationship
    if (Array.isArray(el.annotations) || Array.isArray(el.annotation)) {
      console.log('[SourceCreate] inferModelType - Detected AnnotatedRelationshipElement for:', el.idShort);
      return 'AnnotatedRelationshipElement';
    }
    
    // Entity
    if (Array.isArray(el.statements)) {
      console.log('[SourceCreate] inferModelType - Detected Entity for:', el.idShort);
      return 'Entity';
    }
    
    // Reference element
    if (Array.isArray(el.keys)) {
      console.log('[SourceCreate] inferModelType - Detected ReferenceElement for:', el.idShort);
      return 'ReferenceElement';
    }
    
    // File
    if (el.contentType && (el.fileName || el.path)) {
      console.log('[SourceCreate] inferModelType - Detected File for:', el.idShort);
      return 'File';
    }
    
    // Additional fallback checks for common patterns
    if (el.value !== undefined && el.value !== null) {
      console.log('[SourceCreate] inferModelType - Fallback to Property for:', el.idShort, 'value:', el.value);
      return 'Property';
    }
    
    // Check for submodel elements with children
    if (el.hasChildren === true || el.submodelElements || el.items) {
      console.log('[SourceCreate] inferModelType - Fallback to SubmodelElementCollection for:', el.idShort);
      return 'SubmodelElementCollection';
    }
    
    console.log('[SourceCreate] inferModelType - Could not determine type for:', el.idShort, 'returning undefined');
    return undefined;
  }
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
        console.log('[SourceCreate] mapElementToNode - Collection/List:', label, 'hasItems:', hasItems, 'value:', el?.value, 'submodelElements:', el?.submodelElements);
      } else {
        // Für andere Typen (Operation, Entity): Standard-Logik
        hasChildren = el?.hasChildren === true || typeHasChildren;
      }
    } else {
      hasChildren = el?.hasChildren === true;
    }
    
    return {
      key: `${submodelId}::${el.idShortPath}`,
      label,
      data: { type: 'element', submodelId, idShortPath: el.idShortPath || el.idShort, modelType: computedType, raw: el },
      leaf: !hasChildren,
      children: []
    } as TreeNode;
  }

  onNodeExpand(event: any): void {
    const node: TreeNode = event.node;
    if (!node) return;
    
    // Don't expand if node is a leaf (no children)
    if (node.leaf) {
      console.log('[SourceCreate] onNodeExpand - Node is leaf, skipping expansion');
      return;
    }
    
    if (node.data?.type === 'submodel') {
      this.loadRootElements(node.data.id, node);
    } else if (node.data?.type === 'element') {
      const { submodelId, idShortPath } = node.data;
      this.loadChildren(submodelId, idShortPath, node);
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
    // Smooth scroll details into view
    setTimeout(() => {
      const el = document.getElementById('element-details');
      if (el && el.scrollIntoView) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 0);
  }

  private loadLiveElementDetails(smId: string, idShortPath: string | undefined, node?: TreeNode): void {
    console.log('[SourceCreate] loadLiveElementDetails called with:', { smId, idShortPath, node: node?.label });
    if (!this.createdSourceSystemId) return;
    this.selectedLiveLoading = true;
    const keyStr = (node && typeof node.key === 'string') ? (node.key as string) : '';
    const keyPath = keyStr.includes('::') ? keyStr.split('::')[1] : '';
    const safePath = (idShortPath || keyPath || (node?.data?.raw?.idShort || '')).replace(/\/+/g, '/');
    const last = safePath.split('/').pop() as string;
    const parent = safePath.includes('/') ? safePath.substring(0, safePath.lastIndexOf('/')) : '';
    // Robust load: try direct element endpoint (backend has deep fallback)
    console.log('[SourceCreate] Calling aasService.getElement with:', { 
      systemId: this.createdSourceSystemId, 
      smId, 
      safePath, 
      source: 'LIVE' 
    });
    
    this.aasService.getElement(this.createdSourceSystemId, smId, safePath, 'LIVE').subscribe({
      next: (found: any) => {
        console.log('[SourceCreate] aasService.getElement response:', found);
        this.selectedLiveLoading = false;
        // Use the same logic as inferModelType for consistent type detection
        let liveType = found?.modelType || found?.type || (found?.valueType ? 'Property' : undefined);
        
        // Fallback: Use inferModelType method for consistent type detection
        if (!liveType) {
          liveType = this.inferModelType(found);
        }
        
        console.log('[SourceCreate] loadLiveElementDetails - Element:', found.idShort, 'liveType:', liveType, 'found:', found);
        const minValue = (found as any).min ?? (found as any).minValue;
        const maxValue = (found as any).max ?? (found as any).maxValue;
        const inputVars = Array.isArray((found as any).inputVariables) ? (found as any).inputVariables : [];
        const outputVars = Array.isArray((found as any).outputVariables) ? (found as any).outputVariables : [];
        const inoutVars = Array.isArray((found as any).inoutputVariables) ? (found as any).inoutputVariables : [];
        const ann1 = (found as any).annotations;
        const ann2 = (found as any).annotation;
        const annotationsRaw = Array.isArray(ann1) ? ann1 : (Array.isArray(ann2) ? ann2 : []);
        const mapVar = (v: any): OperationVarView | null => {
          const val = v?.value ?? v;
          const idShort = val?.idShort;
          if (!idShort) return null;
          return { idShort, modelType: val?.modelType, valueType: val?.valueType };
        };
        const mapAnnotation = (a: any): AnnotationView | null => {
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
        this.selectedLivePanel = {
          label: found.idShort,
          type: liveType || 'Unknown',
          value: (found as any).value,
          valueType: (found as any).valueType,
          min: minValue,
          max: maxValue,
          inputVariables: inputVars.map(mapVar).filter(Boolean) as OperationVarView[],
          outputVariables: outputVars.map(mapVar).filter(Boolean) as OperationVarView[],
          inoutputVariables: inoutVars.map(mapVar).filter(Boolean) as OperationVarView[],
          firstRef,
          secondRef,
          annotations: annotationsRaw.map(mapAnnotation).filter(Boolean) as AnnotationView[]
        } as any;
        
        console.log('[SourceCreate] selectedLivePanel created:', this.selectedLivePanel);
        // Fallback: If AnnotatedRelationshipElement has no annotations in direct payload, load children as annotations
        if ((liveType === 'AnnotatedRelationshipElement') && (((this.selectedLivePanel?.annotations?.length ?? 0) === 0))) {
          const pathForChildren = safePath;
          // Try deep list to get full element (with annotations)
          this.aasService
            .listElements(this.createdSourceSystemId!, smId, { depth: 'all', source: 'LIVE' })
            .subscribe({
              next: (resp: any) => {
                const arr: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
                const foundDeep = arr.find((el: any) => (el?.idShortPath || el?.idShort) === pathForChildren);
                const anns = Array.isArray(foundDeep?.annotations) ? foundDeep.annotations : (Array.isArray(foundDeep?.annotation) ? foundDeep.annotation : []);
                let annotations: AnnotationView[] = [];
                if (anns.length) {
                  annotations = anns.map((a: any) => ({ idShort: a?.idShort, modelType: a?.modelType, valueType: a?.valueType, value: a?.value } as AnnotationView));
                } else {
                  // Fallback: treat shallow children as annotations
                  const list: any[] = arr.filter((el: any) => {
                    const p = el?.idShortPath || el?.idShort;
                    if (!p || !p.startsWith(pathForChildren + '/')) return false;
                    const rest = p.substring((pathForChildren + '/').length);
                    return rest && !rest.includes('/');
                  });
                  annotations = list.map((el: any) => ({ idShort: el?.idShort, modelType: el?.modelType, valueType: el?.valueType, value: el?.value } as AnnotationView));
                }
                if (this.selectedLivePanel) {
                  this.selectedLivePanel = { ...this.selectedLivePanel, annotations };
                }
              },
              error: () => {
              }
            });
        }
        if (node && node.data) {
          const computedPath = safePath;
          node.data.idShortPath = computedPath;
          node.data.modelType = liveType || node.data.modelType;
          node.data.raw = { ...(node.data.raw || {}), idShortPath: computedPath, modelType: found.modelType, valueType: found.valueType };
          this.treeNodes = [...this.treeNodes];
        }
      },
      error: (err: any) => {
        console.log('[SourceCreate] aasService.getElement error:', err);
        this.selectedLiveLoading = false;
        
        // DIRECT FALLBACK: Use tree node data immediately
        console.log('[SourceCreate] DIRECT FALLBACK: Using tree node data immediately');
        if (node?.data?.raw) {
          const rawData = node.data.raw;
          const directType = rawData?.modelType || rawData?.type || this.inferModelType(rawData) || 'Unknown';
          console.log('[SourceCreate] DIRECT FALLBACK: rawData:', rawData, 'type:', directType, 'value:', rawData.value);
          console.log('[SourceCreate] DIRECT FALLBACK: node.data.raw:', node.data.raw);
          
          // Extract value from raw data - check multiple possible locations
          let extractedValue = rawData.value;
          console.log('[SourceCreate] DIRECT FALLBACK: rawData.value:', rawData.value);
          console.log('[SourceCreate] DIRECT FALLBACK: rawData.valueType:', rawData.valueType);
          
          // Don't use valueType as value - it's metadata, not the actual value
          if (extractedValue === rawData.valueType) {
            console.log('[SourceCreate] DIRECT FALLBACK: Value is same as valueType, this is wrong - setting to undefined');
            extractedValue = undefined;
          }
          
          if (!extractedValue && rawData.raw && rawData.raw.value) {
            extractedValue = rawData.raw.value;
            console.log('[SourceCreate] DIRECT FALLBACK: Found in rawData.raw.value:', extractedValue);
          }
          if (!extractedValue && node.data.raw && node.data.raw.value) {
            extractedValue = node.data.raw.value;
            console.log('[SourceCreate] DIRECT FALLBACK: Found in node.data.raw.value:', extractedValue);
          }
          
          // Additional check: look for value in the entire node structure
          if (!extractedValue) {
            console.log('[SourceCreate] DIRECT FALLBACK: Searching entire node structure for value...');
            console.log('[SourceCreate] DIRECT FALLBACK: node.data:', node.data);
            console.log('[SourceCreate] DIRECT FALLBACK: node.data.raw:', node.data.raw);
            
            // Check if value is nested deeper
            if (node.data.raw && typeof node.data.raw === 'object') {
              for (const key in node.data.raw) {
                if (key.toLowerCase().includes('value') && node.data.raw[key] && node.data.raw[key] !== rawData.valueType) {
                  extractedValue = node.data.raw[key];
                  console.log('[SourceCreate] DIRECT FALLBACK: Found value in key:', key, 'value:', extractedValue);
                  break;
                }
              }
            }
          }
          
          // Use only real values - no mock values
          if (extractedValue) {
            console.log('[SourceCreate] DIRECT FALLBACK: Using real value found:', extractedValue);
          } else {
            console.log('[SourceCreate] DIRECT FALLBACK: No value found for element type:', directType);
          }
          
          console.log('[SourceCreate] DIRECT FALLBACK: Final extractedValue:', extractedValue);
          
          // SIMPLE TEST: Create a basic panel first
          this.selectedLivePanel = {
            label: rawData.idShort || last,
            type: directType,
            value: extractedValue,
            valueType: rawData.valueType,
            min: rawData.min,
            max: rawData.max,
            inputVariables: [],
            outputVariables: [],
            inoutputVariables: [],
            firstRef: undefined,
            secondRef: undefined,
            annotations: []
          } as any;
          
          console.log('[SourceCreate] DIRECT FALLBACK selectedLivePanel created:', this.selectedLivePanel);
          
          // Force change detection
          setTimeout(() => {
            console.log('[SourceCreate] DIRECT FALLBACK - selectedLivePanel after timeout:', this.selectedLivePanel);
            console.log('[SourceCreate] DIRECT FALLBACK - selectedNode:', this.selectedNode);
            console.log('[SourceCreate] DIRECT FALLBACK - selectedLiveLoading:', this.selectedLiveLoading);
          }, 100);
          
          return; // Skip the complex fallback logic
        }
        
        // Fallback: list under parent shallow
        console.log('[SourceCreate] Fallback: listElements with:', { 
          systemId: this.createdSourceSystemId, 
          smId, 
          parentPath: parent || undefined, 
          source: 'LIVE',
          lookingFor: last
        });
        
        this.aasService
          .listElements(this.createdSourceSystemId, smId, { depth: 'shallow', parentPath: parent || undefined, source: 'LIVE' })
          .subscribe({
            next: (resp: any) => {
              console.log('[SourceCreate] Fallback listElements response:', resp);
              this.selectedLiveLoading = false;
              const list: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
              console.log('[SourceCreate] Fallback: looking for element with idShort:', last, 'in list:', list.map(el => el.idShort));
              const found2 = list.find((el: any) => el.idShort === last);
              if (found2) {
                const liveType = found2?.modelType || (found2?.valueType ? 'Property' : undefined);
                const minValue = (found2 as any).min ?? (found2 as any).minValue;
                const maxValue = (found2 as any).max ?? (found2 as any).maxValue;
                const inputVars2 = Array.isArray((found2 as any).inputVariables) ? (found2 as any).inputVariables : [];
                const outputVars2 = Array.isArray((found2 as any).outputVariables) ? (found2 as any).outputVariables : [];
                const inoutVars2 = Array.isArray((found2 as any).inoutputVariables) ? (found2 as any).inoutputVariables : [];
                const mapVar2 = (v: any): OperationVarView | null => {
                  const val = v?.value ?? v;
                  const idShort = val?.idShort;
                  if (!idShort) return null;
                  return { idShort, modelType: val?.modelType, valueType: val?.valueType };
                };
                const ann1b = (found2 as any).annotations;
                const ann2b = (found2 as any).annotation;
                const annotationsRaw2 = Array.isArray(ann1b) ? ann1b : (Array.isArray(ann2b) ? ann2b : []);
                const mapAnnotation2 = (a: any): AnnotationView | null => {
                  const val = a?.value ?? a;
                  const idShort = val?.idShort;
                  if (!idShort) return null;
                  return { idShort, modelType: val?.modelType, valueType: val?.valueType, value: val?.value };
                };
                const stringifyRef2 = (ref: any): string | undefined => {
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
                const firstRef2 = stringifyRef2((found2 as any).first || (found2 as any).firstReference);
                const secondRef2 = stringifyRef2((found2 as any).second || (found2 as any).secondReference);
                this.selectedLivePanel = {
                  label: found2.idShort,
                  type: liveType || 'Unknown',
                  value: (found2 as any).value,
                  valueType: (found2 as any).valueType,
                  min: minValue,
                  max: maxValue,
                  inputVariables: inputVars2.map(mapVar2).filter(Boolean) as OperationVarView[],
                  outputVariables: outputVars2.map(mapVar2).filter(Boolean) as OperationVarView[],
                  inoutputVariables: inoutVars2.map(mapVar2).filter(Boolean) as OperationVarView[],
                  firstRef: firstRef2,
                  secondRef: secondRef2,
                  annotations: annotationsRaw2.map(mapAnnotation2).filter(Boolean) as AnnotationView[]
                } as any;
                if (node && node.data) {
                  node.data.modelType = liveType || node.data.modelType;
                  this.treeNodes = [...this.treeNodes];
                }
              } else {
                console.log('[SourceCreate] Fallback: Element not found, trying alternative approach');
                // Alternative: Try to load the element with a different approach
                console.log('[SourceCreate] Fallback: Trying to load element with SNAPSHOT source');
                
                this.aasService.getElement(this.createdSourceSystemId, smId, safePath, 'SNAPSHOT').subscribe({
                  next: (snapshotData: any) => {
                    console.log('[SourceCreate] Fallback SNAPSHOT response:', snapshotData);
                    this.selectedLiveLoading = false;
                    
                    const snapshotType = snapshotData?.modelType || snapshotData?.type || this.inferModelType(snapshotData) || 'Unknown';
                    
                    this.selectedLivePanel = {
                      label: snapshotData.idShort || last,
                      type: snapshotType,
                      value: snapshotData.value,
                      valueType: snapshotData.valueType,
                      min: snapshotData.min,
                      max: snapshotData.max,
                      inputVariables: [],
                      outputVariables: [],
                      inoutputVariables: [],
                      firstRef: undefined,
                      secondRef: undefined,
                      annotations: []
                    } as any;
                    
                    console.log('[SourceCreate] Fallback SNAPSHOT selectedLivePanel created:', this.selectedLivePanel);
                  },
                  error: (snapshotErr: any) => {
                    console.log('[SourceCreate] Fallback SNAPSHOT error:', snapshotErr);
                    // Final fallback: Use tree node data
                    if (node?.data?.raw) {
                      const rawData = node.data.raw;
                      const fallbackType = rawData?.modelType || rawData?.type || this.inferModelType(rawData) || 'Unknown';
                      console.log('[SourceCreate] Final fallback: Using tree node data:', rawData, 'type:', fallbackType, 'value:', rawData.value);
                      
                      this.selectedLivePanel = {
                        label: rawData.idShort || last,
                        type: fallbackType,
                        value: rawData.value,
                        valueType: rawData.valueType,
                        min: rawData.min,
                        max: rawData.max,
                        inputVariables: [],
                        outputVariables: [],
                        inoutputVariables: [],
                        firstRef: undefined,
                        secondRef: undefined,
                        annotations: []
                      } as any;
                      
                      console.log('[SourceCreate] Final fallback selectedLivePanel created:', this.selectedLivePanel);
                    } else {
                      this.selectedLivePanel = { label: last, type: 'Unknown' };
                    }
                    this.selectedLiveLoading = false;
                  }
                });
              }
            },
            error: (err2: any) => {
              console.log('[SourceCreate] Fallback listElements error:', err2);
              this.selectedLiveLoading = false;
              this.selectedLivePanel = { label: last, type: 'Unknown' };
              this.errorService.handleError(err2);
            }
          });
      }
    });
  }

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
  setSubmodelTemplate(kind: 'minimal'|'property'|'collection'): void {
    if (kind === 'minimal') this.newSubmodelJson = this.minimalSubmodelTemplate;
    if (kind === 'property') this.newSubmodelJson = this.propertySubmodelTemplate;
    if (kind === 'collection') this.newSubmodelJson = this.collectionSubmodelTemplate;
  }
  openCreateSubmodel(): void { this.showSubmodelDialog = true; }
  createSubmodel(): void {
    if (!this.createdSourceSystemId) return;
    try {
      const body = JSON.parse(this.newSubmodelJson);
      this.aasService.createSubmodel(this.createdSourceSystemId, body).subscribe({
        next: () => {
          this.showSubmodelDialog = false;
          this.discoverSubmodels();
        },
        error: (err) => this.errorService.handleError(err)
      });
    } catch (e) {
      this.errorService.handleError(e as any);
    }
  }

  showElementDialog = false;
  elementDialogData: AasElementDialogData | null = null;
  openCreateElement(smId: string, parent?: string): void {
    if (!this.createdSourceSystemId) return;
    
    this.elementDialogData = {
      submodelId: smId,
      parentPath: parent,
      systemId: this.createdSourceSystemId,
      systemType: 'source'
    };
    this.showElementDialog = true;
  }

  onElementDialogResult(result: AasElementDialogResult): void {
    if (result.success && result.element) {
      this.handleElementCreation(result.element);
    } else if (result.error) {
      console.error('[SourceCreate] Element creation failed:', result.error);
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
    if (!this.createdSourceSystemId) return;
    
    try {
      console.log('[SourceCreate] Creating element:', elementData);
      
      // Use the AAS service to create the element
      const smIdB64 = this.aasService.encodeIdToBase64Url(elementData.submodelId);
      await this.aasService.createElement(
        this.createdSourceSystemId,
        smIdB64,
        elementData.body,
        elementData.parentPath
      ).toPromise();
      
      console.log('[SourceCreate] Element created successfully');
      
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
      console.error('[SourceCreate] Error creating element:', error);
      
      // Show error toast
      if (error && error.error && error.error.includes('Duplicate entry')) {
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
          detail: error?.error || error?.message || 'Failed to create element',
          life: 5000
        });
      }
    }
  }

  private findNodeByKey(key: string, nodes: TreeNode[] | undefined): TreeNode | null {
    if (!nodes) return null;
    for (const n of nodes) {
      if (n.key === key) return n;
      const found = this.findNodeByKey(key, n.children as TreeNode[]);
      if (found) return found;
    }
    return null;
  }

  private refreshNodeLive(submodelId: string, parentPath: string, node?: TreeNode): void {
    if (!this.createdSourceSystemId) {
      console.log('[SourceCreate] refreshNodeLive: No createdSourceSystemId');
      return;
    }
    
    const key = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    console.log('[SourceCreate] refreshNodeLive: Starting refresh', {
      submodelId,
      parentPath,
      key,
      node: node?.label
    });
    
    this.childrenLoading[key] = true;
    this.aasService
      .listElements(this.createdSourceSystemId, submodelId, { depth: 'shallow', parentPath: parentPath || undefined, source: 'LIVE' })
      .subscribe({
        next: (resp) => {
          console.log('[SourceCreate] refreshNodeLive: Response received', {
            key,
            response: resp,
            node: node?.label
          });
          
          this.childrenLoading[key] = false;
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const mapped = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElementToNode(submodelId, el);
          });
          
          console.log('[SourceCreate] refreshNodeLive: Mapped elements', {
            key,
            mappedCount: mapped.length,
            mapped: mapped.map((m: any) => m.label)
          });
          
          if (node) {
            node.children = mapped;
            console.log('[SourceCreate] refreshNodeLive: Updated node children', {
              nodeLabel: node.label,
              childrenCount: mapped.length
            });
          } else {
            // root
            const attachNode = this.findNodeByKey(submodelId, this.treeNodes);
            if (attachNode) {
              attachNode.children = mapped;
              console.log('[SourceCreate] refreshNodeLive: Updated root node children', {
                attachNodeLabel: attachNode.label,
                childrenCount: mapped.length
              });
            } else {
              console.log('[SourceCreate] refreshNodeLive: Root node not found for submodelId', submodelId);
            }
          }
          
          // Force tree update
          this.treeNodes = [...this.treeNodes];
          console.log('[SourceCreate] refreshNodeLive: Tree updated');
        },
        error: (err) => {
          console.error('[SourceCreate] refreshNodeLive: Error', {
            key,
            error: err
          });
          this.childrenLoading[key] = false;
          this.errorService.handleError(err);
        }
      });
  }

  deleteSubmodel(submodelId: string): void {
    // route through confirmation dialog
    this.deleteSubmodelId = submodelId;
    this.showDeleteSubmodelDialog = true;
  }

  deleteElement(submodelId: string, idShortPath: string): void {
    if (!this.createdSourceSystemId || !submodelId || !idShortPath) {
      console.log('[SourceCreate] deleteElement: Missing required data', {
        createdSourceSystemId: this.createdSourceSystemId,
        submodelId,
        idShortPath
      });
      return;
    }
    
    // Use the original submodelId, not base64 encoded
    // The backend will handle the encoding internally
    
    console.log('[SourceCreate] deleteElement: Deleting element', {
      systemId: this.createdSourceSystemId,
      submodelId,
      idShortPath
    });
    
    // Skip existence check for deep elements and proceed directly with deletion
    console.log('[SourceCreate] deleteElement: Proceeding directly with deletion (skipping existence check for deep elements)');
    this.performDelete(submodelId, idShortPath);
  }
  
  private performDelete(submodelId: string, idShortPath: string): void {
    this.aasService.deleteElement(this.createdSourceSystemId, submodelId, idShortPath).subscribe({
      next: () => {
        console.log('[SourceCreate] deleteElement: Element deleted successfully');
        
        // Trigger discover to refresh the entire tree
        console.log('[SourceCreate] deleteElement: Triggering discover to refresh tree');
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
        console.error('[SourceCreate] deleteElement: Error deleting element', err);
        console.log('[SourceCreate] deleteElement: Error details', {
          status: err.status,
          statusText: err.statusText,
          url: err.url,
          message: err.message,
          idShortPath,
          submodelId
        });
        
        // Check if it's a 404 error (element not found)
        if (err.status === 404) {
          console.log('[SourceCreate] deleteElement: Element not found, removing from tree anyway');
          
          // Remove the element from the tree directly
          this.removeElementFromTree(submodelId, idShortPath);
          
          this.messageService.add({
            severity: 'success',
            summary: 'Element Removed',
            detail: `Element '${idShortPath.split('.').pop()}' has been removed from the tree.`,
            life: 3000
          });
        } else {
          this.errorService.handleError(err);
        }
      }
    });
  }

  private removeElementFromTree(submodelId: string, idShortPath: string): void {
    console.log('[SourceCreate] removeElementFromTree: Removing element from tree', {
      submodelId,
      idShortPath
    });
    
    const elementKey = `${submodelId}::${idShortPath}`;
    const parentPath = idShortPath.includes('.') ? idShortPath.substring(0, idShortPath.lastIndexOf('.')) : '';
    const parentKey = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    
    console.log('[SourceCreate] removeElementFromTree: Keys', {
      elementKey,
      parentKey,
      parentPath
    });
    
    // Try to find and remove the element from the tree
    let elementRemoved = false;
    
    // Find the parent node
    const parentNode = this.findNodeByKey(parentKey, this.treeNodes);
    if (parentNode && parentNode.children) {
      console.log('[SourceCreate] removeElementFromTree: Parent node found', {
        parentLabel: parentNode.label,
        childrenCount: parentNode.children.length
      });
      
      // Remove the element from parent's children
      const initialLength = parentNode.children.length;
      parentNode.children = parentNode.children.filter(child => {
        const shouldKeep = child.key !== elementKey;
        if (!shouldKeep) {
          console.log('[SourceCreate] removeElementFromTree: Removing child', {
            childKey: child.key,
            childLabel: child.label
          });
        }
        return shouldKeep;
      });
      
      elementRemoved = initialLength > parentNode.children.length;
      console.log('[SourceCreate] removeElementFromTree: Element removed', {
        initialLength,
        finalLength: parentNode.children.length,
        removed: elementRemoved
      });
    }
    
    // Force tree update
    this.treeNodes = [...this.treeNodes];
    
    // If element wasn't found in the tree, refresh the parent node
    if (!elementRemoved) {
      console.log('[SourceCreate] removeElementFromTree: Element not found in tree, refreshing parent node');
      this.refreshNodeLive(submodelId, parentPath, parentNode || undefined);
    }
  }

  // Delete Submodel confirmation
  showDeleteSubmodelDialog = false;
  deleteSubmodelId: string | null = null;
  proceedDeleteSubmodel(): void {
    if (!this.createdSourceSystemId || !this.deleteSubmodelId) {
      this.showDeleteSubmodelDialog = false;
      return;
    }
    const smIdB64 = this.aasService.encodeIdToBase64Url(this.deleteSubmodelId);
    this.aasService.deleteSubmodel(this.createdSourceSystemId, smIdB64).subscribe({
      next: () => {
        this.showDeleteSubmodelDialog = false;
        this.deleteSubmodelId = null;
        this.discoverSubmodels();
        this.messageService.add({ severity: 'success', summary: 'Submodel deleted', detail: 'Submodel, elements, and shell reference removed.' });
      },
      error: (err) => {
        this.showDeleteSubmodelDialog = false;
        this.errorService.handleError(err);
      }
    });
  }

  // PATCH value dialog
  showValueDialog = false;
  valueSubmodelId = '';
  valueElementPath = '';
  valueNew = '';
  valueTypeHint = 'xs:string';
  openSetValue(smId: string, element: any): void {
    this.valueSubmodelId = smId;
    this.valueElementPath = element.idShortPath || element.data?.idShortPath || element.raw?.idShortPath || element.idShort;
    this.valueTypeHint = element.valueType || 'xs:string';
    // Prefill with current LIVE value if available
    if (this.selectedLivePanel && this.selectedNode && this.selectedNode.data?.idShortPath === this.valueElementPath) {
      this.valueNew = (this.selectedLivePanel.value ?? '').toString();
    } else {
      this.valueNew = '';
    }
    this.showValueDialog = true;
  }
  setValue(): void {
    if (!this.createdSourceSystemId || !this.valueSubmodelId || !this.valueElementPath) return;
    const smIdB64 = this.aasService.encodeIdToBase64Url(this.valueSubmodelId);
    const parsedValue = this.parseValueForType(this.valueNew, this.valueTypeHint);
    this.aasService.setPropertyValue(this.createdSourceSystemId, smIdB64, this.valueElementPath, parsedValue as any)
      .subscribe({
        next: () => {
          this.showValueDialog = false;
          // Refresh LIVE details of the selected node if matching
          if (this.selectedNode && this.selectedNode.data?.idShortPath === this.valueElementPath) {
            this.loadLiveElementDetails(this.valueSubmodelId, this.valueElementPath, this.selectedNode);
          } else {
            // Otherwise refresh parent listing
            const parent = this.valueElementPath.includes('/') ? this.valueElementPath.substring(0, this.valueElementPath.lastIndexOf('/')) : '';
            const parentNode = parent ? this.findNodeByKey(`${this.valueSubmodelId}::${parent}`, this.treeNodes) : this.findNodeByKey(this.valueSubmodelId, this.treeNodes);
            this.refreshNodeLive(this.valueSubmodelId, parent, parentNode || undefined);
          }
          this.messageService.add({ severity: 'success', summary: 'Value updated', detail: 'Property value saved', life: 2500 });
        },
        error: (err) => this.errorService.handleError(err)
      });
  }

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
    return raw; // default string
  }
  /**
   * Advances the stepper to the next step.
   * If on first step, saves the Source System before proceeding.
   */
  goNext(): void {
    if (this.currentStep === 0) {
      // Avoid duplicate creation: if already created (e.g., via Test), just advance
      if (this.createdSourceSystemId) {
        this.currentStep = 1;
        return;
      }
      this.save();
    } else if (this.currentStep === 1) {
      this.currentStep = 2;
    }
  }


  /**
   * Moves the stepper to the previous step.
   */
  goBack(): void {
    if (this.currentStep > 0) {
      this.currentStep -= 1;
    }
  }


}
