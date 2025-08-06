import {ChangeDetectorRef, Component, inject, OnDestroy, OnInit,} from '@angular/core';
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
    FloatLabel
  ],
})
export class ScriptEditorPageComponent implements OnInit, OnDestroy {

  currentTransformationId: string | null = null;
  preloadedData?: ScriptEditorData;

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
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    this.preloadedData = navigation?.extras?.state?.['scriptData'];
    console.log('Preloaded data from state: ', this.preloadedData);

    if (this.preloadedData?.scriptName) {
      this.loadingMessage = `Loading ${this.preloadedData.scriptName}...`;
    }

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
      this.loadContextForScript(this.currentTransformationId).subscribe();
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

  private loadContextForScript(transformationId: string) {
    if (!transformationId) {
      return of(null);
    }

    this.isLoading = true;
    this.loadingMessage = `Loading context for Transformation with ID: ${transformationId}...`;

    return this.scriptEditorService.getScriptForTransformation(Number(transformationId)).pipe(
      catchError(error => {
        if (error.status === 404) return of(null);
        return throwError(() => error);
      }),
      tap(savedScript => {
        const scriptName = this.preloadedData?.scriptName;
        if (savedScript?.typescriptCode) {
          this.code = savedScript.typescriptCode;
          this.analyzeEditorContentForTypes();
        } else {
          this.code = this.generateDefaultScriptTemplate(scriptName);
        }
      }),
      finalize(() => this.isLoading = false)
    );
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

    const hasErrors = await this.hasValidationErrors();
    if (hasErrors) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Failed',
        detail: 'Please fix the TypeScript errors before saving.'
      });
      return;
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

    try {
      const transpileOutput = ts.transpileModule(codeToSave, {
        compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ESNext},
      });

      //https://github.tik.uni-stuttgart.de/st189097/StuPro/issues/245
      //const jsHash = await this.generateHash(transpileOutput.outputText);
      const jsHash = "thisIsAHash";

      const payload: ScriptPayload = {
        name: this.preloadedData?.scriptName,
        typescriptCode: codeToSave,
        javascriptCode: transpileOutput.outputText,
        hash: jsHash,
        requiredArcAliases: requiredArcAliases
      };

      console.log(payload);

      this.isSaving = true;

      this.scriptEditorService.saveScriptForTransformation(Number(this.currentTransformationId), payload).subscribe({
        next: (savedScript) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Saved!',
            detail: 'Script has been saved successfully.'
          });

          if (this.preloadedData) {
            this.preloadedData.id = savedScript.id;
          }

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

    } catch (e) {
      this.messageService.add({
        severity: 'error',
        summary: 'Transpilation Error',
        detail: 'An error occurred during script transpilation.'
      });
      console.error('Error in saveScript:', e);
      this.isSaving = false;
    }
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

  private generateDefaultScriptTemplate(scriptName: string | undefined): string {
    return `/**
 * Transformation Script for SyncJob: ${scriptName}
 *
 * Available API functions:
 *  - stayinsync.log('Your message', 'INFO' | 'WARN' | 'ERROR')
 *  - stayinsync.setOutput({ ... your final JSON object ... })
 */
async function transformData() {
    stayinsync.log('Starting transformation for job: ${scriptName}');

    // --- YOUR TRANSFORMATION LOGIC STARTS HERE ---

    // Example: Accessing data from a source system.
    // Replace 'crmCustomer' with an alias from your context.
    // const customer = sourceData.crmCustomer;

    // if (customer) {
    //   const output = {
    //      id: customer.id,
    //      fullName: customer.name,
    //      isContactable: customer.isActive && customer.email,
    //   };
    //   stayinsync.setOutput(output);
    // } else {
    //   stayinsync.log('Source data not found or invalid.', 'WARN');
    //   stayinsync.setOutput(null); // Explicitly set output to null
    // }

    // --- YOUR TRANSFORMATION LOGIC ENDS HERE ---

    // Don't forget to call setOutput!
    stayinsync.setOutput({ message: "Transformation complete. Replace this with your actual output." });
}

// Execute the transformation function.
transformData();
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
