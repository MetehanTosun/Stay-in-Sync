import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArcWizardAasComponent } from './arc-wizard-aas.component';

describe('ArcWizardAasComponent', () => {
  let component: ArcWizardAasComponent;
  let fixture: ComponentFixture<ArcWizardAasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArcWizardAasComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ArcWizardAasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
