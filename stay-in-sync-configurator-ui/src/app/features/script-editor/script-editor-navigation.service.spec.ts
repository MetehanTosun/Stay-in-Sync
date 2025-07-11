import { TestBed } from '@angular/core/testing';

import { ScriptEditorNavigationService } from './script-editor-navigation.service';

describe('ScriptEditorNavigationService', () => {
  let service: ScriptEditorNavigationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ScriptEditorNavigationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
