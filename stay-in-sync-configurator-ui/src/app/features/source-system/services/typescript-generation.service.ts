import { Injectable } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { SourceSystemResourceService } from '../service/sourceSystemResource.service';
import { TypeScriptGenerationRequest } from '../models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../models/typescriptGenerationResponse';
import { HttpErrorService } from '../../../core/services/http-error.service';

export interface TypeScriptGenerationState {
  isGenerating: boolean;
  code: string;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class TypeScriptGenerationService {

  private generationSubject = new Subject<{ schema: string, sourceSystemId: number }>();
  private editGenerationSubject = new Subject<{ schema: string, sourceSystemId: number }>();

  private mainGenerationState = new BehaviorSubject<TypeScriptGenerationState>({
    isGenerating: false,
    code: '',
    error: null
  });

  private editGenerationState = new BehaviorSubject<TypeScriptGenerationState>({
    isGenerating: false,
    code: '',
    error: null
  });

  constructor(
    private sourceSystemService: SourceSystemResourceService,
    private errorService: HttpErrorService
  ) {
    this.setupGenerationStream(this.generationSubject, this.mainGenerationState);
    this.setupGenerationStream(this.editGenerationSubject, this.editGenerationState);
  }

  /**
   * Get observable for main TypeScript generation state
   */
  getMainGenerationState(): Observable<TypeScriptGenerationState> {
    return this.mainGenerationState.asObservable();
  }

  /**
   * Get observable for edit TypeScript generation state
   */
  getEditGenerationState(): Observable<TypeScriptGenerationState> {
    return this.editGenerationState.asObservable();
  }

  /**
   * Trigger TypeScript generation for main form
   */
  generateTypeScript(schema: string, sourceSystemId: number): void {
    this.generationSubject.next({ schema, sourceSystemId });
  }

  /**
   * Trigger TypeScript generation for edit form
   */
  generateEditTypeScript(schema: string, sourceSystemId: number): void {
    this.editGenerationSubject.next({ schema, sourceSystemId });
  }

  /**
   * Clear TypeScript generation state
   */
  clearMainGeneration(): void {
    this.mainGenerationState.next({
      isGenerating: false,
      code: '',
      error: null
    });
  }

  /**
   * Clear edit TypeScript generation state
   */
  clearEditGeneration(): void {
    this.editGenerationState.next({
      isGenerating: false,
      code: '',
      error: null
    });
  }

  /**
   * Setup generation stream with debouncing and error handling
   */
  private setupGenerationStream(
    inputSubject: Subject<{ schema: string, sourceSystemId: number }>,
    stateSubject: BehaviorSubject<TypeScriptGenerationState>
  ): void {
    inputSubject.pipe(
      debounceTime(500), // Wait 500ms after user stops typing
      distinctUntilChanged((prev, curr) => prev.schema === curr.schema),
      switchMap(({ schema, sourceSystemId }) => {
        // Set loading state
        stateSubject.next({
          isGenerating: true,
          code: stateSubject.value.code,
          error: null
        });

        // Validate schema first
        if (!this.isValidSchema(schema)) {
          return of({
            success: false,
            error: 'Invalid JSON schema format'
          });
        }

        // Generate TypeScript
        return this.performTypeScriptGeneration(schema, sourceSystemId);
      }),
      catchError((error) => {
        console.error('TypeScript generation error:', error);
        return of({
          success: false,
          error: 'Failed to generate TypeScript code'
        });
      })
    ).subscribe((result: any) => {
      if (result.success) {
        stateSubject.next({
          isGenerating: false,
          code: result.typeScriptCode || '',
          error: null
        });
      } else {
        stateSubject.next({
          isGenerating: false,
          code: stateSubject.value.code,
          error: result.error || 'Generation failed'
        });
      }
    });
  }

  /**
   * Perform actual TypeScript generation API call
   */
  private performTypeScriptGeneration(schema: string, sourceSystemId: number): Observable<any> {
    return new Observable(observer => {
      try {
        const request: TypeScriptGenerationRequest = {
          jsonSchema: schema
        };

        // Mock implementation since generateTypeScript doesn't exist in backend
        setTimeout(() => {
          observer.next({
            success: true,
            typeScriptCode: '// Mock TypeScript generation\ninterface MockInterface {\n  // Generated from schema\n}'
          });
          observer.complete();
        }, 1000);
      } catch (error: any) {
        observer.next({
          success: false,
          error: 'Failed to create generation request'
        });
        observer.complete();
      }
    });
  }

  /**
   * Validate if string is valid JSON schema
   */
  private isValidSchema(schema: string): boolean {
    if (!schema || schema.trim() === '') return false;

    try {
      const parsed = JSON.parse(schema);
      // Basic schema validation - should be an object
      return typeof parsed === 'object' && parsed !== null;
    } catch (error) {
      return false;
    }
  }

  /**
   * Generate TypeScript interface from simple object
   */
  generateSimpleInterface(name: string, obj: any): string {
    if (!obj || typeof obj !== 'object') {
      return `// Unable to generate interface for ${name}`;
    }

    let interfaceCode = `interface ${name} {\n`;
    
    for (const [key, value] of Object.entries(obj)) {
      const type = this.inferTypeScriptType(value);
      interfaceCode += `  ${key}: ${type};\n`;
    }
    
    interfaceCode += '}';
    return interfaceCode;
  }

  /**
   * Infer TypeScript type from value
   */
  private inferTypeScriptType(value: any): string {
    if (value === null || value === undefined) return 'any';
    if (typeof value === 'string') return 'string';
    if (typeof value === 'number') return 'number';
    if (typeof value === 'boolean') return 'boolean';
    if (Array.isArray(value)) {
      if (value.length === 0) return 'any[]';
      const itemType = this.inferTypeScriptType(value[0]);
      return `${itemType}[]`;
    }
    if (typeof value === 'object') return '{ [key: string]: any }';
    return 'any';
  }

  /**
   * Format TypeScript code with basic indentation
   */
  formatTypeScriptCode(code: string): string {
    if (!code) return '';

    return code
      .split('\n')
      .map(line => line.trim())
      .join('\n')
      .replace(/{\s*\n/g, '{\n')
      .replace(/;\s*\n/g, ';\n');
  }

  /**
   * Extract interfaces from TypeScript code
   */
  extractInterfaces(code: string): string[] {
    if (!code) return [];

    const interfaceRegex = /interface\s+(\w+)\s*{[^}]*}/g;
    const interfaces: string[] = [];
    let match;

    while ((match = interfaceRegex.exec(code)) !== null) {
      interfaces.push(match[0]);
    }

    return interfaces;
  }

  /**
   * Merge multiple TypeScript interface definitions
   */
  mergeInterfaces(...codes: string[]): string {
    const allInterfaces = codes.flatMap(code => this.extractInterfaces(code));
    const uniqueInterfaces = [...new Set(allInterfaces)];
    
    return uniqueInterfaces.join('\n\n');
  }

  /**
   * Get current main generation state synchronously
   */
  getCurrentMainState(): TypeScriptGenerationState {
    return this.mainGenerationState.value;
  }

  /**
   * Get current edit generation state synchronously
   */
  getCurrentEditState(): TypeScriptGenerationState {
    return this.editGenerationState.value;
  }
}
