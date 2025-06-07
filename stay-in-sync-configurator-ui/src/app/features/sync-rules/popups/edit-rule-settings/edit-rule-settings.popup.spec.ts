import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditRuleSettingsPopup } from './edit-rule-settings.popup';

describe('EditRuleSettingsPopup', () => {
  let component: EditRuleSettingsPopup;
  let fixture: ComponentFixture<EditRuleSettingsPopup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditRuleSettingsPopup]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditRuleSettingsPopup);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
