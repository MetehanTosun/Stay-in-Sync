import {Component, OnInit} from '@angular/core';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {DialogModule} from 'primeng/dialog';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {CommonModule} from '@angular/common';
import {ManageApiHeadersComponent} from '../manage-api-headers/manage-api-headers.component';
import {ManageEndpointsComponent} from '../manage-endpoints/manage-endpoints.component';
import {TreeNode} from 'primeng/api';
import {TabViewModule} from 'primeng/tabview';
import {TextareaModule} from 'primeng/textarea';
import {TreeModule} from 'primeng/tree';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';
import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {SourceSystemEndpointResourceService} from '../../service/sourceSystemEndpointResource.service';
import { SourceSystemSearchPipe} from '../../pipes/source-system-search.pipe';
import {AasService} from '../../services/aas.service';
import {TableModule} from 'primeng/table';
import {ToolbarModule} from 'primeng/toolbar';
import {MessageModule} from 'primeng/message';
import {DropdownModule} from 'primeng/dropdown';
import {FileUploadModule} from 'primeng/fileupload';
import {ActivatedRoute} from '@angular/router';
import {InplaceModule} from 'primeng/inplace';
import { AasElementDialogComponent, AasElementDialogData, AasElementDialogResult } from '../../../../shared/components/aas-element-dialog/aas-element-dialog.component';
import { CheckboxModule } from 'primeng/checkbox';
import { CreateSourceSystemDialogService } from '../../services/create-source-system-dialog.service';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';


interface AasOperationVarView { idShort: string; modelType?: string; valueType?: string }
interface AasAnnotationView { idShort: string; modelType?: string; valueType?: string; value?: any }
interface AasElementLivePanel {
  label: string;
  type: string;
  value?: any;
  valueType?: string;
  min?: any;
  max?: any;
  inputVariables?: AasOperationVarView[];
  outputVariables?: AasOperationVarView[];
  inoutputVariables?: AasOperationVarView[];
  firstRef?: string;
  secondRef?: string;
  annotations?: AasAnnotationView[];
}

@Component({
  selector: 'app-source-system-page',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    DialogModule,
    ToolbarModule,
    MessageModule,
    CardModule,
    TabViewModule,
    TreeModule,
    DropdownModule,
    InputTextModule,
    TextareaModule,
    FileUploadModule,
    ReactiveFormsModule,
    ManageApiHeadersComponent,
    ManageEndpointsComponent,
    FormsModule,
    InplaceModule,
    AasElementDialogComponent,
    CheckboxModule,
    ToastModule
  ],
  templateUrl: './source-system-page.component.html',
  styleUrl: './source-system-page.component.css',
  providers: [MessageService]
})
/**
 * Source system page: manages base info, endpoints, and AAS snapshot/live views,
 * including element creation, value setting, and AASX upload flows.
 */
export class SourceSystemPageComponent implements OnInit {
  private originalName: string = "";
  private originalApiUrl: string = "";
  ngOnInit(): void {
    const id = Number.parseInt(this.route.snapshot.paramMap.get('id')!);
    this.sourceSystemService.apiConfigSourceSystemIdGet(id).subscribe({
      next: data => { this.selectedSystem = data; },
      error: err => { this.httpErrorService.handleError(err); }
    });

  }


  /** Indicates whether page data is currently loading. */
  loading = false;

  /** Currently selected system for viewing and editing. */
  selectedSystem?: SourceSystemDTO;


  /** AAS Snapshot/Live management state. */
  aasTreeNodes: TreeNode[] = [];
  aasTreeLoading = false;
  aasTestLoading = false;
  aasTestError: string | null = null;
  selectedAasNode?: TreeNode;
  aasSelectedLivePanel: AasElementLivePanel | null = null;
  aasSelectedLiveLoading = false;
  showAasValueDialog = false;
  aasValueSubmodelId = '';
  aasValueElementPath = '';
  aasValueNew = '';
  aasValueTypeHint = 'xs:string';

  // Shared element dialog
  showElementDialog = false;
  elementDialogData: AasElementDialogData | null = null;

  // AASX upload
  showAasxUpload = false;
  aasxSelectedFile: File | null = null;
  isUploadingAasx = false;
  aasxPreview: any = null;
  aasxSelection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> } = { submodels: [] };

  // Cache: submodelId -> (idShortPath -> modelType)
  private aasTypeCache: Record<string, Record<string, string>> = {};

  private ensureAasTypeMap(submodelId: string): void {
    const systemId = this.selectedSystem?.id;
    if (!systemId) return;
    if (this.aasTypeCache[submodelId]) {
      this.applyAasTypeMapToTree(submodelId);
      return;
    }
    this.aasService
      .listElements(systemId, submodelId, {depth: 'all', source: 'LIVE'})
      .subscribe({
        next: (resp) => {
          const list: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const map: Record<string, string> = {};
          for (const el of list) {
            const p = el?.idShortPath || el?.idShort;
            const t = this.inferModelType(el);
            if (p && t) map[p] = t;
          }
          this.aasTypeCache[submodelId] = map;
          this.applyAasTypeMapToTree(submodelId);
        },
        error: () => {
        }
      });
  }

  private applyAasTypeMapToTree(submodelId: string): void {
    const map = this.aasTypeCache[submodelId];
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
    applyMap(this.aasTreeNodes);
    this.aasTreeNodes = [...this.aasTreeNodes];
  }

  private hydrateAasNodeTypesForNodes(submodelId: string, nodes: TreeNode[] | undefined): void {
    const systemId = this.selectedSystem?.id;
    if (!systemId || !nodes || nodes.length === 0) return;
    for (const n of nodes) {
      if (n?.data?.type === 'element' && n.data?.submodelId === submodelId) {
        const path: string = n.data.idShortPath || n.data.raw?.idShortPath || n.data.raw?.idShort;
        if (!path) continue;
        this.aasService.getElement(systemId, submodelId, path, 'LIVE').subscribe({
          next: (el: any) => {
            const liveType = el?.modelType || this.inferModelType(el);
            if (liveType) {
              n.data.modelType = liveType;
              this.aasTreeNodes = [...this.aasTreeNodes];
            }
          },
          error: () => {
          }
        });
      }
    }
  }

  // AAS create dialogs
  showAasSubmodelDialog = false;
  aasNewSubmodelJson = '{\n  "id": "https://example.com/ids/sm/new",\n  "idShort": "NewSubmodel"\n}';
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

  setAasSubmodelTemplate(kind: 'minimal' | 'property' | 'collection'): void {
    if (kind === 'minimal') this.aasNewSubmodelJson = this.aasMinimalSubmodelTemplate;
    if (kind === 'property') this.aasNewSubmodelJson = this.aasPropertySubmodelTemplate;
    if (kind === 'collection') this.aasNewSubmodelJson = this.aasCollectionSubmodelTemplate;
  }

  openAasCreateSubmodel(): void {
    this.showAasSubmodelDialog = true;
  }

  aasCreateSubmodel(): void {
    if (!this.selectedSystem?.id) return;
    try {
      const body = JSON.parse(this.aasNewSubmodelJson);
      this.aasService.createSubmodel(this.selectedSystem.id, body).subscribe({
        next: () => {
          this.showAasSubmodelDialog = false;
          this.discoverAasSnapshot();
          this.messageService.add({ key: 'sourceAAS', severity: 'success', summary: 'Submodel created', detail: 'Submodel was created successfully.' });
        },
        error: (err) => { this.erorrService.handleError(err); this.messageService.add({ key: 'sourceAAS', severity: 'error', summary: 'Error', detail: (err?.message || 'Failed to create submodel') }); }
      });
    } catch (e) {
      this.erorrService.handleError(e as any);
      this.messageService.add({ key: 'sourceAAS', severity: 'error', summary: 'Error', detail: 'Invalid JSON for submodel.' });
    }
  }

  showAasElementDialog = false;
  aasTargetSubmodelId = '';
  aasParentPath = '';
  aasNewElementJson = '{\n  "modelType": "Property",\n  "idShort": "NewProp",\n  "valueType": "xs:string",\n  "value": "42"\n}';

  openAasCreateElement(smId: string, parent?: string): void {
    if (!this.selectedSystem?.id) return;
    this.elementDialogData = {
      submodelId: smId,
      parentPath: parent,
      systemId: this.selectedSystem.id!,
      systemType: 'source'
    };
    this.showElementDialog = true;
  }

  onElementDialogResult(result: AasElementDialogResult): void {
    if (result.success && result.element) {
      const el = result.element;
      const smIdB64 = this.aasService.encodeIdToBase64Url(el.submodelId);
      this.aasService.createElement(this.selectedSystem!.id!, smIdB64, el.body, el.parentPath)
        .subscribe({
          next: () => {
            this.showElementDialog = false;
            this.refreshAasNodeLive(el.submodelId, el.parentPath || '', undefined);
            this.messageService.add({ key: 'sourceAAS', severity: 'success', summary: 'Element created', detail: 'Element has been created.' });
          },
          error: (err) => { this.erorrService.handleError(err); this.messageService.add({ key: 'sourceAAS', severity: 'error', summary: 'Error', detail: (err?.message || 'Failed to create element') }); }
        });
    }
  }

  openAasxUpload(): void {
    this.showAasxUpload = true;
    this.aasxSelectedFile = null;
  }

  onAasxFileSelected(event: any): void {
    this.aasxSelectedFile = event.files?.[0] || null;
    if (this.aasxSelectedFile && this.selectedSystem?.id) {
      this.aasService.previewAasx(this.selectedSystem.id, this.aasxSelectedFile).subscribe({
        next: (resp) => {
          this.aasxPreview = resp?.submodels || (resp?.result ?? []);
          const arr = Array.isArray(this.aasxPreview) ? this.aasxPreview : (this.aasxPreview?.submodels ?? []);
          this.aasxSelection = { submodels: (arr || []).map((sm: any) => ({ id: sm.id || sm.submodelId, full: true, elements: [] })) };
        },
        error: () => { this.aasxPreview = null; this.aasxSelection = { submodels: [] }; }
      });
    }
  }

  private getSmId(sm: any): string { return sm?.id || sm?.submodelId || ''; }
  getOrInitAasxSelFor(sm: any): { id: string; full: boolean; elements: string[] } {
    const id = this.getSmId(sm);
    let found = this.aasxSelection.submodels.find((s) => s.id === id);
    if (!found) { found = { id, full: true, elements: [] }; this.aasxSelection.submodels.push(found); }
    return found;
  }
  toggleAasxSubmodelFull(sm: any, checked: boolean): void {
    const sel = this.getOrInitAasxSelFor(sm);
    sel.full = !!checked;
    if (sel.full) sel.elements = [];
  }

  uploadAasx(): void {
    if (this.isUploadingAasx) return;
    if (!this.aasxSelectedFile || !this.selectedSystem?.id) return;
    this.isUploadingAasx = true;
    this.createDialogService.uploadAasx(this.selectedSystem.id, this.aasxSelectedFile, this.aasxSelection)
      .then(() => {
        this.isUploadingAasx = false;
        this.showAasxUpload = false;
        this.discoverAasSnapshot();
        this.messageService.add({ key: 'sourceAAS', severity: 'success', summary: 'Upload accepted', detail: 'AASX uploaded successfully.' });
      })
      .catch((err) => {
        this.isUploadingAasx = false;
        this.erorrService.handleError(err);
        this.messageService.add({ key: 'sourceAAS', severity: 'error', summary: 'Upload failed', detail: (err?.message || 'See console for details') });
      });
  }


  constructor(
    private sourceSystemService: SourceSystemResourceService,
    private readonly route: ActivatedRoute,
    private fb: FormBuilder,
    protected erorrService: HttpErrorService,
    private apiEndpointSvc: SourceSystemEndpointResourceService,
    private searchPipe: SourceSystemSearchPipe,
    private aasService: AasService,
    private readonly httpErrorService: HttpErrorService,
    private readonly createDialogService: CreateSourceSystemDialogService,
    private readonly messageService: MessageService
  ) {
  }


  // AAS: Manage Page helpers
  isAasSelected(): boolean {
    return (this.selectedSystem?.apiType || '').toUpperCase().includes('AAS');
  }

  discoverAasSnapshot(): void {
    if (!this.selectedSystem?.id) return;
    this.aasTreeLoading = true;
    this.aasService.listSubmodels(this.selectedSystem.id, 'SNAPSHOT').subscribe({
      next: (resp) => {
        const submodels = Array.isArray(resp) ? resp : (resp?.result ?? []);
        this.aasTreeNodes = submodels.map((sm: any) => this.mapSmToNode(sm));
        this.aasTreeLoading = false;
      },
      error: (err) => {
        this.aasTreeLoading = false;
        this.erorrService.handleError(err);
      }
    });
  }

  onAasNodeExpand(event: any): void {
    const node: TreeNode = event.node;
    if (!node || !this.selectedSystem?.id) return;
    if (node.data?.type === 'submodel') {
      this.loadAasChildren(node.data.id, undefined, node);
    } else if (node.data?.type === 'element') {
      this.loadAasChildren(node.data.submodelId, node.data.idShortPath, node);
    }
  }

  onAasNodeSelect(event: any): void {
    const node: TreeNode = event.node;
    this.selectedAasNode = node;
    this.aasSelectedLivePanel = null;
    if (!node || node.data?.type !== 'element') return;
    const smId: string = node.data.submodelId;
    const idShortPath: string = node.data.idShortPath;
    this.loadAasLiveElementDetails(smId, idShortPath, node);
  }

  private loadAasChildren(submodelId: string, parentPath: string | undefined, attach: TreeNode): void {
    if (!this.selectedSystem?.id) return;
    this.aasService.listElements(this.selectedSystem.id, submodelId, {
      depth: 'shallow',
      parentPath,
      source: 'SNAPSHOT'
    }).subscribe({
      next: (resp) => {
        const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
        const mapped = list.map((el: any) => {
          if (!el.idShortPath && el.idShort) {
            el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
          }
          return this.mapElToNode(submodelId, el);
        });
        attach.children = mapped;
        this.aasTreeNodes = [...this.aasTreeNodes];
        // Background precise type hydration via LIVE element details
        this.hydrateAasNodeTypesForNodes(submodelId, attach.children as TreeNode[]);
        // hydrate types in background
        this.ensureAasTypeMap(submodelId);
      },
      error: (err) => this.erorrService.handleError(err)
    });
  }

  private refreshAasNodeLive(submodelId: string, parentPath: string, node?: TreeNode): void {
    if (!this.selectedSystem?.id) return;
    const key = parentPath ? `${submodelId}::${parentPath}` : submodelId;
    this.aasService
      .listElements(this.selectedSystem.id, submodelId, {
        depth: 'shallow',
        parentPath: parentPath || undefined,
        source: 'LIVE'
      })
      .subscribe({
        next: (resp) => {
          const list = Array.isArray(resp) ? resp : (resp?.result ?? []);
          const mapped = list.map((el: any) => {
            if (!el.idShortPath && el.idShort) {
              el.idShortPath = parentPath ? `${parentPath}/${el.idShort}` : el.idShort;
            }
            return this.mapElToNode(submodelId, el);
          });
          if (node) {
            node.children = mapped;
            this.aasTreeNodes = [...this.aasTreeNodes];
          } else {
            const attachKey = parentPath ? `${submodelId}::${parentPath}` : submodelId;
            const attachNode = this.findAasNodeByKey(attachKey, this.aasTreeNodes);
            if (attachNode) {
              attachNode.children = mapped;
              this.aasTreeNodes = [...this.aasTreeNodes];
            }
          }
        },
        error: (err) => this.erorrService.handleError(err)
      });
  }

  private loadAasLiveElementDetails(smId: string, idShortPath: string | undefined, node?: TreeNode): void {
    const systemId = this.selectedSystem?.id;
    if (!systemId) return;
    this.aasSelectedLiveLoading = true;
    const keyStr = (node && typeof node.key === 'string') ? (node.key as string) : '';
    const keyPath = keyStr.includes('::') ? keyStr.split('::')[1] : '';
    const safePath = idShortPath || keyPath || (node?.data?.raw?.idShort || '');
    const last = safePath.split('/').pop() as string;
    const parent = safePath.includes('/') ? safePath.substring(0, safePath.lastIndexOf('/')) : '';
    // Robust: try direct element details endpoint (backend has deep fallback)
    if (!systemId) { this.aasSelectedLiveLoading = false; return; }
    this.aasService.getElement(systemId, smId, safePath, 'LIVE').subscribe({
      next: (found: any) => {
        this.aasSelectedLiveLoading = false;
        const liveType = found?.modelType || (found?.valueType ? 'Property' : undefined);
        const minValue = (found as any).min ?? (found as any).minValue;
        const maxValue = (found as any).max ?? (found as any).maxValue;
        const inputVars = Array.isArray((found as any).inputVariables) ? (found as any).inputVariables : [];
        const outputVars = Array.isArray((found as any).outputVariables) ? (found as any).outputVariables : [];
        const inoutVars = Array.isArray((found as any).inoutputVariables) ? (found as any).inoutputVariables : [];
        const ann1 = (found as any).annotations;
        const ann2 = (found as any).annotation;
        const annotationsRaw = Array.isArray(ann1) ? ann1 : (Array.isArray(ann2) ? ann2 : []);
        const mapVar = (v: any): AasOperationVarView | null => {
          const val = v?.value ?? v;
          const idShort = val?.idShort;
          if (!idShort) return null;
          return {idShort, modelType: val?.modelType, valueType: val?.valueType};
        };
        const mapAnnotation = (a: any): AasAnnotationView | null => {
          const val = a?.value ?? a;
          const idShort = val?.idShort;
          if (!idShort) return null;
          return {idShort, modelType: val?.modelType, valueType: val?.valueType, value: val?.value};
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
          try {
            return JSON.stringify(ref);
          } catch {
            return String(ref);
          }
        };
        const firstRef = stringifyRef((found as any).first || (found as any).firstReference);
        const secondRef = stringifyRef((found as any).second || (found as any).secondReference);
        this.aasSelectedLivePanel = {
          label: found.idShort,
          type: liveType || 'Unknown',
          value: (found as any).value,
          valueType: (found as any).valueType,
          min: minValue,
          max: maxValue,
          inputVariables: inputVars.map(mapVar).filter(Boolean) as AasOperationVarView[],
          outputVariables: outputVars.map(mapVar).filter(Boolean) as AasOperationVarView[],
          inoutputVariables: inoutVars.map(mapVar).filter(Boolean) as AasOperationVarView[],
          firstRef,
          secondRef,
          annotations: annotationsRaw.map(mapAnnotation).filter(Boolean) as AasAnnotationView[]
        } as any;
        // Fallback: If AnnotatedRelationshipElement has no annotations in direct payload, load children as annotations
        if ((liveType === 'AnnotatedRelationshipElement') && (((this.aasSelectedLivePanel?.annotations?.length ?? 0) === 0))) {
          const pathForChildren = safePath;
          // Try deep list to get full element (with annotations)
          this.aasService
            .listElements(systemId, smId, {depth: 'all', source: 'LIVE'})
            .subscribe({
              next: (resp: any) => {
                const arr: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
                const foundDeep = arr.find((el: any) => (el?.idShortPath || el?.idShort) === pathForChildren);
                const anns = Array.isArray(foundDeep?.annotations) ? foundDeep.annotations : (Array.isArray(foundDeep?.annotation) ? foundDeep.annotation : []);
                let annotations: AasAnnotationView[] = [];
                if (anns.length) {
                  annotations = anns.map((a: any) => ({
                    idShort: a?.idShort,
                    modelType: a?.modelType,
                    valueType: a?.valueType,
                    value: a?.value
                  } as AasAnnotationView));
                } else {
                  // Fallback: treat shallow children as annotations
                  const list: any[] = arr.filter((el: any) => {
                    const p = el?.idShortPath || el?.idShort;
                    if (!p || !p.startsWith(pathForChildren + '/')) return false;
                    const rest = p.substring((pathForChildren + '/').length);
                    return rest && !rest.includes('/');
                  });
                  annotations = list.map((el: any) => ({
                    idShort: el?.idShort,
                    modelType: el?.modelType,
                    valueType: el?.valueType,
                    value: el?.value
                  } as AasAnnotationView));
                }
                if (this.aasSelectedLivePanel) {
                  this.aasSelectedLivePanel = {...this.aasSelectedLivePanel, annotations};
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
          node.data.raw = {
            ...(node.data.raw || {}),
            idShortPath: computedPath,
            modelType: found.modelType,
            valueType: found.valueType
          };
          this.aasTreeNodes = [...this.aasTreeNodes];
        }
      },
      error: (_err: any) => {
        // Fallback: list under parent shallow and pick child
        this.aasService
          .listElements(systemId, smId, {depth: 'shallow', parentPath: parent || undefined, source: 'LIVE'})
          .subscribe({
            next: (resp: any) => {
              this.aasSelectedLiveLoading = false;
              const list: any[] = Array.isArray(resp) ? resp : (resp?.result ?? []);
              const found2 = list.find((el: any) => el.idShort === last);
              if (found2) {
                const liveType = found2?.modelType || (found2?.valueType ? 'Property' : undefined);
                const minValue = (found2 as any).min ?? (found2 as any).minValue;
                const maxValue = (found2 as any).max ?? (found2 as any).maxValue;
                const inputVars2 = Array.isArray((found2 as any).inputVariables) ? (found2 as any).inputVariables : [];
                const outputVars2 = Array.isArray((found2 as any).outputVariables) ? (found2 as any).outputVariables : [];
                const inoutVars2 = Array.isArray((found2 as any).inoutputVariables) ? (found2 as any).inoutputVariables : [];
                const mapVar2 = (v: any): AasOperationVarView | null => {
                  const val = v?.value ?? v;
                  const idShort = val?.idShort;
                  if (!idShort) return null;
                  return {idShort, modelType: val?.modelType, valueType: val?.valueType};
                };
                const ann1b = (found2 as any).annotations;
                const ann2b = (found2 as any).annotation;
                const annotationsRaw2 = Array.isArray(ann1b) ? ann1b : (Array.isArray(ann2b) ? ann2b : []);
                const mapAnnotation2 = (a: any): AasAnnotationView | null => {
                  const val = a?.value ?? a;
                  const idShort = val?.idShort;
                  if (!idShort) return null;
                  return {idShort, modelType: val?.modelType, valueType: val?.valueType, value: val?.value};
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
                  try {
                    return JSON.stringify(ref);
                  } catch {
                    return String(ref);
                  }
                };
                const firstRef2 = stringifyRef2((found2 as any).first || (found2 as any).firstReference);
                const secondRef2 = stringifyRef2((found2 as any).second || (found2 as any).secondReference);
                this.aasSelectedLivePanel = {
                  label: found2.idShort,
                  type: liveType || 'Unknown',
                  value: (found2 as any).value,
                  valueType: (found2 as any).valueType,
                  min: minValue,
                  max: maxValue,
                  inputVariables: inputVars2.map(mapVar2).filter(Boolean) as AasOperationVarView[],
                  outputVariables: outputVars2.map(mapVar2).filter(Boolean) as AasOperationVarView[],
                  inoutputVariables: inoutVars2.map(mapVar2).filter(Boolean) as AasOperationVarView[],
                  firstRef: firstRef2,
                  secondRef: secondRef2,
                  annotations: annotationsRaw2.map(mapAnnotation2).filter(Boolean) as AasAnnotationView[]
                } as any;
                if (node && node.data) {
                  node.data.modelType = liveType || node.data.modelType;
                  this.aasTreeNodes = [...this.aasTreeNodes];
                }
              } else {
                this.aasSelectedLivePanel = {label: last, type: 'Unknown'} as any;
              }
            },
            error: (err2: any) => {
              this.aasSelectedLiveLoading = false;
              this.erorrService.handleError(err2);
            }
          });
      }
    });
  }

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

  aasSetValue(): void {
    if (!this.selectedSystem || !this.selectedSystem.id || !this.aasValueSubmodelId || !this.aasValueElementPath) return;
    const smIdB64 = this.aasService.encodeIdToBase64Url(this.aasValueSubmodelId);
    const parsedValue = this.parseValueForType(this.aasValueNew, this.aasValueTypeHint);
    this.aasService.setPropertyValue(this.selectedSystem.id, smIdB64, this.aasValueElementPath, parsedValue as any)
      .subscribe({
        next: () => {
          this.showAasValueDialog = false;
          // refresh live details
          const node = this.selectedAasNode;
          if (node?.data) {
            this.loadAasLiveElementDetails(node.data.submodelId, node.data.idShortPath, node);
          } else {
            const parent = this.aasValueElementPath.includes('/') ? this.aasValueElementPath.substring(0, this.aasValueElementPath.lastIndexOf('/')) : '';
            const parentNode = parent ? this.findAasNodeByKey(`${this.aasValueSubmodelId}::${parent}`, this.aasTreeNodes) : this.findAasNodeByKey(this.aasValueSubmodelId, this.aasTreeNodes);
            this.refreshAasNodeLive(this.aasValueSubmodelId, parent, parentNode || undefined);
          }
        },
        error: (err) => this.erorrService.handleError(err)
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
    return raw;
  }

  deleteAasSubmodel(submodelId: string): void {
    if (!this.selectedSystem?.id || !submodelId) return;
    const smIdB64 = this.aasService.encodeIdToBase64Url(submodelId);
    this.aasService.deleteSubmodel(this.selectedSystem.id, smIdB64).subscribe({
      next: () => {
        this.discoverAasSnapshot();
        this.messageService.add({ key: 'sourceAAS', severity: 'success', summary: 'Submodel deleted', detail: 'Submodel removed from shell.' });
      },
      error: (err) => { this.erorrService.handleError(err); this.messageService.add({ key: 'sourceAAS', severity: 'error', summary: 'Error', detail: (err?.message || 'Failed to delete submodel') }); }
    });
  }

  deleteAasElement(submodelId: string, idShortPath: string): void {
    if (!this.selectedSystem?.id || !submodelId || !idShortPath) return;
    // Pass raw submodelId; service will encode internally
    this.aasService.deleteElement(this.selectedSystem.id, submodelId, idShortPath).subscribe({
      next: () => {
        let parent = '';
        if (idShortPath.includes('/')) {
          parent = idShortPath.substring(0, idShortPath.lastIndexOf('/'));
        } else if (idShortPath.includes('.')) {
          parent = idShortPath.substring(0, idShortPath.lastIndexOf('.'));
        }
        const parentNode = parent ? this.findAasNodeByKey(`${submodelId}::${parent}`, this.aasTreeNodes) : this.findAasNodeByKey(submodelId, this.aasTreeNodes);
        if (parentNode) {
          this.loadAasChildren(submodelId, parent || undefined, parentNode);
        } else {
          // Fallback: refresh appropriate branch using LIVE view
          this.refreshAasNodeLive(submodelId, parent || '', undefined);
        }
        this.messageService.add({ key: 'sourceAAS', severity: 'success', summary: 'Element deleted', detail: 'Element has been deleted.' });
      },
      error: (err) => { this.erorrService.handleError(err); this.messageService.add({ key: 'sourceAAS', severity: 'error', summary: 'Error', detail: (err?.message || 'Failed to delete element') }); }
    });
  }

  private findAasNodeByKey(key: string, nodes: TreeNode[] | undefined): TreeNode | null {
    if (!nodes) return null;
    for (const n of nodes) {
      if ((n.key as string) === key) return n;
      const found = this.findAasNodeByKey(key, n.children as TreeNode[]);
      if (found) return found;
    }
    return null;
  }

  private mapSmToNode(sm: any): TreeNode {
    const id = sm.submodelId || sm.id || (sm.keys && sm.keys[0]?.value);
    const label = (sm.submodelIdShort || sm.idShort) || id;
    return {
      key: id,
      label,
      data: {type: 'submodel', id, modelType: 'Submodel', raw: sm},
      leaf: false,
      children: []
    } as TreeNode;
  }

  private mapElToNode(submodelId: string, el: any): TreeNode {
    const computedType = this.inferModelType(el);
    const label = el.idShort;
    const typeHasChildren = computedType === 'SubmodelElementCollection' || computedType === 'SubmodelElementList' || computedType === 'Operation' || computedType === 'Entity';
    // If type is unknown, allow expansion to let user try to open children; LIVE hydration will correct it
    const hasChildren = el?.hasChildren === true || typeHasChildren || computedType === undefined;
    return {
      key: `${submodelId}::${el.idShortPath || el.idShort}`,
      label,
      data: {type: 'element', submodelId, idShortPath: el.idShortPath || el.idShort, modelType: computedType, raw: el},
      leaf: !hasChildren,
      children: []
    } as TreeNode;
  }

  // Infer a more precise modelType when missing
  private inferModelType(el: any): string | undefined {
    if (!el) return undefined;
    if (el.modelType) return el.modelType;
    // Detect Range before Property
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

  aasTest(): void {
    if (!this.selectedSystem?.id) return;
    this.aasTestLoading = true;
    this.aasTestError = null;
    this.aasService.aasTest(this.selectedSystem.id).subscribe({
      next: () => {
        this.aasTestLoading = false;
        this.messageService.add({ key: 'sourceAAS', severity: 'success', summary: 'Connection successful', detail: 'AAS connection works.' });
      },
      error: (err) => {
        this.aasTestLoading = false;
        this.aasTestError = 'Connection failed. Please verify Base URL, AAS ID and auth.';
        this.erorrService.handleError(err);
        this.messageService.add({ key: 'sourceAAS', severity: 'error', summary: 'Connection failed', detail: this.aasTestError });
      }
    });
  }


  goBack() {

  }

  onInplaceActivate() {
    this.originalName = this.selectedSystem?.name || '';
    this.originalApiUrl = this.selectedSystem?.apiUrl || '';
  }

  onSave(closeCallback: any) {
    this.sourceSystemService.apiConfigSourceSystemIdPut(this.selectedSystem!.id!, this.selectedSystem!).subscribe({
      next: data => {
      },
      error: err => {
        this.httpErrorService.handleError(err)
      }
    });
    closeCallback();
  }

  onClose(closeCallback: any) {
    this.selectedSystem!.name = this.originalName;
    this.selectedSystem!.apiUrl = this.originalApiUrl;
    closeCallback();
  }
}

