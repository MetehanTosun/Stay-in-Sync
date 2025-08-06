import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PolicyTemplatesComponent } from './policy-templates.component';

describe('PolicyTemplatesComponent', () => {
  let component: PolicyTemplatesComponent;
  let fixture: ComponentFixture<PolicyTemplatesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PolicyTemplatesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PolicyTemplatesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
