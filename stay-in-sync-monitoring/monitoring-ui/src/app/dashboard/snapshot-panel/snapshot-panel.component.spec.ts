import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SnapshotPanelComponent } from './snapshot-panel.component';

describe('SnapshotPanelComponent', () => {
  let component: SnapshotPanelComponent;
  let fixture: ComponentFixture<SnapshotPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SnapshotPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SnapshotPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
