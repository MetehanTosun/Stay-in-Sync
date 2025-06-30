import { Injectable } from "@angular/core";
import { IDisposable } from "monaco-editor";
import { BehaviorSubject, Observable, of } from "rxjs";
import { catchError, map, tap } from 'rxjs/operators';
import { ApiRequestConfiguration } from "../../features/script-editor/models/arc.models";
import { ScriptEditorService } from "./script-editor.service";


// Defines the shape of the state managed by this service.
interface ArcState {
    arcsBySystem: Map<string, ApiRequestConfiguration[]>;
    loadedLibs: Map<string, IDisposable>;
    loadingSystems: Set<string>;
}

@Injectable({
    providedIn: 'root'
})
export class ArcStateService {
    private monaco: typeof import('monaco-editor') | undefined;

    private readonly state = new BehaviorSubject<ArcState>({
        arcsBySystem: new Map(),
        loadedLibs: new Map(),
        loadingSystems: new Set()
    });

    public readonly arcBySystem$ = this.state.asObservable().pipe(
        map(s => s.arcsBySystem)
    );

    constructor(private scriptEditorService: ScriptEditorService) {}

    /**
     * Initializes the service with the Monaco instance.
     * This MUST be called from `onEditorInit` in your main component.
     */
    public initializeMonaco(monacoInstance: typeof import('monaco-editor')): void {
        this.monaco = monacoInstance;
        this.addGlobalApiDefinitions();
    }

    /**
   * The primary method for lazy-loading ARCs and their types for a given system.
   * It is idempotent and prevents re-fetching data that is already loaded or loading.
   * @param systemId The ID of the source system to load.
   */
  public loadTypesForSourceSystem(systemId: string): Observable<void> {
    const currentState = this.state.getValue();
    if (currentState.arcsBySystem.has(systemId) || currentState.loadingSystems.has(systemId)) {
      return of(undefined);
    }

    // Mark as loading
    this.updateState({ loadingSystems: currentState.loadingSystems.add(systemId) });

    return this.scriptEditorService.getArcsForSourceSystem(systemId).pipe(
      tap(arcs => {
        const newState = this.state.getValue();
        newState.arcsBySystem.set(systemId, arcs);
        
        newState.loadingSystems.delete(systemId);

        this.updateState({
          arcsBySystem: newState.arcsBySystem,
          loadingSystems: newState.loadingSystems,
        });

        this.regenerateAllTypeDefinitions();
      }),
      map(() => undefined), // ARCs dont have to be returned, thus undefined.
      catchError(err => {
        console.error(`Failed to load ARCs for system ${systemId}`, err);
        const newState = this.state.getValue();
        newState.loadingSystems.delete(systemId);
        this.updateState({ loadingSystems: newState.loadingSystems });
        return of(undefined);
      })
    );
  }

  /**
   * Call this after a new ARC is created via the wizard to add it to the state
   * and instantly update Monaco's types without a full backend refetch.
   * @param newArc The newly created ARC returned from the backend.
   */
  public addOrUpdateArc(newArc: ApiRequestConfiguration): void {
    const currentState = this.state.getValue();
    const systemArcs = currentState.arcsBySystem.get(newArc.sourceSystemId) || [];
    
    const existingIndex = systemArcs.findIndex(a => a.id === newArc.id);
    if (existingIndex > -1) {
      systemArcs[existingIndex] = newArc;
    } else {
      systemArcs.push(newArc);
    }

    currentState.arcsBySystem.set(newArc.sourceSystemId, systemArcs);
    this.updateState({ arcsBySystem: currentState.arcsBySystem });
    this.regenerateAllTypeDefinitions();
  }

  /**
   * This central function takes the entire current state and generates the
   * complete `.d.ts` file for all loaded source systems.
   */
  private regenerateAllTypeDefinitions(): void {
    if (!this.monaco) return;

    const currentState = this.state.getValue();
    
    // First, dispose of any existing combined library.
    const globalLib = currentState.loadedLibs.get('global-arc-definitions');
    if (globalLib) {
      globalLib.dispose();
    }

    let dts = 'declare const source: {\n';
    let individualTypeInterfaces = '';

    for (const [systemId, arcs] of currentState.arcsBySystem.entries()) {
      // Find a system name from one of the ARCs, or use the ID.
      // TODO: fetch system details separately.
      const systemName = `system_${systemId.replace(/-/g, '_')}`; // Make a valid JS identifier
      
      dts += `  ${systemName}: {\n`;
      for (const arc of arcs) {
        const typeName = `${this.capitalize(arc.alias)}Type`;
        dts += `    ${arc.alias}: ${typeName};\n`;
        individualTypeInterfaces += `${arc.responseDts.replace('interface Root', `interface ${typeName}`)}\n\n`;
      }
      dts += '  };\n';
    }
    dts += '};\n\n' + individualTypeInterfaces;
    
    const disposable = this.monaco.languages.typescript.typescriptDefaults.addExtraLib(dts, 'file:///global-arc-definitions.d.ts');
    currentState.loadedLibs.set('global-arc-definitions', disposable);
    this.updateState({ loadedLibs: currentState.loadedLibs });
  }

  private addGlobalApiDefinitions(): void {
    const stayinsyncApiDts = `
      declare const stayinsync: {
          log: (message: any, logLevel?: 'INFO' | 'WARN' | 'ERROR') => void;
          setOutput: (outputData: any) => void;
      };
    `;
    if (this.monaco) {
       this.monaco.languages.typescript.typescriptDefaults.addExtraLib(stayinsyncApiDts, 'file:///global-stayinsync-api.d.ts');
    }
  }

  private updateState(partialState: Partial<ArcState>): void {
    this.state.next({ ...this.state.getValue(), ...partialState });
  }

  private capitalize(s: string): string {
    return s.charAt(0).toUpperCase() + s.slice(1);
  }
}
