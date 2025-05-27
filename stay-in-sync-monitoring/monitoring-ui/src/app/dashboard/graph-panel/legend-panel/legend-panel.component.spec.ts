import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LegendPanelComponent } from './legend-panel.component';

describe('LegendPanelComponent', () => {
  let component: LegendPanelComponent;
  let fixture: ComponentFixture<LegendPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LegendPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LegendPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
