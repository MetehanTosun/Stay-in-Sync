import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SyncJobContextPanelComponent } from './sync-job-context-panel.component';

describe('SyncJobContextPanelComponent', () => {
  let component: SyncJobContextPanelComponent;
  let fixture: ComponentFixture<SyncJobContextPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SyncJobContextPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SyncJobContextPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
