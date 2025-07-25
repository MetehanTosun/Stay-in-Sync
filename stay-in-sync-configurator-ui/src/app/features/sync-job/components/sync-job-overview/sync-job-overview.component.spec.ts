import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SyncJobOverviewComponent } from './sync-job-overview.component';

describe('SyncJobOverviewComponent', () => {
  let component: SyncJobOverviewComponent;
  let fixture: ComponentFixture<SyncJobOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SyncJobOverviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SyncJobOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
