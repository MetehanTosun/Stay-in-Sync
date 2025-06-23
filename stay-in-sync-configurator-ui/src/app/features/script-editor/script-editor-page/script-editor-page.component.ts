import {
  Component,
  OnDestroy,
  ChangeDetectorRef,
  Input,
  OnChanges,
  SimpleChanges,
  OnInit,
  AfterViewInit,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import type { editor, IDisposable } from 'monaco-editor';
import * as ts from 'typescript';
import {
  ScriptEditorService,
  ScriptPayload,
} from '../../../core/services/script-editor.service';
import { MessagesModule } from 'primeng/messages';
import { MessageService } from 'primeng/api';
import { catchError, finalize, forkJoin, of, Subscription, tap } from 'rxjs';

// PrimeNG Modules
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { SplitterModule } from 'primeng/splitter';
import { AccordionModule } from 'primeng/accordion';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';

// Monaco Editor Module
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';

import {
  SyncJobContextPanelComponent,
  SyncJobContextData,
} from '../sync-job-context-panel/sync-job-context-panel.component';

interface MonacoExtraLib {
  uri: String;
  disposable: IDisposable;
}

interface Message {
  severity?: 'success' | 'info' | 'warn' | 'error';
  summary?: string;
  detail?: string;
  life?: number;
}

@Component({
  selector: 'app-script-editor-step',
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
    SyncJobContextPanelComponent,
    ToastModule,
  ],
})
export class ScriptEditorPageComponent implements OnInit, OnChanges, OnDestroy {
  @Input() syncJobId: string | null = null;

  editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    minimap: { enabled: true },
  };

  private monaco!: typeof import('monaco-editor');
  private monacoInstance: editor.IStandaloneCodeEditor | undefined;
  private currentExtraLibs: MonacoExtraLib[] = [];
  private subscriptions = new Subscription();

  executionResult: any = null;
  executionError: any = null;
  analysisMessages: Message[] = [];
  analysisResults: string[] = [];

  isLoading: boolean = false;
  isSaving: boolean = false;
  loadingMessage = 'Initializing...';
  code: string = `// Script editor will initialize once a SyncJob context is loaded.`;
  currentSyncJobContextData: SyncJobContextData | null = null;

  constructor(
    private cdr: ChangeDetectorRef,
    private scriptEditorService: ScriptEditorService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.syncJobId = 'activeJob123';
    if (this.syncJobId) {
      this.loadContextForSyncJob(this.syncJobId);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['syncJobId'] && changes['syncJobId'].currentValue) {
      this.loadContextForSyncJob(changes['syncJobId'].currentValue);
    } else if (changes['syncJobId'] && !changes['syncJobId'].currentValue) {
      this.resetEditorState();
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
    if(this.syncJobId){
      this.loadContextForSyncJob('activeJob123');
    }
    setTimeout(()=>{
        this.monacoInstance?.layout();
      }, 100);
  }

  private loadContextForSyncJob(jobId: string): void {
    if (!jobId || !this.monaco) {
      return;
    }

    this.isLoading = true;
    this.loadingMessage = `Loading context for SyncJob: ${jobId}...`;
    this.resetEditorState(true); // Soft reset, keeps global libs

    const jobLoader$ = forkJoin({
      context: this.scriptEditorService.getSyncJobContext(jobId),
      typeDefs: this.scriptEditorService.getTypeDefinitions(jobId),
      savedScript: this.scriptEditorService.getSavedScript(jobId),
    }).pipe(
      tap(({ context, typeDefs, savedScript }) => {
        // 1. Set context data for the side panel
        this.currentSyncJobContextData = context;

        // 2. Add the dynamic type definitions for this job
        this.addExtraLib(typeDefs, `file:///syncjob-${jobId}-sources.d.ts`);

        // 3. Set the editor code
        this.code = savedScript
          ? savedScript.typescriptCode
          : this.generateDefaultScriptTemplate(context);
        
        this.messageService.add({ severity: 'success', summary: 'Context Loaded', detail: `Successfully loaded context for ${context.syncJobName}.` });
      }),
      catchError(error => {
        console.error(`Error loading context for SyncJob ${jobId}:`, error);
        this.code = `// ERROR: Failed to load context for SyncJob ${jobId}.\n// Check browser console for details.`;
        this.messageService.add({ severity: 'error', summary: 'Load Failed', detail: error.message || 'Could not load SyncJob context.' });
        return of(null);
      }),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    );
    
    this.subscriptions.add(jobLoader$.subscribe());
  }

  async saveScript(): Promise<void> {
    if (!this.syncJobId || this.isSaving) {
        return;
    }

    // Step 1: Client-side validation using Monaco's diagnostics
    const hasErrors = await this.hasValidationErrors();
    if (hasErrors) {
        this.messageService.add({ severity: 'error', summary: 'Validation Failed', detail: 'Please fix the TypeScript errors before saving.' });
        return;
    }

    this.isSaving = true;
    try {
        // Step 2: Transpile TypeScript to JavaScript
        const transpileOutput = ts.transpileModule(this.code, {
            compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ESNext },
        });

        // Step 3: Generate a hash (placeholder for a real hashing function)
        const jsHash = await this.generateHash(transpileOutput.outputText);

        const payload: ScriptPayload = {
            typescriptCode: this.code,
            javascriptCode: transpileOutput.outputText,
            hash: jsHash,
        };

        // Step 4: Call the service to save the script
        this.scriptEditorService.saveScript(this.syncJobId, payload).subscribe({
            next: () => {
                this.messageService.add({ severity: 'success', summary: 'Saved!', detail: 'Script has been saved successfully.' });
                this.isSaving = false;
                this.cdr.detectChanges();
            },
            error: (err) => {
                this.messageService.add({ severity: 'error', summary: 'Save Failed', detail: 'Could not save the script to the server.' });
                console.error('Save script error:', err);
                this.isSaving = false;
                this.cdr.detectChanges();
            }
        });

    } catch (e) {
        this.messageService.add({ severity: 'error', summary: 'Transpilation Error', detail: 'An error occurred during script transpilation.' });
        console.error('Error in saveScript:', e);
        this.isSaving = false;
    }
  }

  // --- Helper & Private Methods ---

  private resetEditorState(isChangingJob = false): void {
    // Dispose only job-specific libs, not global ones
    const libsToRemove = this.currentExtraLibs.filter(lib => !lib.uri.includes('global-'));
    libsToRemove.forEach(lib => lib.disposable.dispose());
    this.currentExtraLibs = this.currentExtraLibs.filter(lib => lib.uri.includes('global-'));

    this.currentSyncJobContextData = null;
    this.code = isChangingJob ? '// Loading new context...' : '// Please select a SyncJob to begin.';
    this.cdr.detectChanges();
  }

  private addExtraLib(content: string, uri: string): void {
    if (!this.monaco || this.currentExtraLibs.find(lib => lib.uri === uri)) {
      return;
    }
    const disposable = this.monaco.languages.typescript.typescriptDefaults.addExtraLib(content, uri);
    this.currentExtraLibs.push({ uri, disposable });
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

  private generateDefaultScriptTemplate(context: SyncJobContextData): string {
    const sourceAliases = context.sourceSystems.flatMap(s => s.dataEntities.map(e => e.aliasInScript));
    return `/**
 * Transformation Script for SyncJob: ${context.syncJobName}
 * 
 * Available source data objects:
 * ${sourceAliases.map(alias => ` *  - sourceData.${alias}`).join('\n')}
 * 
 * Available API functions:
 *  - stayinsync.log('Your message', 'INFO' | 'WARN' | 'ERROR')
 *  - stayinsync.setOutput({ ... your final JSON object ... })
 */
async function transformData() {
    stayinsync.log('Starting transformation for job: ${context.syncJobName}');

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

    // Filter only for errors. Warnings are acceptable.
    const errors = diagnostics.filter(d => d.category === ts.DiagnosticCategory.Error);
    return errors.length > 0;
  }

  private async generateHash(data: string): Promise<string> {
    // In a real app, use a proper crypto library like `crypto-js` or the browser's SubtleCrypto API.
    // This is a simple placeholder.
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
