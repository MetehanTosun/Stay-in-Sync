import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';


import {DialogModule} from 'primeng/dialog';
import {DropdownModule} from 'primeng/dropdown';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {TextareaModule} from 'primeng/textarea';
import {StepsModule} from 'primeng/steps';
import {FileSelectEvent, FileUploadEvent, FileUploadModule} from 'primeng/fileupload';
import {TreeModule} from 'primeng/tree';
import {MessageService, TreeNode} from 'primeng/api';



import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {CreateSourceSystemDTO} from '../../models/createSourceSystemDTO';
import {ApiAuthType} from '../../models/apiAuthType';
import {BasicAuthDTO} from '../../models/basicAuthDTO';
import {ApiKeyAuthDTO} from '../../models/apiKeyAuthDTO';
import {ManageEndpointsComponent} from '../manage-endpoints/manage-endpoints.component';
import {ManageApiHeadersComponent} from '../manage-api-headers/manage-api-headers.component';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {AasService} from '../../services/aas.service';

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
  ]
})
export class CreateSourceSystemComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() sourceSystem: SourceSystemDTO | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();



  steps = [
    {label: 'Metadaten'},
    {label: 'Api Header'},
    {label: 'Endpoints'},
  ];
  currentStep = 0;
  createdSourceSystemId!: number;

  form!: FormGroup;
  selectedFile: File | null = null;
  fileSelected = false;

  typeOptions = [
    {label: 'REST-OpenAPI', value: 'REST_OPENAPI'},
    {label: 'AAS', value: 'AAS'}
  ];
  authTypeOptions = [
    {label: 'Basic', value: ApiAuthType.Basic},
    {label: 'API Key', value: ApiAuthType.ApiKey}
  ];
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
  ) {
  }

  /**
   * Initialize reactive form with metadata fields and validators.
   * Subscribes to authentication type changes to adjust validators dynamically.
   */
  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: [''],
      apiType: ['REST_OPENAPI', Validators.required],
      apiAuthType: [null],
      aasId: [''],
      authConfig: this.fb.group({
        username: [''],
        password: [''],
        apiKey: [''],
        headerName: ['']
      }),
      openApiSpec: [{value: null, disabled: false}]
    });

    this.form.get('apiType')!.valueChanges.subscribe((apiType: string) => {
      const aasIdCtrl = this.form.get('aasId')!;
      const openApiCtrl = this.form.get('openApiSpec')!;
      if (apiType === 'AAS') {
        aasIdCtrl.setValidators([Validators.required]);
        openApiCtrl.disable();
        openApiCtrl.clearValidators();
        this.steps = [
          {label: 'Metadaten & Test'},
          {label: 'Api Header'},
          {label: 'AAS Submodels'}
        ];
      } else {
        aasIdCtrl.clearValidators();
        openApiCtrl.enable();
        this.steps = [
          {label: 'Metadaten'},
          {label: 'Api Header'},
          {label: 'Endpoints'},
        ];
      }
      aasIdCtrl.updateValueAndValidity();
      openApiCtrl.updateValueAndValidity();
    });

    this.form.get('apiAuthType')!.valueChanges.subscribe((authType: ApiAuthType) => {
      const grp = this.form.get('authConfig') as FormGroup;
      // reset
      ['username', 'password', 'apiKey', 'headerName'].forEach(k => {
        grp.get(k)!.clearValidators();
        grp.get(k)!.updateValueAndValidity();
      });
      if (authType === ApiAuthType.Basic) {
        grp.get('username')!.setValidators([Validators.required]);
        grp.get('password')!.setValidators([Validators.required]);
      } else if (authType === ApiAuthType.ApiKey) {
        grp.get('apiKey')!.setValidators([Validators.required]);
        grp.get('headerName')!.setValidators([Validators.required]);
      }
      ['username', 'password', 'apiKey', 'headerName'].forEach(k => grp.get(k)!.updateValueAndValidity());
    });
  }

  /**
   * Reacts to incoming changes of the sourceSystem Input.
   * If editing an existing system, patches form values and resets to first step.
   *
   * @param changes Object containing changed @Input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sourceSystem'] && this.sourceSystem) {
      this.form.patchValue({
        name: this.sourceSystem.name,
        apiUrl: this.sourceSystem.apiUrl,
        description: this.sourceSystem.description,
        apiType: this.sourceSystem.apiType
      });
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
    console.log('📤 Sending DTO to backend:', dto);
    console.log('📤 openApiSpec field:', dto.openApiSpec);
    console.log('📤 openApiSpec type:', typeof dto.openApiSpec);

    this.sourceSystemService
      .apiConfigSourceSystemPost(dto)
      .subscribe({
        next: (resp: SourceSystemDTO) => {
          console.log('✅ Backend response:', resp);
          console.log('✅ Returned openApiSpec:', resp.openApiSpec);
          this.createdSourceSystemId = resp.id!;
          if (opts?.advanceStep !== false) {
            this.currentStep = 1;
          }
          if (opts?.onSuccess) {
            opts.onSuccess(resp);
          }
        },
        error: (err) => {
          console.error('❌ CREATE failed:', err);
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
        if (status === 401) detail = '401 Unauthorized – check Basic/API Key credentials.';
        else if (status === 403) detail = '403 Forbidden – check permissions/token.';
        else if (status === 404) detail = '404 Not Found – verify Base URL or AAS ID (AAS does not exist or wrong ID).';
        else if (status === 405) detail = '405 Method Not Allowed – verify HTTP method (expected: POST /test).';
        else if (status === 409) detail = '409 Conflict – conflicting input or already exists.';
        else if (status === 504) detail = '504 Gateway Timeout – upstream not reachable (is BaSyx running?).';
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
  selectedLivePanel: { label: string; type: string; value?: any; valueType?: string } | null = null;
  selectedLiveLoading = false;
  selectedNode?: TreeNode;

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
          }
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
          const mapped = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElementToNode(submodelId, el);
          });
          node.children = mapped;
        },
        error: (err) => {
          this.childrenLoading[key] = false;
          this.errorService.handleError(err);
        }
      });
  }

  // Tree mapping helpers
  private mapSubmodelToNode(sm: any): TreeNode {
    const id = sm.submodelId || sm.id || (sm.keys && sm.keys[0]?.value);
    const label = (sm.submodelIdShort || sm.idShort) || id;
    return {
      key: id,
      label,
      data: { type: 'submodel', id, raw: sm },
      leaf: false,
      children: []
    } as TreeNode;
  }

  private mapElementToNode(submodelId: string, el: any): TreeNode {
    const label = `${el.idShort} (${el.modelType})`;
    const typeHasChildren = el?.modelType === 'SubmodelElementCollection' || el?.modelType === 'SubmodelElementList' || el?.modelType === 'Operation';
    const hasChildren = el?.hasChildren === true || typeHasChildren;
    return {
      key: `${submodelId}::${el.idShortPath}`,
      label,
      data: { type: 'element', submodelId, idShortPath: el.idShortPath, modelType: el.modelType, raw: el },
      leaf: !hasChildren,
      children: []
    } as TreeNode;
  }

  onNodeExpand(event: any): void {
    const node: TreeNode = event.node;
    if (!node) return;
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
  }

  private loadLiveElementDetails(smId: string, idShortPath: string | undefined, node?: TreeNode): void {
    if (!this.createdSourceSystemId) return;
    this.selectedLiveLoading = true;
    const keyStr = (node && typeof node.key === 'string') ? (node.key as string) : '';
    const keyPath = keyStr.includes('::') ? keyStr.split('::')[1] : '';
    const safePath = idShortPath || keyPath || (node?.data?.raw?.idShort || '');
    const last = safePath.split('/').pop() as string;
    const parent = safePath.includes('/') ? safePath.substring(0, safePath.lastIndexOf('/')) : '';
    this.aasService
      .listElements(this.createdSourceSystemId, smId, { depth: 'shallow', parentPath: parent || undefined, source: 'LIVE' })
      .subscribe({
        next: (resp: any) => {
          this.selectedLiveLoading = false;
          const list: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const found = list.find((el: any) => el.idShort === last);
          if (found) {
            this.selectedLivePanel = {
              label: `${found.idShort} (${found.modelType})`,
              type: found.modelType,
              value: (found as any).value,
              valueType: (found as any).valueType
            };
            if (node && node.data) {
              const computedPath = safePath || (parent ? `${parent}/${found.idShort}` : found.idShort);
              node.data.idShortPath = computedPath;
              node.data.raw = { ...(node.data.raw || {}), idShortPath: computedPath, modelType: found.modelType, valueType: found.valueType };
            }
          } else {
            this.selectedLivePanel = { label: last, type: 'Unknown' };
          }
        },
        error: (err: any) => {
          this.selectedLiveLoading = false;
          this.errorService.handleError(err);
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
  targetSubmodelId = '';
  parentPath = '';
  newElementJson = '{\n  "modelType": "Property",\n  "idShort": "NewProp",\n  "valueType": "xs:string",\n  "value": "42"\n}';
  openCreateElement(smId: string, parent?: string): void {
    this.targetSubmodelId = smId;
    this.parentPath = parent || '';
    this.showElementDialog = true;
  }
  createElement(): void {
    if (!this.createdSourceSystemId || !this.targetSubmodelId) return;
    try {
      const body = JSON.parse(this.newElementJson);
      const smIdB64 = this.aasService.encodeIdToBase64Url(this.targetSubmodelId);
      this.aasService.createElement(this.createdSourceSystemId, smIdB64, body, this.parentPath || undefined)
        .subscribe({
          next: () => {
            this.showElementDialog = false;
            // Ensure snapshot reflects new element, then refresh the tree at the right place
            this.aasService.refreshSnapshot(this.createdSourceSystemId).subscribe({
              next: () => this.refreshTreeAfterCreate(),
              error: () => this.refreshTreeAfterCreate()
            });
          },
          error: (err) => this.errorService.handleError(err)
        });
    } catch (e) {
      this.errorService.handleError(e as any);
    }
  }

  private refreshTreeAfterCreate(): void {
    if (this.parentPath) {
      const key = `${this.targetSubmodelId}::${this.parentPath}`;
      const parentNode = this.findNodeByKey(key, this.treeNodes);
      if (parentNode) {
        (parentNode as any).expanded = true;
        this.refreshNodeLive(this.targetSubmodelId, this.parentPath, parentNode);
      } else {
        this.refreshNodeLive(this.targetSubmodelId, '', undefined);
      }
    } else {
      this.refreshNodeLive(this.targetSubmodelId, '', undefined);
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
    if (!this.createdSourceSystemId) return;
    const key = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    this.childrenLoading[key] = true;
    this.aasService
      .listElements(this.createdSourceSystemId, submodelId, { depth: 'shallow', parentPath: parentPath || undefined, source: 'LIVE' })
      .subscribe({
        next: (resp) => {
          this.childrenLoading[key] = false;
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const mapped = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElementToNode(submodelId, el);
          });
          if (node) {
            node.children = mapped;
          } else {
            // root
            const attachNode = this.findNodeByKey(submodelId, this.treeNodes);
            if (attachNode) {
              attachNode.children = mapped;
            }
          }
        },
        error: (err) => {
          this.childrenLoading[key] = false;
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
    this.valueElementPath = element.idShortPath;
    this.valueTypeHint = element.valueType || 'xs:string';
    this.valueNew = '';
    this.showValueDialog = true;
  }
  setValue(): void {
    if (!this.createdSourceSystemId || !this.valueSubmodelId || !this.valueElementPath) return;
    const smIdB64 = this.aasService.encodeIdToBase64Url(this.valueSubmodelId);
    const payload = {
      modelType: 'Property',
      idShort: this.valueElementPath.split('/').pop(),
      valueType: this.valueTypeHint,
      value: this.valueNew
    };
    this.aasService.setPropertyValue(this.createdSourceSystemId, smIdB64, this.valueElementPath.replaceAll('/', '.'), payload)
      .subscribe({
        next: () => {
          this.showValueDialog = false;
        },
        error: (err) => this.errorService.handleError(err)
      });
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
