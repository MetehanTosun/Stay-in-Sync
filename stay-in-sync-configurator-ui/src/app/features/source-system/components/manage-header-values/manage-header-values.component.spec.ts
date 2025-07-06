import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManageHeaderValuesComponent } from './manage-header-values.component';

describe('ManageHeaderValuesComponent', () => {
  let component: ManageHeaderValuesComponent;
  let fixture: ComponentFixture<ManageHeaderValuesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageHeaderValuesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageHeaderValuesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
