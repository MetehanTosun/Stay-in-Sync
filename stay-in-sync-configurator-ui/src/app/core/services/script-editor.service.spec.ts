import { TestBed } from '@angular/core/testing';

import { ScriptEditorService } from './script-editor.service';

describe('ScriptEditorService', () => {
  let service: ScriptEditorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ScriptEditorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
