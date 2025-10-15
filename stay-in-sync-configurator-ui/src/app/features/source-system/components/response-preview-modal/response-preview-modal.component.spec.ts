import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ResponsePreviewModalComponent } from './response-preview-modal.component';

describe('ResponsePreviewModalComponent', () => {
  let component: ResponsePreviewModalComponent;
  let fixture: ComponentFixture<ResponsePreviewModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ResponsePreviewModalComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ResponsePreviewModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should update jsonEditorModel when responseBodySchema is set', () => {
    const validJson = '{"a":1}';
    component.responseBodySchema = validJson;
    component.ngOnChanges({} as any);
    expect(component.jsonEditorModel.value).toContain('"a"');
  });

  it('should expose jsonEditorOptions and typescriptEditorOptions', () => {
    expect(component.jsonEditorOptions.language).toBe('json');
    expect(component.typescriptEditorOptions.language).toBe('typescript');
  });
}); 