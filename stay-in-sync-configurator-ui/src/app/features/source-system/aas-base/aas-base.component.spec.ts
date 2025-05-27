// biome-ignore lint/style/useImportType: <explanation>
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AasBaseComponent } from './aas-base.component';

describe('AssBaseComponent', () => {
  let component: AasBaseComponent;
  let fixture: ComponentFixture<AasBaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AasBaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AasBaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
