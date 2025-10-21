import {
  ChangeDetectorRef,
  Component,
  inject,
  OnDestroy,
  OnInit,
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
import { ConfirmationService, MessageService } from 'primeng/api';

import { ActivatedRoute, Router } from '@angular/router';

import {
  debounceTime,
  filter,
  finalize,
  first,
  of,
  Subject,
  Subscription,
  switchMap,
} from 'rxjs';

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
  AasArc,
  AnyArc,
  ApiRequestConfiguration,
  SubmodelDescription,
} from '../models/arc.models';
import { ArcStateService } from '../../../core/services/arc-state.service';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../source-system/models/source-system.models';
import { ArcManagementPanelComponent } from '../arc-management-panel/arc-management-panel.component';
import { ArcWizardComponent } from '../arc-wizard/arc-wizard.component';
import { InputTextModule } from 'primeng/inputtext';
import { MonacoEditorService } from '../../../core/services/monaco-editor.service';
import { TypeDefinitionsResponse } from '../models/target-system.models';
import { TargetArcPanelComponent } from '../target-arc-panel/target-arc-panel.component';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Inplace } from 'primeng/inplace';
import { ArcWizardAasComponent } from '../arc-wizard-aas/arc-wizard-aas.component';
import { AasService } from '../../source-system/services/aas.service';
import { ToolbarModule } from 'primeng/toolbar';

/**
 * @description
 * The ScriptEditorPageComponent is the primary container and central orchestrator for the entire script editing experience.
 * It is a stateful, high-level component responsible for:
 *
 * 1.  **Hosting the Monaco Editor:** It initializes and manages the lifecycle of the `ngx-monaco-editor-v2` instance.
 * 2.  **Data Lifecycle Management:** It loads the script payload (`ScriptPayload`) for a given transformation from the backend,
 *     populates the editor, and handles the save operation, including transpilation and validation.
 * 3.  **Coordinating Child Components:** It acts as the parent for the `ArcManagementPanelComponent` (for source ARCs) and
 *     the `TargetArcPanelComponent` (for target ARCs), listening for their events to open the appropriate wizards.
 * 4.  **Monaco Type Definition Management:** It serves as the main consumer of type definition updates from the `MonacoEditorService`.
 *     It orchestrates the initial loading of both source and target types and contains the critical "purge and reload" logic
 *     for target type definitions to ensure the editor's intellisense is always up-to-date.
 * 5.  **UI State Management:** It controls global loading and saving indicators, as well as the visibility and context
 *     of the various ARC creation/editing wizards.
 */
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
    TargetArcPanelComponent,
    Inplace,
    ToolbarModule,
  ],
})
export class ScriptEditorPageComponent implements OnInit, OnDestroy {
  @ViewChild(TargetArcPanelComponent) targetArcPanel!: TargetArcPanelComponent;
  @ViewChild(ArcManagementPanelComponent)
  arcManagementPanel!: ArcManagementPanelComponent;

  currentTransformationId: string | null = null;
  originalName: string = '';
  scriptPayload: ScriptPayload | null = null;

  editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    minimap: { enabled: true },
  };

  private monaco!: typeof import('monaco-editor');
  private monacoInstance: editor.IStandaloneCodeEditor | undefined;
  private targetTypeDisposables: IDisposable[] = [];
  private subscriptions = new Subscription();

  isLoading: boolean = false;
  isSaving: boolean = false;
  loadingMessage = 'Initializing...';
  code: string = `// Script editor will initialize once a SyncJob context is loaded.`;

  isWizardVisible = false;
  wizardContext:
    | {
        system: SourceSystem;
        endpoint: SourceSystemEndpoint;
        arcToEdit?: ApiRequestConfiguration;
        arcToClone?: ApiRequestConfiguration;
      }
    | null
    | undefined = null;

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

  /**
   * @description
   * Initializes the component by subscribing to the editor's model change events with a debounce
   * to perform on-demand type analysis for source ARCs efficiently.
   * @param {Router} router - The Angular Router service for navigation.
   */
  constructor(private router: Router) {
    const timeForMonacoEditorStartup = 1000;
    this.subscriptions.add(
      this.onModelChange
        .pipe(debounceTime(timeForMonacoEditorStartup))
        .subscribe(() => this.analyzeEditorContentForTypes())
    );
  }

  /**
   * @description
   * Angular lifecycle hook that executes when the component initializes. It retrieves the `transformationId`
   * from the route parameters, which is essential for loading the script context.
   */
  ngOnInit(): void {
    this.currentTransformationId =
      this.route.snapshot.paramMap.get('transformationId');

    if (!this.currentTransformationId) {
      this.isLoading = false;
      this.code =
        '// ERROR: No Transformation ID found in URL. Cannot load context.';
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'No Transformation ID provided in the URL.',
      });
    }
  }

  /**
   * @description
   * An event handler called by the `ngx-monaco-editor-v2` component when the editor instance is ready.
   * This method is the main entry point for all Monaco-related setup. It:
   * - Subscribes to the `MonacoEditorService` to receive and apply target type definitions.
   * - Configures the TypeScript compiler and diagnostics options.
   * - Initializes the `ArcStateService` with the Monaco instance to manage source ARC types.
   * - Sets up listeners for content changes in the editor.
   * - Orchestrates the initial loading of the script content and its associated type definitions.
   * @param {editor.IStandaloneCodeEditor} editorInstance - The raw Monaco editor instance.
   * @param {typeof import('monaco-editor')} monaco - The global `monaco` namespace object.
   */
  async onEditorInit(
    editorInstance: editor.IStandaloneCodeEditor,
    monaco = (window as any).monaco
  ): Promise<void> {
    this.monacoInstance = editorInstance;
    this.monaco = monaco;

    this.subscriptions.add(
      this.monacoEditorService.typeUpdateRequested$
        .pipe(
          filter((response): response is TypeDefinitionsResponse => !!response)
        )
        .subscribe((response) => {
          this.applyTargetTypeDefinitions(response);
        })
    );

    this.resetMonacoCompilerOptions();

    monaco.languages.typescript.typescriptDefaults.setDiagnosticsOptions({
      noSemanticValidation: false,
      noSyntaxValidation: false,
    });

    this.addGlobalApiDefinitions();
    this.arcStateService.initializeMonaco(monaco);
    this.monacoInstance?.onDidChangeModelContent(() =>
      this.onModelChange.next()
    );

    this.arcStateService.initializeGlobalSourceType().subscribe({
      next: () => {
        if (this.currentTransformationId) {
          this.loadContextForScript(this.currentTransformationId);
        }
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail:
            'FATAL: Could not initialize global source systems for the editor.',
        });
      },
    });

    if (this.currentTransformationId) {
      this.loadContextForScript(this.currentTransformationId);
      this.scriptEditorService
        .getTargetTypeDefinitions(Number(this.currentTransformationId))
        .subscribe((initialTypes) => {
          this.monacoEditorService.requestTypeUpdate(initialTypes);
        });
    }
  }

  /**
   * @description
   * Event handler for the `(createArc)` output from the `ArcManagementPanelComponent`.
   * Sets the context for the REST ARC wizard and makes it visible in 'create' mode.
   * @param context - An object containing the parent `SourceSystem` and `SourceSystemEndpoint`.
   */
  handleCreateArc(context: {
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
  }): void {
    this.wizardContext = context;
    this.isWizardVisible = true;
  }

  /**
   * @description
   * Event handler for the `(editArc)` output from the `ArcManagementPanelComponent`.
   * Sets the context for the REST ARC wizard, including the ARC to edit, and makes it visible.
   * @param context - An object containing the system, endpoint, and the `ApiRequestConfiguration` to be edited.
   */
  handleEditArc(context: {
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arc: ApiRequestConfiguration;
  }): void {
    this.wizardContext = { ...context, arcToEdit: context.arc };
    this.isWizardVisible = true;
  }

  /**
   * @description
   * Event handler for the `(cloneArc)` output from the `ArcManagementPanelComponent`.
   * Sets the context for the REST ARC wizard, including the ARC to clone, and makes it visible.
   * @param context - An object containing the system, endpoint, and the `ApiRequestConfiguration` to be cloned.
   */
  handleCloneArc(context: {
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arc: ApiRequestConfiguration;
  }): void {
    this.wizardContext = {
      system: context.system,
      endpoint: context.endpoint,
      arcToClone: context.arc,
    };

    this.isWizardVisible = true;
  }

  /**
   * @description
   * Event handler for the `(deleteArc)` output from the `ArcManagementPanelComponent`.
   * It checks for ARC usages, shows a confirmation dialog, and calls the service to delete the ARC upon confirmation.
   * @param context - An object containing the `ApiRequestConfiguration` to be deleted.
   */
  handleDeleteArc(context: { arc: ApiRequestConfiguration }): void {
    const arc = context.arc;
    this.scriptEditorService
      .checkArcUsage(arc.id)
      .pipe(
        switchMap((usages) => {
          let message = `Are you sure you want to delete the ARC "${arc.alias}"?`;

          const noUsagesOfSelectedArc = 0;
          if (usages.length > noUsagesOfSelectedArc) {
            const usageList = usages
              .map((u) => `<li>${u.scriptName}</li>`)
              .join('');
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
                  this.messageService.add({
                    severity: 'success',
                    summary: 'Deleted',
                    detail: 'ARC has been successfully deleted.',
                  });
                  this.isLoading = false;
                },
                error: (err) => {
                  this.messageService.add({
                    severity: 'error',
                    summary: 'Delete Failed',
                    detail: err.error?.message || 'Could not delete the ARC.',
                  });
                  this.isLoading = false;
                },
              });
            },
            reject: () => {
              // user cancellation
            },
          });
          return of(null);
        })
      )
      .subscribe();
  }

  /**
   * @description
   * Event handler for the `(onSaveSuccess)` output from the REST ARC wizard.
   * Updates the global `ArcStateService` with the new/updated ARC to refresh source types in the editor.
   * @param {ApiRequestConfiguration} savedArc - The ARC that was successfully saved.
   */
  handleArcSave(savedArc: ApiRequestConfiguration): void {
    this.arcManagementPanel.allSourceSystems$
      .pipe(first())
      .subscribe((allSystems) => {
        const parentSystem = allSystems.find(
          (s) => s.name === savedArc.sourceSystemName
        );

        if (parentSystem) {
          this.arcStateService
            .ensureSystemIsLoaded(parentSystem.name)
            .subscribe();
        }

        this.arcStateService.addOrUpdateArc(savedArc);

        this.messageService.add({
          severity: 'success',
          summary: 'ARC Saved',
          detail: `Configuration '${savedArc.alias}' is now available.`,
        });
      });
  }

  /**
   * @description
   * Event handler for the `(createAasArc)` output from the `ArcManagementPanelComponent`.
   * Sets the context for the AAS ARC wizard and makes it visible in 'create' mode.
   * @param context - An object containing the parent `SourceSystem` and `SubmodelDescription`.
   */
  handleCreateAasArc(context: {
    system: SourceSystem;
    submodel: SubmodelDescription;
  }): void {
    console.log(context);
    this.aasWizardContext = {
      system: context.system,
      submodel: context.submodel,
      arcToEdit: undefined, // no contextData present in new creation
    };
    this.isAasWizardVisible = true;
  }

  /**
   * @description
   * Event handler for the `(editAasArc)` output from the `ArcManagementPanelComponent`.
   * Sets the context for the AAS ARC wizard, including the ARC to edit, and makes it visible.
   * @param context - An object containing the system, submodel, and the `AasArc` to be edited.
   */
  handleEditAasArc(context: {
    system: SourceSystem;
    submodel: SubmodelDescription;
    arc: AasArc;
  }): void {
    this.aasWizardContext = {
      system: context.system,
      submodel: context.submodel,
      arcToEdit: context.arc,
    };
    this.isAasWizardVisible = true;
  }

  /**
   * @description
   * Event handler for the `(deleteAasArc)` output from the `ArcManagementPanelComponent`.
   * Shows a confirmation dialog and calls the service to delete the AAS ARC upon confirmation.
   * @param context - An object containing the `AasArc` to be deleted.
   */
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
            this.messageService.add({
              severity: 'success',
              summary: 'Deleted',
              detail: 'AAS ARC has been deleted.',
            });
            this.isLoading = false;
          },
          error: (err) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Delete Failed',
              detail: err.error?.message || 'Could not delete the AAS ARC.',
            });
            this.isLoading = false;
          },
        });
      },
    });
  }

  /**
   * @description
   * Event handler for the `(onSaveSuccess)` output from the AAS ARC wizard.
   * Updates the global `ArcStateService` with the new/updated ARC.
   * @param {AasArc} savedArc - The AAS ARC that was successfully saved.
   */
  handleAasArcSave(savedArc: AasArc): void {
    this.arcStateService.addOrUpdateArc(savedArc as AnyArc);
    this.messageService.add({
      severity: 'success',
      summary: 'AAS ARC Saved',
      detail: `Configuration '${savedArc.alias}' is now available.`,
    });
  }

  /**
   * @private
   * @description
   * Performs a robust "Purge and Reload" of **target** type definitions in the Monaco editor. This method is the
   * sole consumer of type updates for Target ARCs and is critical for keeping IntelliSense up-to-date. It solves
   * the Monaco worker's caching issue by:
   * 1.  **Purging:** Disposing of all previously added target type library disposables.
   * 2.  **Reloading:** Adding the new set of libraries received from the backend.
   * A `setTimeout` is used to ensure the purge operation completes in the event loop before reloading.
   * @param {TypeDefinitionsResponse} response - The new set of type definitions from the backend.
   */
  private applyTargetTypeDefinitions(response: TypeDefinitionsResponse): void {
    if (!this.monaco) {
      return;
    }

    // --- PHASE 1: PURGE ---
    this.targetTypeDisposables.forEach((disposable) => disposable.dispose());
    this.targetTypeDisposables = [];

    // --- PHASE 2: RELOAD ---
    // The `setTimeout` ensures that the disposal has been processed by the Monaco event loop
    // before we attempt to add the new libraries. This prevents race conditions.
    const setReloadDelay = 100;
    setTimeout(() => {
      if (response.libraries && response.libraries.length !== 0) {
        const newDisposables: IDisposable[] = response.libraries.map((lib) => {
          return this.monaco.languages.typescript.typescriptDefaults.addExtraLib(
            lib.content,
            lib.filePath
          );
        });

        this.targetTypeDisposables = newDisposables;
      }

      // Force monaco worker AFTER adding the new libs to re-evaluate.
      // This is a crucial step to ensure the editor recognizes the changes immediately.
      this.resetMonacoCompilerOptions();
    }, setReloadDelay);
  }

  /**
   * @private
   * @description
   * Encapsulates the configuration of the Monaco TypeScript worker's compiler options.
   * Critically, calling this method also forces the worker to re-evaluate all loaded
   * type libraries, which is essential after adding or removing type definitions.
   */
  private resetMonacoCompilerOptions(): void {
    if (!this.monaco) return;

    this.monaco.languages.typescript.typescriptDefaults.setCompilerOptions({
      target: this.monaco.languages.typescript.ScriptTarget.ESNext,
      allowNonTsExtensions: true,
      moduleResolution:
        this.monaco.languages.typescript.ModuleResolutionKind.NodeJs,
      module: this.monaco.languages.typescript.ModuleKind.CommonJS,
      noEmit: true,
      esModuleInterop: true,
      lib: ['es2020', 'dom'],
    });
  }

  /**
   * @private
   * @description
   * Loads the primary script context (`ScriptPayload`) for a given transformation ID. It sets loading indicators,
   * fetches the data from the `ScriptEditorService`, and populates the editor with the script code.
   * @param {string} transformationId - The ID of the transformation context to load.
   */
  private loadContextForScript(transformationId: string): void {
    this.isLoading = true;
    this.loadingMessage = `Loading context for Transformation with ID: ${transformationId}...`;

    this.scriptEditorService
      .getScriptForTransformation(Number(transformationId))
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (payload) => {
          this.scriptPayload = payload;
          this.code =
            payload.typescriptCode || this.generateDefaultScriptTemplate();
          this.analyzeEditorContentForTypes();
        },
        error: (err) => {
          console.error('Failed to load script context', err);
          this.code = `// ERROR: Could not load script context for Transformation ID ${transformationId}.`;
          this.messageService.add({
            severity: 'error',
            summary: 'Load Error',
            detail: 'Script context could not be loaded.',
          });
        },
      });
  }

  /**
   * @description
   * Handles the main save action for the entire page. It performs several steps:
   * - Gathers the active target ARC IDs from the child panel.
   * - Validates the TypeScript code for errors using the Monaco worker.
   * - Transpiles the TypeScript to JavaScript.
   * - Performs a simple static analysis to determine which source ARCs are required.
   * - Constructs the final `ScriptPayload` and sends it to the backend.
   * @returns {Promise<void>}
   */
  async saveScript(): Promise<void> {
    if (!this.currentTransformationId) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'No transformation context found. Cannot save.',
      });
      return;
    }
    if (this.isSaving) {
      return;
    }

    const restTargetArcIds = this.targetArcPanel.activeArcs
      .filter((arc) => arc.arcType === 'REST')
      .map((arc) => arc.id);

    const aasTargetArcIds = this.targetArcPanel.activeArcs
      .filter((arc) => arc.arcType === 'AAS')
      .map((arc) => arc.id);

    let scriptStatus: 'DRAFT' | 'VALIDATED' = 'DRAFT';
    let transpiledCode = '';
    let hasErrors = true;

    try {
      hasErrors = await this.hasValidationErrors();
      if (hasErrors) {
        this.messageService.add({
          severity: 'error',
          summary: 'Validation Failed',
          detail: 'Typescript errors present. Saved as draft.',
        });
      }
      const transpileOutput = ts.transpileModule(this.code, {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ESNext,
        },
      });
      transpiledCode = transpileOutput.outputText;

      if (!hasErrors) {
        scriptStatus = 'VALIDATED';
      }
    } catch (e) {
      scriptStatus = 'DRAFT';
    }

    const codeToSave = this.code;
    const arcPattern = /\bsource\.([a-zA-Z0-9_]+)\.([a-zA-Z0-9_]+)/g;
    const matches = codeToSave.matchAll(arcPattern);

    const requiredArcSet = new Set<string>();
    for (const match of matches) {
      const systemName = match[1];
      const arcName = match[2];
      requiredArcSet.add(`${systemName}.${arcName}`);
    }

    const requiredArcAliases = Array.from(requiredArcSet);

    const payload: ScriptPayload = {
      name: this.scriptPayload!.name,
      typescriptCode: this.code,
      javascriptCode: transpiledCode,
      requiredArcAliases: requiredArcAliases,
      status: scriptStatus,
      restTargetArcIds: restTargetArcIds,
      aasTargetArcIds: aasTargetArcIds,
    };

    this.isSaving = true;
    this.scriptEditorService
      .saveScriptForTransformation(
        Number(this.currentTransformationId),
        payload
      )
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Saved!',
            detail: 'Script has been saved successfully.',
          });

          this.isSaving = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Save Failed',
            detail: 'Could not save the script to the server.',
          });
          this.isSaving = false;
          this.cdr.detectChanges();
        },
      });
  }

  // --- Helper & Private Methods ---

  /**
   * @private
   * @description
   * Performs a simple static analysis of the current editor content to detect which `source` systems
   * are being referenced (e.g., `source.myCrm`). It then triggers the `ArcStateService` to dynamically
   * load the type definitions for only those systems, improving performance.
   */
  private analyzeEditorContentForTypes(): void {
    const code = this.monacoInstance?.getValue() || '';
    const matches = code.matchAll(/\bsource\.([a-zA-Z0-9_-]+)\b/g);

    const systemNamesInEditor = new Set<string>();
    for (const match of matches) {
      // TODO: control for js sanitization of identifier names. Requires proper naming convention for Types
      const positionOfSourceSystemName = 1;
      systemNamesInEditor.add(match[positionOfSourceSystemName]);
    }
    const noSystemNamesPatternsPresent = 0;
    if (systemNamesInEditor.size > noSystemNamesPatternsPresent) {
      this.arcStateService
        .loadTypesForSourceSystemNames(Array.from(systemNamesInEditor))
        .subscribe();
    }
  }

  /**
   * @private
   * @description
   * A generic wrapper for adding a TypeScript type definition file (`.d.ts`) to the Monaco editor instance.
   * @param {string} content - The string content of the `.d.ts` file.
   * @param {string} uri - A unique file path URI for the library (e.g., 'file:///my-lib.d.ts').
   * @returns {IDisposable | null} A disposable object that can be used to remove the library later.
   */
  private addExtraLib(content: string, uri: string): IDisposable | null {
    if (!this.monaco) {
      return null;
    }

    return this.monaco.languages.typescript.typescriptDefaults.addExtraLib(
      content,
      uri
    );
  }

  /**
   * @private
   * @description
   * Adds the global API definitions for the `stayinsync` object, making functions like `stayinsync.log()`
   * available with full type-safety and autocompletion in the editor.
   */
  private addGlobalApiDefinitions(): void {
    const globalFileUriName = 'file:///global-stayinsync-api.d.ts';
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

    this.addExtraLib(stayinsyncApiDts, globalFileUriName);
  }

  /**
   * @private
   * @description
   * Generates a default boilerplate template string for a new, empty transformation script.
   * @returns {string} The default script template.
   */
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

  /**
   * @private
   * @description
   * Programmatically interacts with the Monaco TypeScript worker to get a list of syntactic and semantic
   * diagnostics (errors, warnings) for the current code in the editor.
   * @returns {Promise<boolean>} A promise that resolves to `true` if there are any validation errors.
   */
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

    const errors = diagnostics.filter(
      (d) => d.category === ts.DiagnosticCategory.Error
    );
    return errors.length > 0;
  }

  /**
   * @description
   * Angular lifecycle hook called when the component is destroyed. It performs necessary cleanup by
   * unsubscribing from all RxJS subscriptions and disposing of any loaded Monaco type definition libraries
   * to prevent memory leaks.
   */
  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.targetTypeDisposables.forEach((disposable) => disposable.dispose());
  }

  /**
   * @description
   * Navigates the user back to the main transformation scripts list page.
   */
  goBack() {
    this.router.navigate(['transformation-scripts']);
  }

  /**
   * @description
   * Handles the save event from the inplace script name editor.
   * @param {Function} closeCallback - The function provided by the PrimeNG Inplace component to close the editor.
   */
  onSave(closeCallback: Function) {
    this.saveScript();
    closeCallback();
  }

  /**
   * @description
   * Handles the cancel/close event from the inplace script name editor, reverting any changes.
   * @param {Function} closeCallback - The function provided by the PrimeNG Inplace component to close the editor.
   */
  onClose(closeCallback: Function) {
    this.scriptPayload!.name = this.originalName;
    closeCallback();
  }

  /**
   * @description
   * Handles the activate event from the inplace script name editor, storing the original name for potential revert.
   */
  onActivate() {
    this.originalName = this.scriptPayload!.name;
  }
}
