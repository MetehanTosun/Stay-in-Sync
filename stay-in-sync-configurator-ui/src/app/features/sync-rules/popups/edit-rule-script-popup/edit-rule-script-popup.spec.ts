import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditRuleScriptPopup } from './edit-rule-script-popup';

describe('EditRuleScriptPopup', () => {
  let component: EditRuleScriptPopup;
  let fixture: ComponentFixture<EditRuleScriptPopup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditRuleScriptPopup]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditRuleScriptPopup);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
