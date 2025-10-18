import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TypeScriptGenerationService } from './typescript-generation.service';
import { SourceSystemResourceService } from '../service/sourceSystemResource.service';
import { HttpErrorService } from '../../../core/services/http-error.service';

describe('TypeScriptGenerationService', () => {
  let service: TypeScriptGenerationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        TypeScriptGenerationService,
        { provide: SourceSystemResourceService, useValue: {} },
        { provide: HttpErrorService, useValue: { handleError: () => {} } }
      ]
    });
    service = TestBed.inject(TypeScriptGenerationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getCurrentMainState returns initial state', () => {
    const state = service.getCurrentMainState();
    expect(state.isGenerating).toBeFalse();
    expect(state.code).toBe('');
    expect(state.error).toBeNull();
  });

  it('generateSimpleInterface builds fields with inferred types', () => {
    const code = service.generateSimpleInterface('MyType', { a: 'x', b: 1, c: true, d: [1], e: {} });
    expect(code).toContain('interface MyType');
    expect(code).toContain('a: string;');
    expect(code).toContain('b: number;');
    expect(code).toContain('c: boolean;');
    expect(code).toContain('d: number[]');
    expect(code).toContain('e: { [key: string]: any }');
  });

  it('formatTypeScriptCode trims lines and normalizes braces', () => {
    const formatted = service.formatTypeScriptCode(' interface X {  \n  a: string;  \n} ');
    expect(formatted).toContain('interface X');
    expect(formatted).toContain('a: string;');
  });

  it('extractInterfaces finds interface blocks', () => {
    const code = 'interface A { a: string; }\ninterface B { b: number; }';
    const list = service.extractInterfaces(code);
    expect(list.length).toBe(2);
    expect(list[0]).toContain('interface A');
  });

  it('mergeInterfaces deduplicates', () => {
    const merged = service.mergeInterfaces('interface A { a: string; }', 'interface A { a: string; }', 'interface B { b: number; }');
    expect(merged).toContain('interface A');
    expect(merged).toContain('interface B');
  });
});


