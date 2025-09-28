import {ChangeDetectorRef, Component, inject, OnDestroy, OnInit, ViewChild,} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import type {editor, IDisposable} from 'monaco-editor';
import * as ts from 'typescript';
import {ScriptEditorService, ScriptPayload,} from '../../../core/services/script-editor.service';
import {MessagesModule} from 'primeng/messages';
import {ConfirmationService, MessageService} from 'primeng/api';

import {ActivatedRoute} from '@angular/router';

import { debounceTime, finalize, first, of, Subject, Subscription, switchMap } from 'rxjs';

// PrimeNG Modules
import {PanelModule} from 'primeng/panel';
import {ButtonModule} from 'primeng/button';
import {SplitterModule} from 'primeng/splitter';
import {AccordionModule} from 'primeng/accordion';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {ToastModule} from 'primeng/toast';

// Monaco Editor Module
import {MonacoEditorModule} from 'ngx-monaco-editor-v2';

import {AasArc, AnyArc, ApiRequestConfiguration, SubmodelDescription} from '../models/arc.models';
import {ArcStateService} from '../../../core/services/arc-state.service';
import {SourceSystem, SourceSystemEndpoint} from '../../source-system/models/source-system.models';
import {ArcManagementPanelComponent} from '../arc-management-panel/arc-management-panel.component';
import {ArcWizardComponent} from '../arc-wizard/arc-wizard.component';
import {InputTextModule} from 'primeng/inputtext';
import {FloatLabel} from 'primeng/floatlabel';
import { MonacoEditorService } from '../../../core/services/monaco-editor.service';
import { TypeDefinitionsResponse } from '../models/target-system.models';
import { TargetArcPanelComponent } from '../target-arc-panel/target-arc-panel.component';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ArcWizardAasComponent } from '../arc-wizard-aas/arc-wizard-aas.component';
import { AasService } from '../../source-system/services/aas.service';

interface MonacoExtraLib {
  uri: String;
  disposable: IDisposable;
}
// 
@Component({
  selector: 'app-script-editor-step', // might be without step
  providers: [ConfirmationService],
  templateUrl: './script-editor-page.component.html',
  styleUrls: ['./script-editor-page.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MonacoEditorModule,
    PanelModule,
    ButtonModule,
    SplitterModule,
    AccordionModule,
    MessagesModule,
    ProgressSpinnerModule,
    ArcManagementPanelComponent,
    ArcWizardComponent,
    ArcWizardAasComponent,
    ConfirmDialog,
    ToastModule,
    InputTextModule,
    FloatLabel,
    TargetArcPanelComponent
  ],
})
export class ScriptEditorPageComponent implements OnInit, OnDestroy {
  @ViewChild(TargetArcPanelComponent) targetArcPanel!: TargetArcPanelComponent;
  @ViewChild(ArcManagementPanelComponent) arcManagementPanel!: ArcManagementPanelComponent;

  currentTransformationId: string | null = null;
  scriptPayload: ScriptPayload | null = null;

  editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    minimap: {enabled: true},
  };

  private monaco!: typeof import('monaco-editor');
  private monacoInstance: editor.IStandaloneCodeEditor | undefined;
  private currentExtraLibs: MonacoExtraLib[] = [];
  private subscriptions = new Subscription();

  isLoading: boolean = false;
  isSaving: boolean = false;
  loadingMessage = 'Initializing...';
  code: string = `// Script editor will initialize once a SyncJob context is loaded.`;

  isWizardVisible = false;
  wizardContext: {
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arcToEdit?: ApiRequestConfiguration;
    arcToClone?: ApiRequestConfiguration
  } | null | undefined = null;

  isAasWizardVisible = false;
  aasWizardContext: {
    system: SourceSystem;
    submodel: SubmodelDescription;
    arcToEdit?: AasArc;
  } | null = null;

  private onModelChange = new Subject<void>();

  private cdr = inject(ChangeDetectorRef);
  private scriptEditorService = inject(ScriptEditorService);
  private messageService = inject(MessageService);
  private arcStateService = inject(ArcStateService);
  private monacoEditorService = inject(MonacoEditorService);
  private confirmationService = inject(ConfirmationService);
  private aasService = inject(AasService);

  private route = inject(ActivatedRoute);

  constructor() {
    this.subscriptions.add(
      this.onModelChange.pipe(debounceTime(1000)).subscribe(() => this.analyzeEditorContentForTypes())
    );
  }

  ngOnInit(): void {
    this.currentTransformationId = this.route.snapshot.paramMap.get('transformationId');

    if (!this.currentTransformationId) {
      this.isLoading = false;
      this.code = '// ERROR: No Transformation ID found in URL. Cannot load context.';
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'No Transformation ID provided in the URL.'
      });
    }
  }

  async onEditorInit(
    editorInstance: editor.IStandaloneCodeEditor,
    monaco = (window as any).monaco
  ): Promise<void> {
    this.monacoInstance = editorInstance;
    this.monaco = monaco;
    console.log('Monaco editor instance initialized', editorInstance);

    console.log('[ScriptEditorPage] Subscribing to type definition updates...');
    this.subscriptions.add(
      this.monacoEditorService.typeUpdateRequested$.subscribe(response => {
        if(!response){
          return;
        }
        console.log('[ScriptEditorPage] Received type update request. Applying to Monaco instance.');
        this.applyTypeDefinitions(response);
      })
    );

    monaco.languages.typescript.typescriptDefaults.setCompilerOptions({
      target: monaco.languages.typescript.ScriptTarget.ESNext,
      allowNonTsExtensions: true,
      moduleResolution: monaco.languages.typescript.ModuleResolutionKind.NodeJs,
      module: monaco.languages.typescript.ModuleKind.CommonJS,
      noEmit: true,
      esModuleInterop: true,
      // Add essential libs
      lib: ['es2020', 'dom'],
    });

    monaco.languages.typescript.typescriptDefaults.setDiagnosticsOptions({
      noSemanticValidation: false,
      noSyntaxValidation: false,
    });

    this.addGlobalApiDefinitions();
    this.arcStateService.initializeMonaco(monaco);
    this.monacoInstance?.onDidChangeModelContent(() => this.onModelChange.next());

    this.arcStateService.initializeGlobalSourceType().subscribe();

    if (this.currentTransformationId) {
      console.log(`Editor is ready. Loading context for Transformation ID: ${this.currentTransformationId}`);
      this.loadContextForScript(this.currentTransformationId);
    }
  }

  handleCreateArc(context: { system: SourceSystem; endpoint: SourceSystemEndpoint }): void {
    this.wizardContext = context;
    this.isWizardVisible = true;
  }

  handleEditArc(context: { system: SourceSystem; endpoint: SourceSystemEndpoint; arc: ApiRequestConfiguration }): void {
    this.wizardContext = { ...context, arcToEdit: context.arc };
    this.isWizardVisible = true;
  }

  handleCloneArc(context: {
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arc: ApiRequestConfiguration;
  }): void {
    console.log('%c[Editor] Clone requested for:', 'color: #8b5cf6;', context.arc);

    this.wizardContext = {
      system: context.system,
      endpoint: context.endpoint,
      arcToClone: context.arc,
    };

    this.isWizardVisible = true;
  }

  handleDeleteArc(context: { arc: ApiRequestConfiguration }): void {
    const arc = context.arc;
    this.scriptEditorService.checkArcUsage(arc.id).pipe(
      switchMap(usages => {
        let message = `Are you sure you want to delete the ARC "${arc.alias}"?`;
        
        if (usages.length > 0) {
          const usageList = usages.map(u => `<li>${u.scriptName}</li>`).join('');
          message = `
            <p><b>Warning:</b> This ARC is currently used in the following scripts:</p>
            <ul>${usageList}</ul>
            <p>Deleting it may cause these scripts to fail. Do you still want to proceed?</p>
          `;
        }

        this.confirmationService.confirm({
          header: 'Confirm Deletion',
          icon: 'pi pi-exclamation-triangle',
          message: message,
          accept: () => {
            this.isLoading = true;
            this.loadingMessage = `Deleting ARC "${arc.alias}"...`;
            this.scriptEditorService.deleteArc(arc.id).subscribe({
              next: () => {
                this.arcStateService.removeArc(arc);
                this.messageService.add({ severity: 'success', summary: 'Deleted', detail: 'ARC has been successfully deleted.' });
                this.isLoading = false;
              },
              error: (err) => {
                this.messageService.add({ severity: 'error', summary: 'Delete Failed', detail: err.error?.message || 'Could not delete the ARC.' });
                this.isLoading = false;
              }
            });
          },
          reject: () => {
            // user cancellation
          }
        });
        return of(null);
      })
    ).subscribe();
  }

  handleArcSave(savedArc: ApiRequestConfiguration): void {
    console.log('%c[Editor] Handling saved ARC:', 'color: #10b981;', savedArc);

    this.arcManagementPanel.allSourceSystems$.pipe(
      first()
    ).subscribe(allSystems => {
      const parentSystem = allSystems.find(s => s.name === savedArc.sourceSystemName);

      if (parentSystem) {
        this.arcManagementPanel.ensureEndpointsLoaded(parentSystem);
      }

      this.arcStateService.addOrUpdateArc(savedArc);

      this.messageService.add({
        severity: 'success',
        summary: 'ARC Saved',
        detail: `Configuration '${savedArc.alias}' is now available.`
      });
    });
  }

  handleCreateAasArc(context: { system: SourceSystem; submodel: SubmodelDescription }): void {
    console.log(context);
    this.aasWizardContext = {
        system: context.system,
        submodel: context.submodel,
        arcToEdit: undefined
    };
    this.isAasWizardVisible = true;
  }

  handleEditAasArc(context: { system: SourceSystem; submodel: SubmodelDescription; arc: AasArc }): void {
    this.aasWizardContext = {
        system: context.system,
        submodel: context.submodel,
        arcToEdit: context.arc
    };
    this.isAasWizardVisible = true;
  }
  
  handleDeleteAasArc(context: { arc: AasArc }): void {
    const arc = context.arc;
    
    this.confirmationService.confirm({
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      message: `Are you sure you want to delete the AAS ARC "${arc.alias}"?`,
      accept: () => {
        this.isLoading = true;
        this.loadingMessage = `Deleting AAS ARC "${arc.alias}"...`;
        
        this.aasService.deleteAasArc(arc.id).subscribe({
          next: () => {
            this.arcStateService.removeArc(arc);
            this.messageService.add({ severity: 'success', summary: 'Deleted', detail: 'AAS ARC has been deleted.' });
            this.isLoading = false;
          },
          error: (err) => {
            this.messageService.add({ severity: 'error', summary: 'Delete Failed', detail: err.error?.message || 'Could not delete the AAS ARC.' });
            this.isLoading = false;
          }
        });
      }
    });
  }

  handleAasArcSave(savedArc: AasArc): void {
    this.arcStateService.addOrUpdateArc(savedArc as AnyArc);
    this.messageService.add({
      severity: 'success',
      summary: 'AAS ARC Saved',
      detail: `Configuration '${savedArc.alias}' is now available.`
    });
  }

  private applyTypeDefinitions(response: TypeDefinitionsResponse): void {
    if (!this.monaco){
      console.error("Cannot apply types, monaco instance is not available.");
      return;
    }

    if (response && response.libraries) {
      console.log(`[Monaco] Applying ${response.libraries.length} type definitions from backend.`);

      for (const newLib of response.libraries) {
        const libUri = `file:///${newLib.filePath}`;

        const existingLibIndex = this.currentExtraLibs.findIndex(lib => lib.uri === libUri);

        if (existingLibIndex > -1) {
          console.log(`[Monaco] Replacing existing library: ${libUri}`);
          const oldLib = this.currentExtraLibs[existingLibIndex];
          oldLib.disposable.dispose();

          this.currentExtraLibs.splice(existingLibIndex, 1);
        }
        const newDisposable = this.monaco.languages.typescript.typescriptDefaults.addExtraLib(
          newLib.content,
          libUri
        );
        this.currentExtraLibs.push({ uri: libUri, disposable: newDisposable });
      }

      console.log('[Monaco] Type definitions applied successfully.');
    }
  }

  private loadContextForScript(transformationId: string): void {
    this.isLoading = true;
    this.loadingMessage = `Loading context for Transformation with ID: ${transformationId}...`;

    this.scriptEditorService.getScriptForTransformation(Number(transformationId))
      .pipe(
        finalize(() => this.isLoading = false)
      )
      .subscribe({
        next: (payload) => {
          this.scriptPayload = payload;
          this.code = payload.typescriptCode || this.generateDefaultScriptTemplate();
          this.analyzeEditorContentForTypes();
        },
        error: (err) => {
          console.error('Failed to load script context', err);
          this.code = `// ERROR: Could not load script context for Transformation ID ${transformationId}.`;
          this.messageService.add({ severity: 'error', summary: 'Load Error', detail: 'Script context could not be loaded.' });
        }
      });
  }

  async saveScript(): Promise<void> {
    if (!this.currentTransformationId) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'No transformation context found. Cannot save.'
      });
      return;
    }
    if (this.isSaving) {
      return;
    }

    const activeTargetArcIds = this.targetArcPanel.activeArcs.map(arc => arc.id);

    let scriptStatus: 'DRAFT' | 'VALIDATED' = 'DRAFT';
    let transpiledCode = '';
    let hasErrors = true;

    try {
      hasErrors = await this.hasValidationErrors();
      if (hasErrors){
        this.messageService.add({
        severity: 'error',
        summary: 'Validation Failed',
        detail: 'Please fix the TypeScript errors before saving.'
      });
      }
      const transpileOutput = ts.transpileModule(this.code, {
        compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ESNext},
      });
      transpiledCode = transpileOutput.outputText;

      if(!hasErrors){
        scriptStatus = 'VALIDATED';
      }
    } catch (e){
      scriptStatus = 'DRAFT';
    }

    const codeToSave = this.code;
    const arcPattern = /\bsource\.([a-zA-Z0-9_]+)\.([a-zA-Z0-9_]+)/g;
    const matches = codeToSave.matchAll(arcPattern);

    const requiredArcSet = new Set<string>();
    for (const match of matches){
      const systemName = match[1];
      const arcName = match[2];
      requiredArcSet.add(`${systemName}.${arcName}`);
    }

    const requiredArcAliases = Array.from(requiredArcSet);
    console.log('%c[Editor] Extracted ARC dependencies:', 'color: #f97316;', requiredArcAliases);

    const payload: ScriptPayload = {
        name: this.scriptPayload!.name,
        typescriptCode: this.code,
        javascriptCode: transpiledCode,
        requiredArcAliases: requiredArcAliases,
        status: scriptStatus,
        targetArcIds: activeTargetArcIds
      };

    this.isSaving = true;
    this.scriptEditorService.saveScriptForTransformation(Number(this.currentTransformationId), payload).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Saved!',
            detail: 'Script has been saved successfully.'
          });

          this.isSaving = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Save Failed',
            detail: 'Could not save the script to the server.'
          });
          console.error('Save script error:', err);
          this.isSaving = false;
          this.cdr.detectChanges();
        }
      });
  }

  // --- Helper & Private Methods ---

  private analyzeEditorContentForTypes(): void {
    const code = this.monacoInstance?.getValue() || '';
    const matches = code.matchAll(/\bsource\.([a-zA-Z0-9_-]+)\b/g);

    const systemNamesInEditor = new Set<string>();
    for (const match of matches) {
      // TODO: control for js sanitization of identifier names. Requires proper naming convention for Types
      systemNamesInEditor.add(match[1]);
    }

    if (systemNamesInEditor.size > 0) {
      console.log('%c[Editor] Detected system names:', 'color: #eab308;', Array.from(systemNamesInEditor));
      this.arcStateService.loadTypesForSourceSystemNames(Array.from(systemNamesInEditor)).subscribe();
    }
  }

  private addExtraLib(content: string, uri: string): void {
    if (!this.monaco || this.currentExtraLibs.find(lib => lib.uri === uri)) {
      return;
    }
    const disposable = this.monaco.languages.typescript.typescriptDefaults.addExtraLib(content, uri);
    this.currentExtraLibs.push({uri, disposable});
  }

  private addGlobalApiDefinitions(): void {
    const stayinsyncApiDts = `
      /** Provides logging and output capabilities for your script. */
      declare const stayinsync: {
          /**
           * Logs a message to the SyncJob's execution history.
           * @param message The message to log.
           * @param logLevel The severity of the log. Defaults to 'INFO'.
           */
          log: (message: any, logLevel?: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG') => void;
          /**
           * Sets the final output of the transformation script.
           * This data will be passed to the destination system.
           * @param outputData The resulting JSON object or array. Can be null.
           */
          setOutput: (outputData: any) => void;
      };
    `;
    this.addExtraLib(stayinsyncApiDts, 'file:///global-stayinsync-api.d.ts');
  }

  private generateDefaultScriptTemplate(): string {
    return `/**
 * ==========================================================================================
 * Stay-in-Sync Transformation Script
 * ==========================================================================================
 *
 * Your mission is to implement the 'transform' function.
 * This function reads data from 'source' objects and MUST return a 'DirectiveMap' object,
 * which contains arrays of directives for your configured target systems.
 *
 * Golden Rule: The return value of this function IS the output.
 *
 */

/**
 * Transforms source data into a map of instructions (Directives) for the target systems.
 * @returns {DirectiveMap} An object where keys are your Target ARC aliases and
 *                         values are arrays of the corresponding directives.
 */
function transform(): DirectiveMap {
    stayinsync.log('Transformation started...', 'INFO');

    // --- EXAMPLE WORKFLOW (Commented Out) ---
    /*

    // 1. Get and filter your source data
    // const activeProducts = s0urce.myCrm.allProducts.payload.filter(p => p.isActive);

    // 2. Map the data to an array of directives for a specific target
    // const productDirectives = activeProducts.map(product => {
    //     return targets.synchronizeProducts.defineUpsert()
    //         .usingCheck(check => check.withQueryParamId(product.id))
    //         .onCreate(create => create.withPayload({ ...product }))
    //         .onUpdate(update => update.withPathId(res => res.body.id).withPayload({ ...product }))
    //         .build();
    // });

    */

    // 3. Return the DirectiveMap object. The keys MUST match your target ARC aliases.
    // Auto-complete will guide you when you type 'return { }'.
    return {
        // synchronizeProducts: productDirectives,
        // anotherArcAlias: []
    };
}
`;
  }

  private async hasValidationErrors(): Promise<boolean> {
    if (!this.monacoInstance || !this.monaco) return true;

    const model = this.monacoInstance.getModel();
    if (!model) return true;

    const worker = await this.monaco.languages.typescript.getTypeScriptWorker();
    const client = await worker(model.uri);
    const diagnostics = await Promise.all([
      client.getSyntacticDiagnostics(model.uri.toString()),
      client.getSemanticDiagnostics(model.uri.toString()),
    ]).then(([syntactic, semantic]) => [...syntactic, ...semantic]);

    const errors = diagnostics.filter(d => d.category === ts.DiagnosticCategory.Error);
    return errors.length > 0;
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.currentExtraLibs.forEach(lib => lib.disposable.dispose());
  }
}
