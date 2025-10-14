import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, SimpleChanges, OnDestroy, ViewChild, ElementRef, OnInit, OnChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';
import { MessageService } from 'primeng/api';
import * as monaco from 'monaco-editor';
import { describeTopLevel, getTruncatedFileName, runTopLevelCheck, setEditorContent } from './set-schema-modal.utils';

/**
 * Modal for editing/creating a JSON Schema string for Schema nodes
 */
@Component({
  selector: 'app-set-schema-modal',
  standalone: true,
  imports: [FormsModule, CommonModule, Dialog, Button, MonacoEditorModule],
  templateUrl: './set-schema-modal.component.html',
  styleUrls: ['../modal-shared.component.css', './set-schema-modal.component.css']
})
export class SetSchemaModalComponent implements OnChanges, OnDestroy {

  //#region Fields
  /** Controls dialog visibility (two-way binding with `visibleChange`) */
  @Input() visible = true;

  /** Emits when dialog visibility changes (two-way binding with `visible`) */
  @Output() visibleChange = new EventEmitter<boolean>();

  /** Current rule schema */
  @Input() currentSchema: string = '';


  /** Emitted when the user saves the schema (payload: schema as string) */
  @Output() save = new EventEmitter<string>();

  /** Emitted when the modal is closed without saving */
  @Output() modalsClosed = new EventEmitter<void>();

  // Current written schema and found errors
  schemaText: string = '';
  parseError: string | null = null;
  schemaValidationErrors: string[] = [];

  /**
   * The maximum file size for the uploaded schema file in KB
   */
  private readonly MAX_FILE_SIZE = 1024 * 1024; // 1 MB

  uploadedFileName: string | null = null;

  // Monaco editor model, options and refs
  jsonEditorOptions = {
    theme: 'vs-dark',
    language: 'json',
    automaticLayout: true,
    minimap: { enabled: false },
    wordWrap: 'on'
  } as const;
  editorModel: NgxEditorModel = { value: '', language: 'json' };
  private monacoEditorRef: monaco.editor.IStandaloneCodeEditor | null = null;

  /**
   * Disposable returned by Monaco's `model.onDidChangeContent` listener.
   *
   * When attaching a new listener or 'OnDestroy' we dispose the previous one to avoid
   * duplicate callbacks and memory leaks
   */
  private monacoContentChangeDisposable: monaco.IDisposable | null = null;

  /**
   * The currently uploaded file as and html element
   */
  @ViewChild('schemaFile', { static: false }) schemaFileInput?: ElementRef<HTMLInputElement> | null;
  //#endregion

  //#region Lifecycle
  /**
   * Syncs editor content when the modal visibility or provided `currentSchema` change.
   */
  ngOnChanges(changes: SimpleChanges) {
    if (!changes['visible'] && !changes['currentSchema']) return;

    this.schemaText = this.currentSchema || '';
    this.editorModel.value = this.schemaText || '';
    this.setEditorContent(this.schemaText);
  }


  /**
   * Clean up resources when the component is destroyed.
   */
  ngOnDestroy(): void {
    try {
      this.monacoContentChangeDisposable?.dispose();
    } catch { }
    this.monacoContentChangeDisposable = null;
    this.monacoEditorRef = null;
  }
  //#endregion

  constructor(private messageService: MessageService) { }

  //#region Editor lifecycle
  /**
   * Stores the Monaco editor reference at editor initialization.
   * Attaches a content-change listener that keeps `schemaText` in sync
   * with the editor. Disposes any previous listener to avoid leaks.
   *
   * @param editorInstance The Monaco editor instance
   */
  async onEditorInit(
    editorInstance: monaco.editor.IStandaloneCodeEditor,
  ): Promise<void> {
    try {
      this.monacoEditorRef = editorInstance;
      const model = editorInstance.getModel();
      if (model) {
        model.setValue(this.schemaText || '');
        this.monacoContentChangeDisposable?.dispose();
        this.monacoContentChangeDisposable = model.onDidChangeContent(() => {
          try {
            this.schemaText = model.getValue();
            this.parseError = null;
            // reset schema validation errors while typing
            this.schemaValidationErrors = [];
          } catch { }
        });
      }
    } catch (e) {
      console.warn('Monaco editor init failed', e);
      this.messageService.add({
        severity: 'error',
        summary: 'Unable to load editor',
        detail: 'Unable to load the editor'
      });
    }
  }

  /**
   * Closes the schema modal and resets the user actions
   */
  closeModal() {
    this.clearUploadedFile();
    this.schemaText = '';
    this.modalsClosed.emit();
    this.visible = false;
    this.visibleChange.emit(false);
  }
  //#endregion

  //#region File handling
  /**
   * Handler for file input change.
   * Loads the uploaded JSON file into editor
   *
   * @param event The file input change event
   */
  onSchemaFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];

    // Limit file size to configured max
    if (file.size > this.MAX_FILE_SIZE) {
      this.messageService.add({ severity: 'error', summary: 'File too large', detail: 'Please upload a file smaller than 1 MB.' });
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      try {
        // Parse and load the file content
        const text = String(reader.result ?? '');
        const parsed = JSON.parse(text);
        this.schemaText = JSON.stringify(parsed, null, 2);
        this.editorModel.value = this.schemaText;
        this.setEditorContent(this.schemaText);

        this.uploadedFileName = file.name;

        // Top-level type check: schema must be an object or boolean
        try {
          const topType = describeTopLevel(parsed);
          if (topType !== 'object' && topType !== 'boolean') {
            const msg = `Top-level JSON Schema must be an object or boolean (true/false); found ${topType}.`;
            this.schemaValidationErrors = [msg];
            this.messageService.add({ severity: 'error', summary: 'Schema invalid', detail: msg });
          } else {
            this.schemaValidationErrors = [];
          }
        } catch { }
      } catch (err: unknown) {
        const errMsg = (err && typeof err === 'object' && 'message' in err) ? (err as any).message : String(err);
        this.messageService.add({ severity: 'error', summary: 'Invalid JSON', detail: `Uploaded file is not valid JSON: ${errMsg}` });
      }
    };
    reader.onerror = () => {
      this.messageService.add({ severity: 'error', summary: 'File read error', detail: 'Unable to read the selected file.' });
    };
    reader.readAsText(file, 'utf-8');
  }

  /**
   * Clears the uploaded file state and resets the file input element
   */
  clearUploadedFile() {
    this.uploadedFileName = null;
    const schemaFile = this.schemaFileInput?.nativeElement;
    if (schemaFile) schemaFile.value = '';
  }

  /**
   * Simulate opening the file input
   */
  triggerFileSelector() {
    const schemaFile = this.schemaFileInput?.nativeElement;
    if (schemaFile) schemaFile.click();
  }

  /**
   * Return a truncated filename if it exceeds 16 characters, preserving extension when possible
   */
  getTruncatedFileName(name: string): string {
    return getTruncatedFileName(name);
  }
  //#endregion

  //#region Modal Actions
  /**
   * Validate and submit the current schema text.
   */
  submit(): void {
    if (!this.schemaText.trim()) {
      this.messageService.add({ severity: 'warn', summary: 'No schema', detail: 'Please enter a JSON Schema' });
      return;
    }

    // Validate JSON
    try {
      const parsed = JSON.parse(this.schemaText);
      const schemaText = JSON.stringify(parsed, null, 2);
      this.parseError = null;

      // Top-level type check
      const topType = describeTopLevel(parsed);
      if (topType !== 'object' && topType !== 'boolean') {
        const msg = `Top-level JSON Schema must be an object or boolean (true/false); found ${topType}.`;
        this.schemaValidationErrors = [msg];
        this.messageService.add({ severity: 'error', summary: 'Schema invalid', detail: msg });
        return;
      }

      // Complete submission
      this.save.emit(schemaText);
      this.closeModal();
    } catch (err: unknown) {
      const errMsg = (err && typeof err === 'object' && 'message' in err) ? (err as any).message : String(err);
      this.parseError = errMsg;
      this.messageService.add({
        severity: 'error',
        summary: 'Invalid JSON',
        detail: `Schema is not valid: ${errMsg}`
      });
      return;
    }
  }

  /**
   * Format the current schema text if valid JSON
   */
  async format() {
    try {
      const parsed = JSON.parse(this.schemaText);
      this.schemaText = JSON.stringify(parsed, null, 2);
      this.editorModel.value = this.schemaText;
      this.setEditorContent(this.schemaText);

      this.parseError = null;
      this.messageService.add({
        severity: 'success',
        summary: 'Formatted',
        detail: 'Schema formatted'
      });

    } catch (err: unknown) {
      const errMsg = (err && typeof err === 'object' && 'message' in err) ? (err as any).message : String(err);
      this.parseError = errMsg;
      this.messageService.add({
        severity: 'error',
        summary: 'Invalid JSON',
        detail: errMsg
      });
    }

    runTopLevelCheck(this.schemaText);
  }
  //#endregion

  //#region Utilities
  /**
   * Synchronizes the given string value into both the simple `NgxEditorModel` and
   * the Monaco editor model (if available).
   *
   * @param value the string value to load into the editor
   */
  private setEditorContent(value: string) {
    this.schemaText = value || '';
    const ok = setEditorContent(this.schemaText, this.editorModel, this.monacoEditorRef || undefined);
    if (!ok) {
      this.messageService.add({
        severity: 'error',
        summary: 'Unable to load schema',
        detail: 'Unable to load schema into the editor'
      });
    }
  }
  //#endregion
}
