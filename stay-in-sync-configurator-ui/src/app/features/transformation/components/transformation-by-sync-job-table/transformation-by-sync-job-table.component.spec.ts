import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransformationBySyncJobTableComponent } from './transformation-by-sync-job-table.component';

describe('TransformationBySyncJobTableComponent', () => {
  let component: TransformationBySyncJobTableComponent;
  let fixture: ComponentFixture<TransformationBySyncJobTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransformationBySyncJobTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransformationBySyncJobTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
