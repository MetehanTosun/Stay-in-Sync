import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransformationRuleSelectionComponent } from './transformation-rule-selection.component';

describe('TransformationRuleSelectionComponent', () => {
  let component: TransformationRuleSelectionComponent;
  let fixture: ComponentFixture<TransformationRuleSelectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransformationRuleSelectionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransformationRuleSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
