import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RuleConfigurationComponent } from './rule-configuration.component';

describe('RuleConfigurationComponent', () => {
  let component: RuleConfigurationComponent;
  let fixture: ComponentFixture<RuleConfigurationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuleConfigurationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RuleConfigurationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
