// biome-ignore lint/style/useImportType: <explanation>
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateSourceSystemComponent } from './create-source-system.component';

describe('SourceSystemFormComponent', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateSourceSystemComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
