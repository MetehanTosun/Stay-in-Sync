import { Component, OnDestroy, ChangeDetectorRef, Input, OnChanges, SimpleChanges, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import type { editor, languages, IDisposable } from 'monaco-editor';
import * as ts from 'typescript';

// PrimeNG Modules
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { SplitterModule } from 'primeng/splitter';
import { AccordionModule } from 'primeng/accordion';
import { MessagesModule } from 'primeng/messages';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

// Monaco Editor Module
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';

import { SyncJobContextPanelComponent, SyncJobContextData } from '../sync-job-context-panel/sync-job-context-panel.component';

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
  ]
})
export class ScriptEditorPageComponent implements OnInit, OnChanges, OnDestroy {
  @Input() syncJobId: string | null = null;

  editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    minimap: { enabled: true }
  };
  code: string = `// Script editor will initialize once a SyncJob context is loaded.`;

  private monaco!: typeof import('monaco-editor');
  private monacoInstance: editor.IStandaloneCodeEditor | undefined;
  private currentExtraLibs: MonacoExtraLib[] = [];
  
  executionResult: any = null;
  executionError: any = null;
  analysisMessages: Message[] = [];
  analysisResults: string[] = [];

  isLoading: boolean = false;
  currentSyncJobContextData: SyncJobContextData | null = null;

  constructor(
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    if(this.syncJobId){
      this.loadContextForSyncJob(this.syncJobId);
    } else {
      this.code = "// Please ensure a SyncJob is active to use the script editor."
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if(changes['syncJobId'] && changes['syncJobId'].currentValue){
      const newJobId = changes['syncJobId'].currentValue;
      const oldJobId = changes['syncJobId'].previousValue;
      if(newJobId !== oldJobId){
        this.loadContextForSyncJob(newJobId);
      }
    } else if (changes['syncJobId'] && !changes['syncJobId'].currentValue){
      this.resetEditorState();
    }
  }

  async onEditorInit(editorInstance: editor.IStandaloneCodeEditor, monaco = (window as any).monaco): Promise<void> {
    this.monacoInstance = editorInstance;
    this.monaco = monaco;
    console.log('Monaco editor instance initialized', editorInstance);

    monaco.languages.typescript.typescriptDefaults.setCompilerOptions({})
  }

  private resetEditorState(): void {
    this.currentExtraLibs.forEach(lib => lib.disposable.dispose());
    this.currentExtraLibs = [];
    this.addGlobalApiDefinitions();
    this.code = "// No active SyncJob. Please configure a SyncJob to proceed.";
    this.currentSyncJobContextData = null;
    this.executionResult = null;
    this.executionError = null;
    this.analysisMessages = [];
    this.analysisResults = [];
    this.cdr.detectChanges();
  }

  private addGlobalApiDefinitions(): void {
    if (!this.monaco) return;
    const stayinsyncApiDts = `
        declare const stayinsync: {
            log: (message: string, logLevel?: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG' | 'TRACE') => void;
            setOutput: (outputData: any) => void;
        };
    `;
    const uri = 'file:///global-stayinsync-api.d.ts'; // to be implemented
    if (!this.currentExtraLibs.find(lib => lib.uri === uri)) {
        const libDisposable = this.monaco.languages.typescript.typescriptDefaults.addExtraLib(stayinsyncApiDts, uri);
        this.currentExtraLibs.push({ uri: uri, disposable: libDisposable });
    }
  }

  async loadContextForSyncJob(jobId: string): Promise<void> {
    if (!jobId || !this.monaco) {
      console.warn('Cannot load context: No Job ID or Monaco not ready.');
      this.resetEditorState();
      return;
    }

    this.isLoading = true;
    this.executionError = null; 
    this.executionResult = null;
    this.analysisMessages = [];
    this.analysisResults = [];
    this.currentSyncJobContextData = null;
    this.cdr.detectChanges();

    const libsToRemove = this.currentExtraLibs.filter(lib => !lib.uri.includes('global-'));
    libsToRemove.forEach(lib => lib.disposable.dispose());

    this.currentExtraLibs = this.currentExtraLibs.filter(lib => lib.uri.includes('global-'));

    try {
      const dtsContent = await this.fetchMockDtsForJob(jobId); // MOCK

      if (dtsContent) {
        const jobSpecificUri = `file:///syncjob-${jobId}-sources.d.ts`; // to be implemented
        if (!this.currentExtraLibs.find(lib => lib.uri === jobSpecificUri)) {
            const libDisposable = this.monaco.languages.typescript.typescriptDefaults.addExtraLib(dtsContent, jobSpecificUri);
            this.currentExtraLibs.push({ uri: jobSpecificUri, disposable: libDisposable });
            console.log(`Loaded .d.ts for SyncJob ${jobId} with URI: ${jobSpecificUri}`);
        } else {
            console.log(`.d.ts for SyncJob ${jobId} already loaded.`);
        }
      }

      // Fetch detailed context data for the right-hand panel
      // this.currentSyncJobContextData = await this.syncJobService.getContextDetailsForJob(jobId).toPromise();
      this.currentSyncJobContextData = await this.fetchMockSyncJobContextData(jobId); // MOCK

      this.code = `// SyncJob: ${this.currentSyncJobContextData?.syncJobName || jobId}
// Type definitions loaded. Access source data via 'sourceData.yourAlias'.
// Use 'stayinsync.log(...)' and 'stayinsync.setOutput(...)'.

async function transformData() {
    stayinsync.log('Starting transformation for job ${jobId}', 'INFO');

    // Example: Access data based on your SyncJob's configuration
    // if (sourceData.crmCustomer && sourceData.crmCustomer.isActive) {
    //   const output = { ...sourceData.crmCustomer, processed: true };
    //   stayinsync.setOutput(output);
    // } else {
    //   stayinsync.log('Customer not active or not found.', 'WARN');
    //   stayinsync.setOutput(null);
    // }
    stayinsync.setOutput({ greeting: "Hello from " + sourceData.systemA_customer.name }); // Adjust based on mock
}

transformData();
`;
    } catch (error) {
      console.error(`Error loading context for SyncJob ${jobId}:`, error);
      this.executionError = { message: `Failed to load context for SyncJob ${jobId}.` };
      this.code = `// Error loading context for SyncJob ${jobId}. Check console.`;
    } finally {
      this.isLoading = false;
      this.cdr.detectChanges();
    }
  }

  // MOCK: Replace with actual service call
  async fetchMockDtsForJob(jobId: string): Promise<string> {
    await new Promise(resolve => setTimeout(resolve, 500));
    if (jobId === 'activeJob123') {
        return `
            // Specific types for Job 'activeJob123'
            declare namespace CrmSystemTypes {
                interface Customer { id: string; name: string; email?: string; lastContactDate: string; isActive: boolean; }
            }
            declare namespace ErpSystemTypes {
                interface Product { sku: string; description: string; stockLevel: number; }
            }
            declare const sourceData: {
                crmCustomer: CrmSystemTypes.Customer;
                erpProducts: ErpSystemTypes.Product[];
            };
        `;
    } else if (jobId === 'anotherJob789') {
         return `
            declare namespace LegacySystem {
                interface DataRecord { key: string; value: any; timestamp: number; }
            }
             declare const sourceData: {
                legacyRecords: LegacySystem.DataRecord[];
            };
         `;
    }
    return `// No types found for ${jobId}`;
  }

  // MOCK: Replace with actual service call
  async fetchMockSyncJobContextData(jobId: string): Promise<SyncJobContextData | null> {
      await new Promise(resolve => setTimeout(resolve, 400));
      if (jobId === 'activeJob123') {
          return {
              syncJobId: jobId,
              syncJobName: 'Customer & Product Sync (Active)',
              syncJobDescription: 'Synchronizes active customer data from CRM and product stock from ERP.',
              sourceSystems: [
                  {
                      id: 'crm',
                      name: 'Main CRM Platform',
                      type: 'REST_OPENAPI',
                      dataEntities: [
                          {
                              aliasInScript: 'crmCustomer',
                              entityName: 'Active Customer Profile',
                              schemaSummary: { type: 'object', title: 'Customer', properties: { id: {type: 'string'}, name: {type: 'string'}, email: {type: 'string'}, lastContactDate: {type: 'string', format: 'date-time'}, isActive: { type: 'boolean'} }, required: ['id', 'name', 'isActive'] }
                          }
                      ]
                  },
                  {
                      id: 'erp',
                      name: 'Central ERP System',
                      type: 'AAS',
                      dataEntities: [
                          {
                              aliasInScript: 'erpProducts',
                              entityName: 'Stocked Products List',
                              schemaSummary: { type: 'array', title: 'Products', items: { type: 'object', properties: { sku: {type: 'string'}, description: {type: 'string'}, stockLevel: {type: 'number'} }, required: ['sku', 'stockLevel'] } }
                          }
                      ]
                  }
              ],
              destinationSystem: {
                  id: 'dataMart',
                  name: 'Sales Data Mart',
                  targetEntity: 'AggregatedCustomerProductView'
              }
          };
      }
      return null;
  }


  checkScript(): void {
    this.analysisMessages = [];
    this.analysisResults = [];
    this.executionError = null;

    if (!this.code) {
        this.analysisMessages.push({ severity: 'warn', summary: 'No script', detail: 'There is no script content to check.' });
        return;
    }
    if (!this.monacoInstance || !this.monaco) {
        this.analysisMessages.push({ severity: 'error', summary: 'Editor Error', detail: 'Monaco editor is not initialized.' });
        return;
    }
    if (!this.syncJobId) {
        this.analysisMessages.push({ severity: 'warn', summary: 'No Context', detail: 'No SyncJob context loaded for script analysis.' });
        return;
    }


    this.monaco.languages.typescript.getTypeScriptWorker().then(worker => {
        worker(this.monacoInstance!.getModel()!.uri).then(client => {
            Promise.all([
                client.getSyntacticDiagnostics(this.monacoInstance!.getModel()!.uri.toString()),
                client.getSemanticDiagnostics(this.monacoInstance!.getModel()!.uri.toString())
            ]).then(([syntacticDiags, semanticDiags]) => {
                const allDiagnostics = [...syntacticDiags, ...semanticDiags];
                if (allDiagnostics.length > 0) {
                    this.analysisMessages = allDiagnostics.map(d => ({
                        severity: d.category === ts.DiagnosticCategory.Error ? 'error' : (d.category === ts.DiagnosticCategory.Warning ? 'warn' : 'info'),
                        summary: `Line ${d.start ? this.monacoInstance!.getModel()!.getPositionAt(d.start).lineNumber : 'N/A'}: ${ts.DiagnosticCategory[d.category]} (TS${d.code})`,
                        detail: typeof d.messageText === 'string' ? d.messageText : ts.flattenDiagnosticMessageText(d.messageText, '\n')
                    }));
                } else {
                    this.analysisMessages.push({ severity: 'success', summary: 'TypeScript Valid', detail: 'No TypeScript errors or warnings found by Monaco.' });
                }
                this.cdr.detectChanges();
            });
        });
    });
  }

  runScript(): void {
    this.executionResult = null;
    this.executionError = null;

    if (!this.code) {
      this.executionError = 'No script to run.';
      return;
    }

    try {
      const transpileOutput = ts.transpileModule(this.code, {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ESNext,
          esModuleInterop: true,
        }
      });

      if (transpileOutput.diagnostics && transpileOutput.diagnostics.length > 0) {
        console.error('Transpilation errors:', transpileOutput.diagnostics);
        this.executionError = {
          message: 'Transpilation failed. Check console for details.',
          diagnostics: transpileOutput.diagnostics.map(d => ({
            message: typeof d.messageText === 'string' ? d.messageText : ts.flattenDiagnosticMessageText(d.messageText, '\n'),
            category: ts.DiagnosticCategory[d.category],
            code: d.code
          }))
        };
        return;
      }

      const javascriptCode = transpileOutput.outputText;
      console.log('Transpiled JavaScript:', javascriptCode);

      // API call here for script saving

    } catch (e) {
      this.executionError = { message: 'Error during transpilation or sending.', details: e };
      console.error('Error in runScript:', e);
    }
  }

  ngOnDestroy(): void {
    this.currentExtraLibs.forEach(lib => lib.disposable.dispose());
    this.currentExtraLibs = [];
  }
}
