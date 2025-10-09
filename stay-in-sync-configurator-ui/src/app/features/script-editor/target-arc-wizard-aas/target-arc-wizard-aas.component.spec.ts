import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TargetArcWizardAasComponent } from './target-arc-wizard-aas.component';

describe('TargetArcWizardAasComponent', () => {
  let component: TargetArcWizardAasComponent;
  let fixture: ComponentFixture<TargetArcWizardAasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TargetArcWizardAasComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TargetArcWizardAasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
