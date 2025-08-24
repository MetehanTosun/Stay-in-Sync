import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConstantNodeModalComponent } from './constant-node-modal.component';

describe('ConstantNodeModalComponent', () => {
  let component: ConstantNodeModalComponent;
  let fixture: ComponentFixture<ConstantNodeModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConstantNodeModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConstantNodeModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
