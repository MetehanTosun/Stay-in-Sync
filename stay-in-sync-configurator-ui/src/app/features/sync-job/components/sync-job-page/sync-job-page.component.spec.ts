import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SyncJobPageComponent } from './sync-job-page.component';

describe('SyncJobPageComponent', () => {
  let component: SyncJobPageComponent;
  let fixture: ComponentFixture<SyncJobPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SyncJobPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SyncJobPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
