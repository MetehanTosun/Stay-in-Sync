import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ErrorSnapshotPanelComponent } from './error-snapshot-panel.component';

describe('ErrorSnapshotPanelComponent', () => {
  let component: ErrorSnapshotPanelComponent;
  let fixture: ComponentFixture<ErrorSnapshotPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorSnapshotPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ErrorSnapshotPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
