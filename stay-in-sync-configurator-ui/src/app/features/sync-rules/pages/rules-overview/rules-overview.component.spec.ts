import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RulesOverviewComponent } from './rules-overview.component';

describe('RulesOverviewComponent', () => {
  let component: RulesOverviewComponent;
  let fixture: ComponentFixture<RulesOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RulesOverviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RulesOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
