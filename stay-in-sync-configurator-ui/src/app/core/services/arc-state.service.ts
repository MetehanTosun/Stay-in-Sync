import { inject, Injectable } from '@angular/core';
import { IDisposable } from 'monaco-editor';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  ApiRequestConfiguration,
  ArcMap,
} from '../../features/script-editor/models/arc.models';
import { ScriptEditorService } from './script-editor.service';
import { HttpClient } from '@angular/common/http';

// Defines the shape of the state managed by this service.
interface ArcState {
  allSystemNames: string[];
  arcsBySystem: Map<string, ApiRequestConfiguration[]>;
  loadedLibs: Map<string, IDisposable>;
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
    loadedLibs: new Map(),
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
  public addOrUpdateArc(newArc: ApiRequestConfiguration): void {
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
   * This central function generates the complete `.d.ts` file for all loaded ARCs.
   */
  private regenerateAllTypeDefinitions(): void {
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
    const allSystemNames = currentState.allSystemNames;

    if (!allSystemNames || allSystemNames.length === 0) return;

    currentState.loadedLibs.get(this.SOURCE_TYPES_URI)?.dispose();

    this.scriptEditorService
      .getSourceSystemNames()
      .subscribe((allSystemNames) => {
        let dts = 'declare const source: {\n';
        let individualTypeInterfaces = '';

        for (const systemName of allSystemNames) {
          const validJsIdentifier = this.sanitizeForJs(systemName);

          if (currentState.arcsBySystem.has(systemName)) {
            const arcs = currentState.arcsBySystem.get(systemName)!;
            dts += `  ${validJsIdentifier}: {\n`;
            for (const arc of arcs) {
              const arcNameJs = this.sanitizeForJs(arc.alias);
              const typeName = `${this.capitalize(
                validJsIdentifier
              )}${this.capitalize(arcNameJs)}ResponseType`;

              dts += `    ${arcNameJs}: ${typeName};\n`;
              individualTypeInterfaces +=
                (arc.responseDts || 'interface Root {}').replace(
                  'interface Root',
                  `interface ${typeName}`
                ) + '\n\n';
            }
            dts += '  };\n';
          } else {
            // In case ARCs were not loaded for this system yet, we set 'any'.
            dts += `  ${validJsIdentifier}: any;\n`;
          }
        }

        const finalDts = dts + '};\n\n' + individualTypeInterfaces;

        console.log('UPDATING MONACO with types:', finalDts);

        const disposable =
          monaco.languages.typescript.typescriptDefaults.addExtraLib(
            finalDts,
            this.SOURCE_TYPES_URI
          );

        const updatedLibs = new Map(currentState.loadedLibs);
        updatedLibs.set(this.SOURCE_TYPES_URI, disposable);
        this.updateState({ loadedLibs: updatedLibs });
      });
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
