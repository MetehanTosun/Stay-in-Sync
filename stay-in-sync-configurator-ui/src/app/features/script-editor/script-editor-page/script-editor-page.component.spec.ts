import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ScriptEditorPageComponent } from './script-editor-page.component';

describe('ScriptEditorPageComponent', () => {
  let component: ScriptEditorPageComponent;
  let fixture: ComponentFixture<ScriptEditorPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScriptEditorPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ScriptEditorPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
