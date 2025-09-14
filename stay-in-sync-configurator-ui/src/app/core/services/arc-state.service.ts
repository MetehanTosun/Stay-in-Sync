import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  ApiRequestConfiguration,
  AasArc,
  AnyArc,
  ArcMap,
} from '../../features/script-editor/models/arc.models';
import { ScriptEditorService } from './script-editor.service';
import { HttpClient } from '@angular/common/http';

// Defines the shape of the state managed by this service.
interface ArcState {
  allSystemNames: string[];
  arcsBySystem: Map<string, AnyArc[]>;
  loadingSystems: Set<string>;
}

@Injectable({
  providedIn: 'root',
})
export class ArcStateService {
  private monaco: typeof import('monaco-editor') | undefined;
  private http = inject(HttpClient);
  private scriptEditorService = inject(ScriptEditorService);

  private readonly state = new BehaviorSubject<ArcState>({
    allSystemNames: [],
    arcsBySystem: new Map(),
    loadingSystems: new Set(),
  });

  private readonly SOURCE_TYPES_URI = 'file:///stayinsync-source-types.d.ts';

  public readonly arcsBySystem$ = this.state
    .asObservable()
    .pipe(map((s) => s.arcsBySystem));

  /**
   * Initializes the service with the Monaco instance.
   * This MUST be called from `onEditorInit` in your main component.
   */
  public initializeMonaco(
    monacoInstance: typeof import('monaco-editor')
  ): void {
    this.monaco = monacoInstance;
    this.addGlobalApiDefinitions();
  }

  public initializeGlobalSourceType(): Observable<void> {
    const monaco = this.monaco;
    if (!monaco) {
      return throwError(
        () =>
          new Error(
            'ArcStateService has not been initialized. Call initializeMonaco() first.'
          )
      );
    }

    return this.scriptEditorService.getSourceSystemNames().pipe(
      tap((allSystemNames) => {
        this.updateState({ allSystemNames: allSystemNames });
        this.regenerateAllTypeDefinitions();
      }),
      map(() => undefined)
    );
  }

  /**
   * Fetches all ARCs for a single source system to display in the panel.
   * This is intended to be called when a user expands a source system accordion.
   * It is idempotent and avoids re-fetching if data is already present or loading.
   * @param systemName The name of the source system.
   * @param systemId The ID of the source system.
   */
  public loadArcsForSourceSystem(systemName: string, systemId: number): Observable<void> {
    const currentState = this.state.getValue();
    
    if (currentState.arcsBySystem.has(systemName) || currentState.loadingSystems.has(systemName)) {
      console.log(`%c[ArcState] ARCs for '${systemName}' are already loaded or in-flight. Skipping fetch.`, 'color: #a1a1aa;');
      return of(undefined);
    }

    console.log(`%c[ArcState] Attemping to load ALL ARCs for:`, 'color: #f97316;', systemName);

    const newLoadingSet = new Set(currentState.loadingSystems).add(systemName);
    this.updateState({ loadingSystems: newLoadingSet });

    return this.scriptEditorService.getArcsForSourceSystem(systemId).pipe(
      tap((loadedArcs) => {
        console.groupCollapsed(`%c[ArcState] Received data from API for [${systemName}]`, 'color: #22c55e;');
        console.log('API Response:', loadedArcs);
        console.groupEnd();
        
        const newState = this.state.getValue();
        const updatedArcsBySystem = new Map(newState.arcsBySystem);

        const enrichedArcs = loadedArcs.map((arc) => ({
          ...arc,
          sourceSystemName: systemName,
        }));
        updatedArcsBySystem.set(systemName, enrichedArcs);

        newState.loadingSystems.delete(systemName);

        this.updateState({
          arcsBySystem: updatedArcsBySystem,
          loadingSystems: newState.loadingSystems,
        });

        this.regenerateAllTypeDefinitions();
      }),
      map(() => undefined),
      catchError((err) => {
        console.error(`Failed to load ARCs for system: ${systemName}`, err);
        const newState = this.state.getValue();
        newState.loadingSystems.delete(systemName);
        this.updateState({ loadingSystems: newState.loadingSystems });
        return of(undefined);
      })
    );
  }

  /**
   * The primary method for lazy-loading ARCs for multiple source systems by name.
   * It is idempotent and prevents re-fetching data that is already loaded or is in-flight.
   * @param systemNames An array of source system names detected in the editor.
   */
  public loadTypesForSourceSystemNames(
    systemNames: string[]
  ): Observable<void> {
    const currentState = this.state.getValue();

    const namesToFetch = systemNames.filter(
      (name) =>
        !currentState.arcsBySystem.has(name) &&
        !currentState.loadingSystems.has(name)
    );

    console.log(`%c[ArcState] Attemping to load types for:`, 'color: #f97316;', namesToFetch);

    if (namesToFetch.length === 0) {
      return of(undefined);
    }

    const newLoadingSet = new Set(currentState.loadingSystems);
    namesToFetch.forEach((name) => newLoadingSet.add(name));
    this.updateState({ loadingSystems: newLoadingSet });

    console.log('reaching inside calling batch system names');

    return this.http
      .post<ArcMap>(
        '/api/config/source-system/request-configuration/by-source-system-names',
        namesToFetch
      )
      .pipe(
        tap((loadedArcMap) => {
          console.groupCollapsed(`%c[ArcState] Received data from API for [${namesToFetch.join(', ')}]`, 'color: #22c55e;');
        console.log('API Response:', loadedArcMap);
        console.groupEnd();

          const newState = this.state.getValue();
          const updatedArcsBySystem = new Map(newState.arcsBySystem);

          for (const [systemName, arcs] of Object.entries(loadedArcMap)) {
            const enrichedArcs = arcs.map((arc) => ({
              ...arc,
              sourceSystemName: systemName,
            }));
            updatedArcsBySystem.set(systemName, enrichedArcs);
          }

          namesToFetch.forEach((name) => newState.loadingSystems.delete(name));

          this.updateState({
            arcsBySystem: updatedArcsBySystem,
            loadingSystems: newState.loadingSystems,
          });

          this.regenerateAllTypeDefinitions();
        }),
        map(() => undefined),
        catchError((err) => {
          console.error(
            `Failed to load ARCs for systems: ${namesToFetch.join(', ')}`,
            err
          );
          const newState = this.state.getValue();
          namesToFetch.forEach((name) => newState.loadingSystems.delete(name));
          this.updateState({ loadingSystems: newState.loadingSystems });
          return of(undefined);
        })
      );
  }

  /**
   * Call this after a new ARC is created to add it to the state instantly.
   * @param newArc The newly created ARC. It MUST include `sourceSystemName`.
   */
  public addOrUpdateArc(newArc: AnyArc): void {
    console.log('%c[ArcState] addOrUpdateArc called with:', 'color: #8b5cf6;', newArc);
    if (!newArc.sourceSystemName) {
      console.error(
        'Cannot add ARC to state: sourceSystemName is missing.',
        newArc
      );
      return;
    }

    const currentState = this.state.getValue();
    const updatedArcsBySystem = new Map(currentState.arcsBySystem);
    const systemArcs = updatedArcsBySystem.get(newArc.sourceSystemName) || [];

    const existingIndex = systemArcs.findIndex((a) => a.id === newArc.id);
    if (existingIndex > -1) {
      systemArcs[existingIndex] = newArc;
    } else {
      systemArcs.push(newArc);
    }

    updatedArcsBySystem.set(newArc.sourceSystemName, systemArcs);
    this.updateState({ arcsBySystem: updatedArcsBySystem });
    this.regenerateAllTypeDefinitions();
  }

  /**
   * Call this after an ARC is successfully deleted to remove it from the state.
   * @param arcToRemove The ARC that was deleted. It MUST include `sourceSystemName`.
   */
  public removeArc(arcToRemove: AnyArc): void {
    console.log('%c[ArcState] removeArc called with:', 'color: #ef4444;', arcToRemove);
    if (!arcToRemove.sourceSystemName) {
      console.error('Cannot remove ARC from state: sourceSystemName is missing.', arcToRemove);
      return;
    }

    const currentState = this.state.getValue();
    const updatedArcsBySystem = new Map(currentState.arcsBySystem);
    const systemArcs = updatedArcsBySystem.get(arcToRemove.sourceSystemName) || [];

    const filteredArcs = systemArcs.filter(a => a.id !== arcToRemove.id);

    if (filteredArcs.length > 0) {
      updatedArcsBySystem.set(arcToRemove.sourceSystemName, filteredArcs);
    } else {
      // If no ARCs are left for this system, remove the system key entirely
      updatedArcsBySystem.delete(arcToRemove.sourceSystemName);
    }

    this.updateState({ arcsBySystem: updatedArcsBySystem });
    this.regenerateAllTypeDefinitions();
  }

  /**
   * This central function generates the complete `.d.ts` file for all loaded ARCs.
   */
  private regenerateAllTypeDefinitions(): void {
    console.groupCollapsed('%c[ArcState] Regenerating All Type Definitions...', 'font-weight: bold; color: #ef4444;');
    const monaco = this.monaco;
    if (!monaco) {
      throwError(
        () =>
          new Error(
            'ArcStateService has not been initialized. Call initializeMonaco() first.'
          )
      );
      return;
    }

    const currentState = this.state.getValue();
    console.log('Current state being used for generation:', currentState);
    const allSystemNames = currentState.allSystemNames;

    if (!allSystemNames || allSystemNames.length === 0) {
      console.warn('[ArcState] No system names loaded. Aborting DTS generation.');
      console.groupEnd();
      return;
    }

    let dts = 'declare const source: {\n';
    let individualTypeInterfaces = '';

    for (const systemName of allSystemNames) {
      const validJsIdentifier = this.sanitizeForJs(systemName);

      if (currentState.arcsBySystem.has(systemName)) {
        const arcs = currentState.arcsBySystem.get(systemName)!;
        dts += `  ${validJsIdentifier}: {\n`;
        for (const arc of arcs) {
          const arcNameJs = this.sanitizeForJs(arc.alias);
          const typeName = `${this.capitalize(validJsIdentifier)}${this.capitalize(arcNameJs)}ResponseType`;

          dts += `    ${arcNameJs}: ${typeName};\n`;
          individualTypeInterfaces += (arc.responseDts || `export interface ${typeName} {}`)
            .replace(/^(export\s+)?(interface|type)\s+Root/, `$1$2 ${typeName}`) + '\n\n';
        }
        dts += '  };\n';
      } else {
        // This system's types haven't been loaded yet. Define it as 'any'.
        dts += `  ${validJsIdentifier}: any;\n`;
      }
    }

    const finalDts = dts + '};\n\n' + individualTypeInterfaces;
    console.log('[ArcState] Upserting library content into Monaco...');

    monaco.languages.typescript.typescriptDefaults.addExtraLib(finalDts,this.SOURCE_TYPES_URI);
    console.log('[ArcState] Library update call complete.');

    // ================== NEW VERIFICATION STEP ==================
// Use a setTimeout to allow Monaco's event loop to process the update
setTimeout(() => {
    const allLibs = monaco.languages.typescript.typescriptDefaults.getExtraLibs();
    console.groupCollapsed('%c[VERIFICATION] Checking Monaco\'s loaded libraries...', 'color: #10b981; font-weight: bold;');
    console.log('Total libs found:', Object.keys(allLibs).length);

    // The keys of this object are the URIs of the libs
    console.log('All library URIs:', Object.keys(allLibs));

    // Let's check the content of OUR specific library
    const ourLibContent = allLibs[this.SOURCE_TYPES_URI]?.content;
    if (ourLibContent) {
        console.log(`%cContent of '${this.SOURCE_TYPES_URI}':`, 'font-weight: bold;');
        console.log(ourLibContent);
    } else {
        console.error(`%cFAILURE: Library '${this.SOURCE_TYPES_URI}' was NOT FOUND in Monaco after update!`, 'color: red;');
    }
    console.groupEnd();
}, 500); // 500ms delay to be safe
// ==========================================================

    console.groupEnd();
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

  private updateState(partialState: Partial<ArcState>): void {
    const oldState = this.state.getValue();
    const newState = { ...oldState, ...partialState };

    console.groupCollapsed(`%c[ArcState] State Updated`, 'font-weight: bold; color: #3b82f6;');
    console.log('%cPartial state applied:', 'font-weight: bold;', partialState);
    console.log('%cPrevious full state:', 'font-weight: bold;', oldState);
    console.log('%cNew full state:', 'font-weight: bold;', newState);
    console.groupEnd();

    this.state.next(newState);


    //this.state.next({ ...this.state.getValue(), ...partialState });
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
