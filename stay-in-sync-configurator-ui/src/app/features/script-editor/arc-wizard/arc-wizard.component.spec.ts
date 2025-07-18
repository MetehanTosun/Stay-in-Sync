import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArcWizardComponent } from './arc-wizard.component';

describe('ArcWizardComponent', () => {
  let component: ArcWizardComponent;
  let fixture: ComponentFixture<ArcWizardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArcWizardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ArcWizardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
