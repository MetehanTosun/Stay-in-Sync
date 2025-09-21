import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TargetArcPanelComponent } from './target-arc-panel.component';

describe('TargetArcPanelComponent', () => {
  let component: TargetArcPanelComponent;
  let fixture: ComponentFixture<TargetArcPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TargetArcPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TargetArcPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
