import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SyncJobDetailsPageComponent } from './sync-job-details-page.component';

describe('SyncJobDetailsPageComponent', () => {
  let component: SyncJobDetailsPageComponent;
  let fixture: ComponentFixture<SyncJobDetailsPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SyncJobDetailsPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SyncJobDetailsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
