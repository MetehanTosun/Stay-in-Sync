import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EdcAssetsAndPoliciesComponent } from './edc-assets-and-policies.component';

describe('EdcAssetsAndPoliciesComponent', () => {
  let component: EdcAssetsAndPoliciesComponent;
  let fixture: ComponentFixture<EdcAssetsAndPoliciesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EdcAssetsAndPoliciesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EdcAssetsAndPoliciesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
