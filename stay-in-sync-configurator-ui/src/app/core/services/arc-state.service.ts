import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, forkJoin, Observable, of, throwError } from 'rxjs';
import { catchError, map, take, tap } from 'rxjs/operators';
import {
  AnyArc,
  ArcMap,
  SubmodelDescription,
} from '../../features/script-editor/models/arc.models';
import { ScriptEditorService } from './script-editor.service';
import { HttpClient } from '@angular/common/http';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../features/source-system/models/source-system.models';
import { AasService } from '../../features/source-system/services/aas.service';

/**
 * @description Represents the state of a single Source System within the ArcStateService.
 * It extends the base SourceSystem model with dynamic data loaded on-demand, such as
 * its ARCs, endpoints, and loading status.
 */
export type SystemState = SourceSystem & {
  arcs?: AnyArc[];
  endpoints?: SourceSystemEndpoint[];
  submodels?: SubmodelDescription[];
  isLoadingDetails: boolean; // For endpoints/submodels
};

/**
 * @description Defines the overall shape of the state managed by this service.
 */
interface ArcState {
  /** A map of all known source systems, keyed by their unique name. */
  systems: Map<string, SystemState>;
  /** Tracks the initial loading of the master list of all systems. */
  isLoadingSystems: boolean;
}

/**
 * @description
 * The ArcStateService is a central, in-memory state store for managing all Source Systems
 * and their associated API Request Configurations (ARCs). It serves three primary purposes:
 *
 * 1. **State Management:** It holds the single source of truth for all systems and ARCs in a
 *    `BehaviorSubject`, providing reactive data streams (`Observable`) to the application.
 * 2. **Data Fetching:** It orchestrates the on-demand loading of system details (endpoints, submodels)
 *    and ARCs, preventing redundant API calls.
 * 3. **Monaco Editor Integration:** It dynamically generates and injects TypeScript type definitions
 *    (`.d.ts`) into the Monaco editor instance, providing rich autocompletion and type safety for
 *    the `source` object in user scripts.
 */
@Injectable({
  providedIn: 'root',
})
export class ArcStateService {
  private monaco: typeof import('monaco-editor') | undefined;
  private http = inject(HttpClient);
  private scriptEditorService = inject(ScriptEditorService);
  private aasService = inject(AasService);

  private readonly SOURCE_TYPES_URI = 'file:///stayinsync-source-types.d.ts';
  private readonly GLOBAL_API_URI = 'file:///global-stayinsync-api.d.ts';

  /**
   * @description The core state of the service, wrapped in a BehaviorSubject to provide
   * replay of the last known value to new subscribers.
   */
  private readonly state = new BehaviorSubject<ArcState>({
    systems: new Map(),
    isLoadingSystems: false,
  });

  /**
   * @description Public observable stream that emits an array of all known SystemState objects.
   * UI components can subscribe to this to display the list of all source systems.
   */
  public readonly systems$: Observable<SystemState[]> = this.state
    .asObservable()
    .pipe(map((s) => Array.from(s.systems.values())));

  /**
   * @description Public observable stream that transforms the system state into a Map where
   * keys are system names and values are arrays of their ARCs.
   */
  public readonly arcsBySystem$: Observable<Map<string, AnyArc[]>> =
    this.systems$.pipe(
      map((systems) => {
        const arcMap = new Map<string, AnyArc[]>();
        systems.forEach((system) => {
          if (system.arcs && system.arcs.length > 0) {
            arcMap.set(system.name, system.arcs);
          }
        });
        return arcMap;
      })
    );

  /**
   * @description Public observable stream that emits an array of only the systems that currently
   * have one or more ARCs loaded. Useful for the "Active in Script" tab.
   */
  public readonly activeSystems$: Observable<SystemState[]> =
    this.systems$.pipe(
      map((systems) => systems.filter((s) => s.arcs && s.arcs.length > 0))
    );

  /**
   * @description Initializes the service with a Monaco editor instance.
   * This method MUST be called once from the parent component to enable type generation.
   * @param monacoInstance The imported `monaco-editor` instance.
   */
  public initializeMonaco(
    monacoInstance: typeof import('monaco-editor')
  ): void {
    this.monaco = monacoInstance;
    this.addGlobalApiDefinitions();
  }

  /**
   * @description Legacy method for loading types based on system names detected in the editor.
   */
  public loadTypesForSourceSystemNames(
    systemNames: string[]
  ): Observable<void> {
    const currentState = this.state.getValue();
    const namesToFetch = systemNames.filter((name) => {
      const system = currentState.systems.get(name);
      return system && !system.arcs; // Fetch if the system exists but its ARCs are not loaded
    });

    if (namesToFetch.length === 0) {
      return of(undefined);
    }

    // Mark systems as loading
    namesToFetch.forEach((name) =>
      this.updateSystemState(name, { isLoadingDetails: true })
    );

    return this.http
      .post<ArcMap>(
        '/api/config/source-system/request-configuration/by-source-system-names',
        namesToFetch
      )
      .pipe(
        tap((loadedArcMap) => {
          for (const [systemName, arcs] of Object.entries(loadedArcMap)) {
            const enrichedArcs = arcs.map((arc) => ({
              ...arc,
              sourceSystemName: systemName,
            }));
            this.updateSystemState(systemName, {
              arcs: enrichedArcs,
              isLoadingDetails: false,
            });
          }
          // For any systems that were in namesToFetch but not in the response
          namesToFetch.forEach((name) => {
            if (!loadedArcMap[name]) {
              this.updateSystemState(name, { isLoadingDetails: false });
            }
          });
          this.regenerateAllTypeDefinitions();
        }),
        map(() => undefined),
        catchError((err) => {
          namesToFetch.forEach((name) =>
            this.updateSystemState(name, { isLoadingDetails: false })
          );
          return of(undefined);
        })
      );
  }

  /**
   * @description Fetches the master list of all source systems from the backend to
   * populate the initial state. This should be called once when the editor initializes.
   * It also triggers the first generation of type definitions.
   * @returns An `Observable<void>` that completes when the operation is finished.
   */
  public initializeGlobalSourceType(): Observable<void> {
    if (!this.monaco) {
      return throwError(() => new Error('ArcStateService not initialized.'));
    }

    this.updateState({ isLoadingSystems: true });

    return this.scriptEditorService.getSourceSystems().pipe(
      tap((allSystems) => {
        const systemsMap = new Map<string, SystemState>();
        allSystems.forEach((system) => {
          systemsMap.set(system.name, {
            ...system,
            isLoadingDetails: false,
          });
        });
        this.updateState({ systems: systemsMap, isLoadingSystems: false });
        this.regenerateAllTypeDefinitions();
      }),
      map(() => undefined)
    );
  }

  /**
   * @description Ensures that all details for a given system (ARCs, endpoints, or submodels)
   * are loaded into the state. It is idempotent and will not re-fetch data that is already
   * present or currently being loaded. This is the primary method for UI components to
   * trigger on-demand data loading (e.g., when an accordion is expanded).
   * @param systemName The name of the system to load.
   * @returns An `Observable<void>` that completes when the loading is finished.
   */
  public ensureSystemIsLoaded(systemName: string): Observable<void> {
    const system = this.state.getValue().systems.get(systemName);

    if (!system) {
      return of(undefined);
    }

    // Determine if we need to fetch details based on the system's API type.
    const needsRestDetails = system.apiType !== 'AAS' && !system.endpoints;
    const needsAasDetails = system.apiType === 'AAS' && !system.submodels;

    // If details are already loaded OR it's currently loading, do nothing.
    if ((!needsRestDetails && !needsAasDetails) || system.isLoadingDetails) {
      return of(undefined);
    }

    // Select the correct service call based on API type.
    const loadDetails$ =
      system.apiType === 'AAS'
        ? this.aasService.listSubmodels(system.id)
        : this.scriptEditorService.getEndpointsForSourceSystem(system.id);

    // Optimize to avoid re-fetching ARCs if they already exist in the state.
    const loadArcs$ = system.arcs
      ? of(system.arcs)
      : this.scriptEditorService
          .getArcsForSourceSystem(system.id)
          .pipe(
            map((arcs) =>
              arcs.map((arc) => ({ ...arc, sourceSystemName: systemName }))
            )
          );

    this.updateSystemState(systemName, { isLoadingDetails: true });

    // Execute fetches in parallel and update the state upon completion.
    return forkJoin([loadDetails$, loadArcs$]).pipe(
      tap(([details, arcs]) => {
        const partialUpdate = {
          ...(system.apiType === 'AAS'
            ? { submodels: details as SubmodelDescription[] }
            : { endpoints: details as SystemState['endpoints'] }),
          arcs: arcs as AnyArc[],
          isLoadingDetails: false,
        };

        this.updateSystemState(systemName, partialUpdate);
        this.regenerateAllTypeDefinitions();
      }),
      map(() => undefined),
      catchError((err) => {
        this.updateSystemState(systemName, { isLoadingDetails: false });
        return of(undefined);
      })
    );
  }

  /**
   * @description Adds a new ARC to the state or updates an existing one. This is the primary
   * method for committing changes from the ARC wizard. After updating the state, it triggers
   * a regeneration of the Monaco types. If it's the first ARC for a system, it also ensures
   * the system's details (endpoints) are loaded.
   * @param newArc The ARC object to add or update. It must include `sourceSystemName`.
   */
  public addOrUpdateArc(newArc: AnyArc): void {
    if (!newArc.sourceSystemName) {
      return;
    }

    const currentState = this.state.getValue();
    const system = currentState.systems.get(newArc.sourceSystemName);

    if (!system) {
      return;
    }

    const currentArcs = system.arcs || [];
    const existingIndex = currentArcs.findIndex((a) => a.id === newArc.id);

    let updatedArcs: AnyArc[];
    if (existingIndex > -1) {
      updatedArcs = [...currentArcs];
      updatedArcs[existingIndex] = newArc;
    } else {
      updatedArcs = [...currentArcs, newArc];
    }

    this.updateSystemState(newArc.sourceSystemName, { arcs: updatedArcs });

    // If this was the first ARC added, trigger a load of the system's details
    // to ensure the UI can display the ARC under its parent endpoint.
    if (currentArcs.length === 0 && updatedArcs.length === 1) {
      this.ensureSystemIsLoaded(newArc.sourceSystemName)
        .pipe(take(1))
        .subscribe();
    }

    this.regenerateAllTypeDefinitions();
  }

  /**
   * @description Removes an ARC from the state. Called after a successful deletion from the backend.
   * Triggers a regeneration of Monaco types to remove the deleted ARC's type.
   * @param arcToRemove The ARC object to remove. It must include `sourceSystemName`.
   */
  public removeArc(arcToRemove: AnyArc): void {
    if (!arcToRemove.sourceSystemName) {
      return;
    }

    const system = this.state
      .getValue()
      .systems.get(arcToRemove.sourceSystemName);
    if (!system || !system.arcs) return;

    const filteredArcs = system.arcs.filter((a) => a.id !== arcToRemove.id);
    this.updateSystemState(arcToRemove.sourceSystemName, {
      arcs: filteredArcs,
    });
    this.regenerateAllTypeDefinitions();
  }

  /**
   * @private
   * @description The core type generation engine. It iterates through the current state,
   * constructs a TypeScript declaration file (`.d.ts`) for the global `source` object,
   * and injects it into the Monaco editor instance.
   */
  private regenerateAllTypeDefinitions(): void {
    const monaco = this.monaco;
    if (!monaco) return;

    const currentState = this.state.getValue();
    const allSystems = Array.from(currentState.systems.values());

    if (allSystems.length === 0) return;

    let dts = 'declare const source: {\n';
    let individualTypeInterfaces = '';

    // 1. Iterate through all systems in the state.
    for (const system of allSystems) {
      const validJsIdentifier = this.sanitizeForJs(system.name);

      // 2. If a system has ARCs, generate a typed entry for it.
      if (system.arcs && system.arcs.length > 0) {
        dts += `  ${validJsIdentifier}: {\n`;
        for (const arc of system.arcs) {
          const arcNameJs = this.sanitizeForJs(arc.alias);
          // Create a unique type name, e.g., "Dummy_JSONGetProductsResponseType".
          const typeName = `${this.capitalize(
            validJsIdentifier
          )}${this.capitalize(arcNameJs)}ResponseType`;

          // 3. Read the `responseIsArray` flag from the ARC object.
          const isArrayType = arc.responseIsArray;

          // 4. Conditionally append array brackets `[]` to the type name.
          const finalTypeName = isArrayType ? `${typeName}[]` : typeName;

          // 5. Add the final declaration to the `source` object.
          dts += `    ${arcNameJs}: ${finalTypeName};\n`;

          // 6. Take the `responseDts` from the ARC (which always starts with `interface Root`)
          //    and rename `Root` to our unique `typeName`.
          const dtsContent = arc.responseDts || `interface ${typeName} {}`;
          individualTypeInterfaces +=
            dtsContent.replace(
              /^(export\s+)?interface\s+Root/,
              `$1interface ${typeName}`
            ) + '\n\n';
        }
        dts += '  };\n';
      } else {
        // If no ARCs are loaded, type the system as 'any'.
        dts += `  ${validJsIdentifier}: any;\n`;
      }
    }

    const finalDts = dts + '};\n\n' + individualTypeInterfaces;
    // 7. Push the complete .d.ts string into Monaco as an "extra library".
    monaco.languages.typescript.typescriptDefaults.addExtraLib(
      finalDts,
      this.SOURCE_TYPES_URI
    );
  }

  /**
   * @private
   * @description Injects global API definitions (e.g., for `stayinsync.log`) into Monaco.
   */
  private addGlobalApiDefinitions(): void {
    const stayinsyncApiDts = `
      declare const stayinsync: {
          log: (message: any, logLevel?: 'INFO' | 'WARN' | 'ERROR') => void;
          setOutput: (outputData: any) => void;
      };
    `;
    if (this.monaco) {
      this.monaco.languages.typescript.typescriptDefaults.addExtraLib(
        stayinsyncApiDts,
        this.GLOBAL_API_URI
      );
    }
  }

  /**
   * @private
   * @description Helper to update a single system's state in an immutable way.
   * @param systemName The name of the system to update.
   * @param partialSystemState An object with the properties of the system to update.
   */
  private updateSystemState(
    systemName: string,
    partialSystemState: Partial<SystemState>
  ): void {
    const currentState = this.state.getValue();
    const newSystemsMap = new Map(currentState.systems);
    const currentSystem = newSystemsMap.get(systemName);

    if (currentSystem) {
      newSystemsMap.set(systemName, {
        ...currentSystem,
        ...partialSystemState,
      });
      this.updateState({ systems: newSystemsMap });
    }
  }

  /**
   * @private
   * @description Helper to update the top-level state in an immutable way.
   * @param partialState An object with the properties of the state to update.
   */
  private updateState(partialState: Partial<ArcState>): void {
    this.state.next({ ...this.state.getValue(), ...partialState });
  }

  /**
   * @private
   * @description Utility function to capitalize a string.
   */
  private capitalize(s: string): string {
    if (!s) return '';
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  /**
   * @private
   * @description Utility function to convert a string into a safe identifier for JavaScript/TypeScript.
   */
  private sanitizeForJs(name: string): string {
    if (!name) return 'invalidName';
    return name.replace(/[^a-zA-Z0-9_]/g, '_');
  }
}
