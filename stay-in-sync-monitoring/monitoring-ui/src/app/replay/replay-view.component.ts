// src/app/replay/replay-view.component.ts
import { CommonModule, NgIf } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NgxJsonViewerModule } from 'ngx-json-viewer';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { PrimeTemplate } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import * as ts from 'typescript';
import { LogEntry } from '../core/models/log.model';
import { SnapshotDTO } from '../core/models/snapshot.model';
import { TransformationScriptDTO } from '../core/models/transformation-script.model';
import { LogService } from '../core/services/log.service';
import { ReplayService } from '../core/services/replay/replay.service';
import { ScriptService } from '../core/services/replay/script.service';
import { SnapshotService } from '../core/services/snapshot.service';

// IMPORTANT: Import monaco types for the onInit handler
declare const monaco: any;

@Component({
  selector: 'app-replay-view',
  standalone: true,
  imports: [
    CommonModule,
    NgIf,
    PrimeTemplate,
    TableModule,
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
    MonacoEditorModule,
    FormsModule,
    NgxJsonViewerModule,
    Button,
  ],
  templateUrl: './replay-view.component.html',
  styleUrl: './replay-view.component.css',
})
/**
 * ReplayViewComponent
 * --------------------
 * UI for loading a snapshot, displaying its associated TypeScript transformation,
 * and executing a replay (TypeScript → transpile → JS run in backend sandbox).
 *
 * Responsibilities:
 * - Resolve the `snapshotId` from the route and fetch the snapshot.
 * - Load the transformation script and generated SDK for the snapshot's transformation.
 * - Configure Monaco's TypeScript environment so globals (source/targets/etc.) are known.
 * - Transpile TS to JS locally and call the replay backend endpoint.
 * - Render output, captured variables, and error info for debugging.
 *
 * Notes:
 * - The component only orchestrates UI and API calls; actual sandboxed execution
 *   happens server-side in the ReplayExecutor.
 * - No internal logic is modified by this documentation.
 *
 * @author Mohammed-Ammar Hassnou
 */
export class ReplayViewComponent implements OnInit {
  // Router access to read the
  // snapshotId query param.
  private route = inject(ActivatedRoute);
  // API client for retrieving snapshots.
  private snapshots = inject(SnapshotService);
  // API client for loading transformation scripts and generated SDK.
  private scripts = inject(ScriptService);
  // API client for fetching recent logs for the transformation.
  private logService = inject(LogService);
  // API client for invoking the backend replay execution.
  private replayService = inject(ReplayService);

  // --- UI state signals ---
  // `loading` controls spinners, `error` holds user-facing error text,
  // and `data` caches the loaded SnapshotDTO.
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  data = signal<SnapshotDTO | null>(null);

  // --- Replay results and context ---
  // These fields are populated after calling the backend replay endpoint.
  outputData: any;
  variables: Record<string, any> = {};
  errorInfo: string | null = null;
  snapshotId: string | null = null;
  generatedSdkCode: any;

  // The TypeScript transformation code shown in the Monaco editor.
  // Populated after fetching the script for the snapshot's transformation.
  scriptDisplay = '// loading TypeScript…';
  logs: LogEntry[] = [];

  /** Monaco editor configuration for displaying/transpiling the TypeScript transform. */
  editorOptions = {
    readOnly: false,
    language: 'typescript',
    automaticLayout: true,
    minimap: { enabled: false },
    theme: 'vs-dark',
    lineNumbers: 'on' as const,
  };

  // Prevents repeated Monaco global reconfiguration when the component/editor re-initializes.
  private globalsConfigured = false;

  /**
   * Monaco editor initialization hook.
   *
   * Ensures that the TypeScript worker knows about global variables used in
   * transformation scripts (e.g., `source`, `targets`, `stayinsync`, `__capture`).
   * Triggers a re-validation so diagnostics reflect these globals.
   */
  onEditorInit(editor: any): void {
    console.log('Monaco editor initializing...');

    // Configure globals immediately when editor is created
    if (!this.globalsConfigured && typeof monaco !== 'undefined') {
      this.configureMonacoGlobals();
      this.globalsConfigured = true;
    }

    // Get the model and force TypeScript to re-validate with new globals
    setTimeout(() => {
      const model = editor.getModel();
      if (model && typeof monaco !== 'undefined') {
        // Trigger TypeScript worker to re-check the model
        const uri = model.uri;
        monaco.editor.setModelLanguage(model, 'typescript');

        // Force diagnostics refresh
        monaco.languages.typescript
          .getTypeScriptWorker()
          .then((worker: any) => worker(uri))
          .then((client: any) => {
            console.log('TypeScript worker refreshed');
          })
          .catch((err: any) =>
            console.error('Error refreshing TS worker:', err)
          );
      }
    }, 100);
  }

  /**
   * Configure Monaco TypeScript defaults for the replay environment.
   *
   * Adds ambient declarations for globals, sets compiler/diagnostic options,
   * and ensures consistent type-checking for scripts pasted or loaded into the editor.
   */
  private configureMonacoGlobals(): void {
    if (typeof monaco === 'undefined') {
      console.error('Monaco is not defined');
      return;
    }

    console.log('Configuring Monaco TypeScript globals...');

    // Clear existing extra libs
    monaco.languages.typescript.typescriptDefaults.setExtraLibs([]);

    // Add global variable declarations
    const libSource = `
declare var source: any;
declare var targets: any;
declare var stayinsync: any;
declare var __capture: (name: string, value: any) => void;
`;

    const libUri = 'ts:filename/globals.d.ts';
    monaco.languages.typescript.typescriptDefaults.addExtraLib(
      libSource,
      libUri
    );

    // Configure TypeScript compiler options
    monaco.languages.typescript.typescriptDefaults.setCompilerOptions({
      target: monaco.languages.typescript.ScriptTarget.ES2020,
      allowNonTsExtensions: true,
      moduleResolution: monaco.languages.typescript.ModuleResolutionKind.NodeJs,
      module: monaco.languages.typescript.ModuleKind.CommonJS,
      noEmit: true,
      esModuleInterop: true,
      allowJs: true,
      strict: false,
      noImplicitAny: false,
      strictNullChecks: false,
    });

    // Set diagnostics options
    monaco.languages.typescript.typescriptDefaults.setDiagnosticsOptions({
      noSemanticValidation: false,
      noSyntaxValidation: false,
      noSuggestionDiagnostics: true,
    });

    console.log('Monaco globals configured successfully');
  }

  /**
   * Lifecycle: initialize component state.
   *
   * - Optionally pre-configures Monaco if it was loaded before the component.
   * - Reads the `snapshotId` from the route and loads the snapshot.
   * - Fetches the associated transformation script + generated SDK.
   * - Loads recent logs for the snapshot's transformation to aid debugging.
   */
  ngOnInit(): void {
    // Pre-configure Monaco if it's already available
    // This handles cases where Monaco loads before component initialization
    if (typeof monaco !== 'undefined' && !this.globalsConfigured) {
      this.configureMonacoGlobals();
      this.globalsConfigured = true;
    }

    // Get snapshotId from URL
    this.snapshotId = this.route.snapshot.queryParamMap.get('snapshotId');
    if (!this.snapshotId) {
      this.error.set('Missing snapshotId in URL.');
      this.loading.set(false);
      return;
    }

    // Load snapshot
    this.snapshots.getById(this.snapshotId).subscribe({
      next: (snap) => {
        if (!snap) {
          this.error.set('Snapshot not found or is null.');
          this.loading.set(false);
          return;
        }
        this.data.set(snap);

        const transformationId = snap.transformationResult?.transformationId;
        if (transformationId == null) {
          this.scriptDisplay =
            '// Snapshot has no transformationId. Cannot load script.';
          this.loading.set(false);
          return;
        }

        // Fetch TypeScript code by transformationId
        this.scripts.getByTransformationId(transformationId).subscribe({
          next: (script: TransformationScriptDTO) => {
            this.generatedSdkCode = script.generatedSdkCode;
            // Set the editor content
            this.scriptDisplay =
              script.typescriptCode || '// No TypeScript code available';

            // Force re-validation after content is loaded
            setTimeout(() => {
              if (typeof monaco !== 'undefined') {
                const models = monaco.editor.getModels();
                if (models.length > 0) {
                  const model = models[0];
                  monaco.editor.setModelLanguage(model, 'typescript');
                }
              }
            }, 100);

            this.loading.set(false);
          },
          error: (err) => {
            console.error('Failed to load script', err);
            this.scriptDisplay = '// Failed to load TypeScript code';
            this.loading.set(false);
          },
        });

        // Load logs
        this.logService
          .getLogsByTransformations(
            [transformationId.toString()],
            this.toNanoSeconds(new Date(Date.now() - 24 * 60 * 60 * 1000)),
            this.toNanoSeconds(new Date()),
            ''
          )
          .subscribe({
            next: (logs) => {
              this.logs = logs;
              console.log('Logs for transformation', transformationId, logs);
            },
            error: (err) => {
              console.error(
                'Failed to load logs for transformation',
                transformationId,
                err
              );
            },
          });
      },
      error: (e) => {
        this.error.set(`Failed to load snapshot: ${e?.message ?? e}`);
        this.loading.set(false);
      },
    });
  }

  /**
   * Handler for the "Replay" action.
   *
   * Transpiles the editor's TypeScript into JavaScript (client-side) and constructs
   * a `ReplayExecuteRequestDTO` payload including the snapshot's source data and
   * the generated SDK code. Sends the request to the backend and stores the
   * returned output/variables/error for display.
   */
  onReplayClick(): void {
    if (!this.data()) {
      this.errorInfo = 'No snapshot data available';
      return;
    }

    let transpiledCode: string;

    try {
      // Transpile TypeScript → JavaScript
      const transpileOutput = ts.transpileModule(this.scriptDisplay, {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ESNext,
        },
      });

      transpiledCode = transpileOutput.outputText;

      // Optionally check diagnostics
      const hasErrors =
        transpileOutput.diagnostics && transpileOutput.diagnostics.length > 0;
      if (hasErrors) {
        console.warn(
          'TypeScript transpile had errors',
          transpileOutput.diagnostics
        );
      }
    } catch (e) {
      console.error('Error transpiling TypeScript:', e);
      this.errorInfo = 'TypeScript transpilation failed';
      return;
    }

    // Build ReplayExecuteRequestDTO
    const dto = {
      scriptName: 'replay.js',
      javascriptCode: transpiledCode,
      sourceData: this.data()?.transformationResult?.sourceData || {},
      generatedSdkCode: this.generatedSdkCode,
    };

    // Call backend endpoint
    this.replayService.executeReplay(dto).subscribe({
      next: (res) => {
        this.outputData = res.outputData;
        this.variables = res.variables;
        this.errorInfo = res.errorInfo;
        console.log('Replay executed successfully', res);
      },
      error: (err) => {
        console.error('Replay request failed', err);
        this.errorInfo = 'Replay request failed';
      },
    });

    console.log(this.scriptDisplay);
    //Debugging
    console.log(dto);
  }

  /**
   * Convert a JavaScript Date to nanoseconds since epoch.
   *
   * @param date The Date instance to convert.
   * @returns Epoch time in nanoseconds.
   */
  private toNanoSeconds(date: Date) {
    return date.getTime() * 1_000_000;
  }
}
