import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TargetArcWizardComponent } from './target-arc-wizard.component';

describe('TargetArcWizardComponent', () => {
  let component: TargetArcWizardComponent;
  let fixture: ComponentFixture<TargetArcWizardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TargetArcWizardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TargetArcWizardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
