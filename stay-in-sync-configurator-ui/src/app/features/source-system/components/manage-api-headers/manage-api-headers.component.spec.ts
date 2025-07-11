import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManageApiHeadersComponent } from './manage-api-headers.component';

describe('ManageApiHeadersComponent', () => {
  let component: ManageApiHeadersComponent;
  let fixture: ComponentFixture<ManageApiHeadersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageApiHeadersComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageApiHeadersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
