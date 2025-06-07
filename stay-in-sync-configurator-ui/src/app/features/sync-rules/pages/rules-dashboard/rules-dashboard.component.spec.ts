import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RulesDashboardComponent } from './rules-dashboard.component';

describe('RuleDashboard', () => {
  let component: RulesDashboardComponent;
  let fixture: ComponentFixture<RulesDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RulesDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RulesDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
