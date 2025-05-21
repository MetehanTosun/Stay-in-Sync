// biome-ignore lint/style/useImportType: <explanation>
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SourceSystemFormComponent } from './source-system-form.component';

describe('SourceSystemFormComponent', () => {
  let component: SourceSystemFormComponent;
  let fixture: ComponentFixture<SourceSystemFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SourceSystemFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceSystemFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
