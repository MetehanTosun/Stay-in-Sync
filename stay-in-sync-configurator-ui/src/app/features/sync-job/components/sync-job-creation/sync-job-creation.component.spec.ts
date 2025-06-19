import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SyncJobCreationComponent } from './sync-job-creation.component';

describe('SyncJobCreationComponent', () => {
  let component: SyncJobCreationComponent;
  let fixture: ComponentFixture<SyncJobCreationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SyncJobCreationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SyncJobCreationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
