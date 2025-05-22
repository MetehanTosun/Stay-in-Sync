import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssBaseComponent } from './ass-base.component';

describe('AssBaseComponent', () => {
  let component: AssBaseComponent;
  let fixture: ComponentFixture<AssBaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssBaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AssBaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
