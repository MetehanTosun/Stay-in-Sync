import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EdcPartnersComponent } from './edc-partners.component';

describe('EdcPartnersComponent', () => {
  let component: EdcPartnersComponent;
  let fixture: ComponentFixture<EdcPartnersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EdcPartnersComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EdcPartnersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
