import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SetNodeNameModalComponent } from './set-node-name-modal.component';

describe('SetNodeNameModalComponent', () => {
  let component: SetNodeNameModalComponent;
  let fixture: ComponentFixture<SetNodeNameModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SetNodeNameModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SetNodeNameModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
