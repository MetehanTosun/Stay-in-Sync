import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransformationAddSyncJobComponent } from './transformation-add-sync-job.component';

describe('TransformationAddSyncJobComponent', () => {
  let component: TransformationAddSyncJobComponent;
  let fixture: ComponentFixture<TransformationAddSyncJobComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransformationAddSyncJobComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransformationAddSyncJobComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
