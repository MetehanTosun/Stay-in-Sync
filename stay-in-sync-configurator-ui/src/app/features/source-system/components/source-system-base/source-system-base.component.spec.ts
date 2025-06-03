// biome-ignore lint/style/useImportType: <explanation>
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SourceSystemBaseComponent } from './source-system-base.component';

describe('SourceSystemBaseComponent', () => {
  let component: SourceSystemBaseComponent;
  let fixture: ComponentFixture<SourceSystemBaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SourceSystemBaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceSystemBaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
