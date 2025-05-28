import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EdcBaseComponent } from './edc-base.component';

describe('EdcBaseComponent', () => {
  let component: EdcBaseComponent;
  let fixture: ComponentFixture<EdcBaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EdcBaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EdcBaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
