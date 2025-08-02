import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-response-preview-modal',
  standalone: true,
  imports: [
    CommonModule,
    MonacoEditorModule,
    DialogModule,
    ButtonModule,
    ProgressSpinnerModule
  ],
  templateUrl: './response-preview-modal.component.html',
  styleUrls: ['./response-preview-modal.component.css']
})
export class ResponsePreviewModalComponent implements OnInit {
  @Input() visible: boolean = false;
  @Input() endpointId: number | null | undefined = null;
  @Input() endpointPath: string = '';
  @Input() httpMethod: string = '';
  @Input() responseBodySchema: string | null | undefined = null;
  @Output() visibleChange = new EventEmitter<boolean>();

  editorOptions = {
    theme: 'vs-dark',
    language: 'json',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false
  };

  editorModel: NgxEditorModel = { value: '', language: 'json' };
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    this.updateEditorModel();
  }

  ngOnChanges(): void {
    this.updateEditorModel();
  }

  private updateEditorModel(): void {
    if (this.responseBodySchema && this.responseBodySchema.trim()) {
      try {
        // Try to format the JSON for better readability
        const parsed = JSON.parse(this.responseBodySchema);
        this.editorModel = {
          value: JSON.stringify(parsed, null, 2),
          language: 'json'
        };
        this.error = null;
      } catch (e) {
        // If it's not valid JSON, display as-is
        this.editorModel = {
          value: this.responseBodySchema,
          language: 'json'
        };
        this.error = 'Response body schema is not valid JSON';
      }
    } else {
      this.editorModel = {
        value: '// No response body schema available for this endpoint',
        language: 'json'
      };
      this.error = null;
    }
  }

  onClose(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  get hasResponseBody(): boolean {
    if (!this.responseBodySchema || this.responseBodySchema === undefined || this.responseBodySchema.trim().length === 0) {
      return false;
    }
    
    try {
      JSON.parse(this.responseBodySchema);
      return true;
    } catch (e) {
      return false;
    }
  }

  get modalTitle(): string {
    return `Response Preview - ${this.httpMethod} ${this.endpointPath}`;
  }
} 