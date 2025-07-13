import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransformationBaseComponent } from './transformation-base.component';

describe('TransformationBaseComponent', () => {
  let component: TransformationBaseComponent;
  let fixture: ComponentFixture<TransformationBaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransformationBaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransformationBaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
