import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReplayPanelComponent } from './replay-panel.component';

describe('ReplayPanelComponent', () => {
  let component: ReplayPanelComponent;
  let fixture: ComponentFixture<ReplayPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReplayPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReplayPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
