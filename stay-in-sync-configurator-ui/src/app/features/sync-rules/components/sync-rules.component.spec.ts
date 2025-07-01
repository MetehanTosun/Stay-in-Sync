import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SyncRulesComponent } from './sync-rules.component';

describe('SyncRulesComponent', () => {
  let component: SyncRulesComponent;
  let fixture: ComponentFixture<SyncRulesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SyncRulesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SyncRulesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
