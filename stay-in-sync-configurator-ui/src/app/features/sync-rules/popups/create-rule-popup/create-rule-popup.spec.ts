import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateRulePopup } from './create-rule-popup';

describe('CreateRulePopup', () => {
  let component: CreateRulePopup;
  let fixture: ComponentFixture<CreateRulePopup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateRulePopup]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateRulePopup);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
