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
import { SourceSystem, SourceSystemEndpoint } from '../../features/source-system/models/source-system.models';
import { AasService } from '../../features/source-system/services/aas.service';

export type SystemState = SourceSystem & {
  arcs?: AnyArc[];
  endpoints?: SourceSystemEndpoint[];
  submodels?: SubmodelDescription[];
  isLoadingDetails: boolean; // For endpoints/submodels
};

// Defines the shape of the state managed by this service.
interface ArcState {
  systems: Map<string, SystemState>;
  isLoadingSystems: boolean; // For the initial list of all systems
}

@Injectable({
  providedIn: 'root',
})
export class ArcStateService {
  private monaco: typeof import('monaco-editor') | undefined;
  private http = inject(HttpClient);
  private scriptEditorService = inject(ScriptEditorService);
  private aasService = inject(AasService);
  private readonly SOURCE_TYPES_URI = 'file:///stayinsync-source-types.d.ts';

  private readonly state = new BehaviorSubject<ArcState>({
    systems: new Map(),
    isLoadingSystems: false,
  });

  public readonly systems$: Observable<SystemState[]> = this.state.asObservable().pipe(
    map(s => Array.from(s.systems.values()))
  );

  public readonly arcsBySystem$: Observable<Map<string, AnyArc[]>> = this.systems$.pipe(
    map(systems => {
      const arcMap = new Map<string, AnyArc[]>();
      systems.forEach(system => {
        if (system.arcs && system.arcs.length > 0) {
          arcMap.set(system.name, system.arcs);
        }
      });
      return arcMap;
    })
  );

  // Selector for only active systems (those with ARCs)
  public readonly activeSystems$: Observable<SystemState[]> = this.systems$.pipe(
    map(systems => systems.filter(s => s.arcs && s.arcs.length > 0))
  );

  // (initializeMonaco and addGlobalApiDefinitions remain the same)
  public initializeMonaco(monacoInstance: typeof import('monaco-editor')): void {
    this.monaco = monacoInstance;
    this.addGlobalApiDefinitions();
  }

  public loadTypesForSourceSystemNames(systemNames: string[]): Observable<void> {
    const currentState = this.state.getValue();
    const namesToFetch = systemNames.filter(name => {
      const system = currentState.systems.get(name);
      return system && !system.arcs; // Fetch if the system exists but its ARCs are not loaded
    });

    if (namesToFetch.length === 0) {
      return of(undefined);
    }
    
    // Mark systems as loading
    namesToFetch.forEach(name => this.updateSystemState(name, { isLoadingDetails: true }));

    return this.http.post<ArcMap>('/api/config/source-system/request-configuration/by-source-system-names', namesToFetch)
      .pipe(
        tap(loadedArcMap => {
          for (const [systemName, arcs] of Object.entries(loadedArcMap)) {
            const enrichedArcs = arcs.map(arc => ({ ...arc, sourceSystemName: systemName }));
            this.updateSystemState(systemName, { arcs: enrichedArcs, isLoadingDetails: false });
          }
          // For any systems that were in namesToFetch but not in the response
          namesToFetch.forEach(name => {
              if (!loadedArcMap[name]) {
                  this.updateSystemState(name, { isLoadingDetails: false });
              }
          });
          this.regenerateAllTypeDefinitions();
        }),
        map(() => undefined),
        catchError(err => {
            console.error(`Failed to load ARCs for systems: ${namesToFetch.join(', ')}`, err);
            namesToFetch.forEach(name => this.updateSystemState(name, { isLoadingDetails: false }));
            return of(undefined);
        })
      );
  }

  /**
   * REFACTORED: Fetches the master list of all source systems to initialize the state.
   */
  public initializeGlobalSourceType(): Observable<void> {
    if (!this.monaco) {
      return throwError(() => new Error('ArcStateService not initialized.'));
    }

    this.updateState({ isLoadingSystems: true });

    return this.scriptEditorService.getSourceSystems().pipe(
      tap(allSystems => {
        console.log('%c[DEBUG 1] API returned systems:', 'color: orange; font-weight: bold;', JSON.parse(JSON.stringify(allSystems)));
        const systemsMap = new Map<string, SystemState>();
        allSystems.forEach(system => {
          systemsMap.set(system.name, {
            ...system,
            isLoadingDetails: false,
          });
        });
        console.log('%c[DEBUG 2] Updating state with systems map:', 'color: orange; font-weight: bold;', systemsMap);
        this.updateState({ systems: systemsMap, isLoadingSystems: false });
        this.regenerateAllTypeDefinitions();
      }),
      map(() => undefined)
    );
  }

  /**
   * REFACTORED: Ensures ARCs and details (endpoints/submodels) for a system are loaded.
   * This is the main method for the panel to call.
   */
  public ensureSystemIsLoaded(systemName: string): Observable<void> {
    const system = this.state.getValue().systems.get(systemName);

    // If the system doesn't exist, we can't do anything.
    if (!system) {
      return of(undefined);
    }

    // --- THIS IS THE CORRECTED LOGIC ---
    // Determine if we need to fetch details. We need to fetch if:
    // 1. It's a REST system and `endpoints` are not yet defined.
    // 2. It's an AAS system and `submodels` are not yet defined.
    const needsRestDetails = system.apiType !== 'AAS' && !system.endpoints;
    const needsAasDetails = system.apiType === 'AAS' && !system.submodels;

    // If details are already loaded OR it's currently loading, do nothing.
    if ((!needsRestDetails && !needsAasDetails) || system.isLoadingDetails) {
        console.log(`%c[ArcState] Details for '${systemName}' are already loaded or in-flight. Skipping fetch.`, 'color: #a1a1aa;');
        return of(undefined);
    }

    const loadDetails$ = (system.apiType === 'AAS')
      ? this.aasService.listSubmodels(system.id)
      : this.scriptEditorService.getEndpointsForSourceSystem(system.id);

    // We can also optimize this to not re-fetch arcs if they already exist
    const loadArcs$ = system.arcs 
        ? of(system.arcs) 
        : this.scriptEditorService.getArcsForSourceSystem(system.id).pipe(
            map(arcs => arcs.map(arc => ({...arc, sourceSystemName: systemName})))
        );

    this.updateSystemState(systemName, { isLoadingDetails: true });

    return forkJoin([loadDetails$, loadArcs$]).pipe(
      tap(([details, arcs]) => {
          const partialUpdate = {
            ...(system.apiType === 'AAS' ? { submodels: details as SubmodelDescription[] } : { endpoints: details as SystemState['endpoints'] }),
            arcs: arcs as AnyArc[],
            isLoadingDetails: false,
          };
  
          this.updateSystemState(systemName, partialUpdate);
          this.regenerateAllTypeDefinitions();
      }),
      map(() => undefined),
      catchError(err => {
          console.error(`Failed to load details for system: ${systemName}`, err);
          this.updateSystemState(systemName, { isLoadingDetails: false });
        return of(undefined);
      })
    );
  }

  /**
   * FIX: This method now correctly handles adding a new ARC and ensures the system's
   * details are loaded if it's the first ARC being added.
   */
  public addOrUpdateArc(newArc: AnyArc): void {
    console.log('%c[ArcState] addOrUpdateArc called with:', 'color: #8b5cf6;', newArc);
    if (!newArc.sourceSystemName) {
      console.error('Cannot add ARC: sourceSystemName is missing.', newArc);
      return;
    }

    const currentState = this.state.getValue();
    const system = currentState.systems.get(newArc.sourceSystemName);

    if (!system) {
      console.error(`System '${newArc.sourceSystemName}' not found in state.`);
      return;
    }

    const currentArcs = system.arcs || [];
    const existingIndex = currentArcs.findIndex(a => a.id === newArc.id);

    let updatedArcs: AnyArc[];
    if (existingIndex > -1) {
      updatedArcs = [...currentArcs];
      updatedArcs[existingIndex] = newArc;
    } else {
      updatedArcs = [...currentArcs, newArc];
    }

    // This is the key part of the fix. After adding the ARC, we update the state...
    this.updateSystemState(newArc.sourceSystemName, { arcs: updatedArcs });

    // ...AND if this was the first ARC, we ensure the system details are loaded.
    if (currentArcs.length === 0 && updatedArcs.length === 1) {
      console.log(`%c[ArcState] First ARC for '${newArc.sourceSystemName}'. Ensuring details are loaded.`, 'color: #10b981;');
      this.ensureSystemIsLoaded(newArc.sourceSystemName).pipe(take(1)).subscribe();
    }

    this.regenerateAllTypeDefinitions();
  }

  /**
   * REFACTORED: Simpler logic due to better state structure.
   */
  public removeArc(arcToRemove: AnyArc): void {
    if (!arcToRemove.sourceSystemName) {
      console.error('Cannot remove ARC: sourceSystemName is missing.', arcToRemove);
      return;
    }

    const system = this.state.getValue().systems.get(arcToRemove.sourceSystemName);
    if (!system || !system.arcs) return;

    const filteredArcs = system.arcs.filter(a => a.id !== arcToRemove.id);
    this.updateSystemState(arcToRemove.sourceSystemName, { arcs: filteredArcs });
    this.regenerateAllTypeDefinitions();
  }

  /**
   * REFACTORED: This now reads from the unified `systems` map in the state.
   */
  private regenerateAllTypeDefinitions(): void {
    const monaco = this.monaco;
    if (!monaco) return;

    const currentState = this.state.getValue();
    const allSystems = Array.from(currentState.systems.values());

    if (allSystems.length === 0) return;

    let dts = 'declare const source: {\n';
    let individualTypeInterfaces = '';

    for (const system of allSystems) {
      const validJsIdentifier = this.sanitizeForJs(system.name);
      if (system.arcs && system.arcs.length > 0) {
        dts += `  ${validJsIdentifier}: {\n`;
        for (const arc of system.arcs) {
          const arcNameJs = this.sanitizeForJs(arc.alias);
          const typeName = `${this.capitalize(validJsIdentifier)}${this.capitalize(arcNameJs)}ResponseType`;
          
          // 1. Read the flag
          const isArrayType = arc.responseIsArray;

          // 2. Conditionally add array brackets '[]'
          const finalTypeName = isArrayType ? `${typeName}[]` : typeName;
          
          // 3. Declare the type with NO "payload"
          dts += `    ${arcNameJs}: ${finalTypeName};\n`;

          // 4. Rename 'interface Root' to the base type name
          const dtsContent = arc.responseDts || `interface ${typeName} {}`;
          individualTypeInterfaces += dtsContent
            .replace(/^(export\s+)?interface\s+Root/, `$1interface ${typeName}`) + '\n\n';
        }
        dts += '  };\n';
      } else {
        dts += `  ${validJsIdentifier}: any;\n`;
      }
    }

    const finalDts = dts + '};\n\n' + individualTypeInterfaces;
    monaco.languages.typescript.typescriptDefaults.addExtraLib(finalDts, this.SOURCE_TYPES_URI);
}

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
        'file:///global-stayinsync-api.d.ts'
      );
    }
  }

  private updateSystemState(systemName: string, partialSystemState: Partial<SystemState>): void {
    const currentState = this.state.getValue();
    const newSystemsMap = new Map(currentState.systems);
    const currentSystem = newSystemsMap.get(systemName);

    if (currentSystem) {
      newSystemsMap.set(systemName, { ...currentSystem, ...partialSystemState });
      this.updateState({ systems: newSystemsMap });
    }
  }

  private updateState(partialState: Partial<ArcState>): void {
    this.state.next({ ...this.state.getValue(), ...partialState });
  }

  private capitalize(s: string): string {
    if (!s) return '';
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  /**
   * Converts a string into a safe identifier for JavaScript/TypeScript.
   * Replaces dashes and other invalid characters with underscores.
   */
  private sanitizeForJs(name: string): string {
    if (!name) return 'invalidName';
    return name.replace(/[^a-zA-Z0-9_]/g, '_');
  }
}
