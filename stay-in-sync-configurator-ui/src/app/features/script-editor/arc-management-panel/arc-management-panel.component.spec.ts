import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArcManagementPanelComponent } from './arc-management-panel.component';

describe('ArcManagementPanelComponent', () => {
  let component: ArcManagementPanelComponent;
  let fixture: ComponentFixture<ArcManagementPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArcManagementPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ArcManagementPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
