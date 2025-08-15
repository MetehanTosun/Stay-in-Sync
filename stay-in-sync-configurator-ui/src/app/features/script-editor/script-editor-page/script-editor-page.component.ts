import {ChangeDetectorRef, Component, inject, OnDestroy, OnInit, ViewChild,} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import type {editor, IDisposable} from 'monaco-editor';
import * as ts from 'typescript';
import {ScriptEditorService, ScriptPayload,} from '../../../core/services/script-editor.service';
import {MessagesModule} from 'primeng/messages';
import {MessageService} from 'primeng/api';

import {ActivatedRoute, Router} from '@angular/router';
import {ScriptEditorData} from '../script-editor-navigation.service';

import {catchError, debounceTime, finalize, of, Subject, Subscription, tap, throwError} from 'rxjs';

// PrimeNG Modules
import {PanelModule} from 'primeng/panel';
import {ButtonModule} from 'primeng/button';
import {SplitterModule} from 'primeng/splitter';
import {AccordionModule} from 'primeng/accordion';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {ToastModule} from 'primeng/toast';

// Monaco Editor Module
import {MonacoEditorModule} from 'ngx-monaco-editor-v2';

import {ApiRequestConfiguration} from '../models/arc.models';
import {ArcStateService} from '../../../core/services/arc-state.service';
import {SourceSystem, SourceSystemEndpoint} from '../../source-system/models/source-system.models';
import {ArcManagementPanelComponent} from '../arc-management-panel/arc-management-panel.component';
import {ArcWizardComponent} from '../arc-wizard/arc-wizard.component';
import {InputTextModule} from 'primeng/inputtext';
import {FloatLabel} from 'primeng/floatlabel';
import { MonacoEditorService } from '../../../core/services/monaco-editor.service';
import { TypeDefinitionsResponse } from '../models/target-system.models';
import { TargetArcPanelComponent } from '../target-arc-panel/target-arc-panel.component';

interface MonacoExtraLib {
  uri: String;
  disposable: IDisposable;
}

@Component({
  selector: 'app-script-editor-step', // might be without step
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
    ToastModule,
    InputTextModule,
    FloatLabel,
    TargetArcPanelComponent
  ],
})
export class ScriptEditorPageComponent implements OnInit, OnDestroy {
  @ViewChild(TargetArcPanelComponent) targetArcPanel!: TargetArcPanelComponent;

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
    arcToClone?: ApiRequestConfiguration
  } | null | undefined = null;

  private onModelChange = new Subject<void>();

  private cdr = inject(ChangeDetectorRef);
  private scriptEditorService = inject(ScriptEditorService);
  private messageService = inject(MessageService);
  private arcStateService = inject(ArcStateService);
  private monacoEditorService = inject(MonacoEditorService);

  private route = inject(ActivatedRoute);
  private router = inject(Router);

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

  handleCloneArc(context: { arc: ApiRequestConfiguration }): void {
    // TODO: Implement proper arc fetch and inject for selected arc inside arc wizard with the same data, but new entity
    // this.wizardContext = { ... };
    // this.isWizardVisible = true;
  }

  handleArcSave(savedArc: ApiRequestConfiguration): void {
    console.log('%c[Editor] Handling saved ARC:', 'color: #10b981;', savedArc);
    this.arcStateService.addOrUpdateArc(savedArc);
    this.messageService.add({
      severity: 'success',
      summary: 'ARC Saved',
      detail: `Configuration '${savedArc.alias}' is now available.`
    });
  }

  private applyTypeDefinitions(response: TypeDefinitionsResponse): void {
    if (!this.monaco){
      console.error("Cannot apply types, monaco instance is not available.");
      return;
    }
    console.log("HELOOOOOOOOOOOOOOOOOO");7
    if (response && response.libraries) {
      for (const lib of response.libraries) {
        this.monaco.languages.typescript.typescriptDefaults.addExtraLib(lib.content, `file:///${lib.filePath}`);
      }
      console.log('Target type definitions successfully applied to Monaco editor.');
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
 * Welcome to the Script Editor!
 * 
 * Your mission is to write a function named 'transform'.
 * This function will read data from your Source Systems (via the 'source' object)
 * and create a list of instructions ("Directives") for your Target Systems
 * (via the 'targets' object).
 * 
 * ------------------------------------------------------------------------------------------
 *                            GOLDEN RULES
 * ------------------------------------------------------------------------------------------
 * 1. The function MUST be named 'transform'.
 * 2. It MUST return an array of Directives.
 * 3. Activate your Data Sources (left panel) and Target Directives (right panel)
 *    to populate the 'source' and 'targets' objects and enable auto-complete.
 * ------------------------------------------------------------------------------------------
 * 
 */

/**
 * This function transforms source data into a list of instructions (Directives)
 * for the target systems to execute.
 * 
 * @returns {any[]} An array of Directive objects.
 */
function transform() {
    stayinsync.log('Transformation started...');

    // You will collect all instructions to be executed in this array.
    const directives = [];

    // --- EXAMPLE WORKFLOW (Commented Out) ---
    // Please uncomment the following sections once you have configured your systems.

    /*
    
    // --- 1. GET DATA FROM THE SOURCE ---
    // Access your configured data sources via the global 'source' object.
    // Replace 'myCrmSystem' and 'allCustomers' with your actual aliases.
    
    // const customerData = source.myCrmSystem.allCustomers.payload;
    // stayinsync.log(\`Loaded \${customerData.length} customers from the CRM.\`);

    
    // --- 2. MAP SOURCE ITEMS TO TARGET DIRECTIVES ---
    // Loop through your source data (e.g., with .map()) and for each item,
    // create an instruction for the target system.

    // const customerDirectives = customerData.map(customer => {

        // Access your target configurations via the global 'targets' object.
        // Replace 'myErpSystem' with your actual alias.
        // 'defineUpsert()' starts the "check-then-create-or-update" process.
        
        // return targets.myErpSystem.defineUpsert()

            // .usingCheck(): Defines the GET call to check for existence.
            // The methods (.with...()) are auto-generated from your OpenAPI specification.
            // .usingCheck(check => {
            //     check.withQueryParamCustomerNumber(customer.id);
            // })

            // .onCreate(): Defines the POST call if the customer does not exist.
            // .onCreate(create => {
            //     create.withPayload({
            //         id: customer.id,
            //         name: customer.companyName,
            //         contact: customer.contactPerson,
            //         isActive: true
            //     });
            // })

            // .onUpdate(): Defines the PUT call if the customer already exists.
            // .onUpdate(update => {
            //     update.withPathId(checkResponse => checkResponse.body.internalId)
            //           .withPayload({
            //              contact: customer.contactPerson
            //           });
            // })

            // .build() finalizes the Directive's definition.
            // .build();
    // });

    // directives.push(...customerDirectives);
    
    */

    stayinsync.log('Transformation finished. Returning directives now.');

    // The function must always return an array, even if it's empty.
    return directives;
}

// The synchronization engine will call your 'transform' function automatically.
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

  private async generateHash(data: string): Promise<string> {
    // Placeholder, switching to crypto library like `crypto-js` or the browser's SubtleCrypto API.
    const buffer = new TextEncoder().encode(data);
    const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.currentExtraLibs.forEach(lib => lib.disposable.dispose());
  }
}
