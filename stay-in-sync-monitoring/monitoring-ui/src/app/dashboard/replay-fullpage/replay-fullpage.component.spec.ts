import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReplayFullpageComponent } from './replay-fullpage.component';

describe('ReplayFullpageComponent', () => {
  let component: ReplayFullpageComponent;
  let fixture: ComponentFixture<ReplayFullpageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReplayFullpageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReplayFullpageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
