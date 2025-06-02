import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EdcAssetsComponent } from './edc-assets.component';

describe('EdcAssetsComponent', () => {
  let component: EdcAssetsComponent;
  let fixture: ComponentFixture<EdcAssetsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EdcAssetsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EdcAssetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
