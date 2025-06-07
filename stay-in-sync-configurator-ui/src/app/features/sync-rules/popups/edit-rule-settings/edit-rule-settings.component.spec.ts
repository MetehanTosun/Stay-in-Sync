import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditRuleSettingsComponent } from './edit-rule-settings.component';

describe('EditRuleSettingsPopup', () => {
  let component: EditRuleSettingsComponent;
  let fixture: ComponentFixture<EditRuleSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditRuleSettingsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditRuleSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
