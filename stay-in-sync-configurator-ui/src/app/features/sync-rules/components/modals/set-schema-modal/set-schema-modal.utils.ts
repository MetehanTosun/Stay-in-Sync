import * as monaco from 'monaco-editor';
import type { NgxEditorModel } from 'ngx-monaco-editor-v2';
/**
 * Utility helpers used by the SetSchemaModalComponent
 */

/**
 * Return a truncated filename; keeping the extension when possible
 *
 * @param name The original filename
 * @param maxFilenameLength Maximum allowed length of characters (defaults to 16)
 * @returns A shortened filename
 */
export function getTruncatedFileName(name: string, maxFilenameLength = 16): string {
  if (!name) return '';
  const maxLen = maxFilenameLength;
  if (name.length <= maxLen) return name;

  const dot = name.lastIndexOf('.');
  const hasExt = dot > 0 && dot < name.length - 1;
  if (!hasExt) {
    return name.slice(0, maxLen - 3) + '...';
  }
  const ext = name.slice(dot);
  const base = name.slice(0, dot);
  const keep = Math.max(1, maxLen - ext.length - 3);
  return base.slice(0, keep) + '...' + ext;
}

/**
 * Synchronizes the given string value into both the simple `NgxEditorModel` and
 * the Monaco editor model (if provided). Returns true on success
 * or false if an error occurred while interacting with Monaco.
 */
export function setEditorContent
  (
    value: string,
    editorModel: NgxEditorModel,
    monacoEditorRef?: monaco.editor.IStandaloneCodeEditor
  ): boolean {
  const v = String(value || '');
  editorModel.value = v;
  try {
    const model = monacoEditorRef?.getModel?.();
    if (model && model.getValue() !== v) {
      model.setValue(v);
    }
    return true;
  } catch {
    return false;
  }
}

/**
 * Return the data type for the top-level JSON value.
 *
 * @param value - The value to inspect
 * @returns A short string describing the top-level type
 */
export function describeTopLevel(value: unknown): string {
  if (typeof value === 'boolean') return 'boolean';
  if (typeof value === 'object') return Array.isArray(value) ? 'array' : 'object';
  return typeof value;
}

/**
 * Runs a top-level type check on the given JSON schema and returns validation errors.
 *
 * @param schemaText The JSON schema as a string
 * @returns An array of validation error messages (empty if valid)
 */
export function runTopLevelCheck(schemaText: string): string[] {
  try {
    const parsed = JSON.parse(schemaText);
    const topType = describeTopLevel(parsed);
    if (topType !== 'object' && topType !== 'boolean') {
      return [`Top-level JSON Schema must be an object or boolean (true/false); found ${topType}.`];
    }
    return [];
  } catch {
    return [];
  }
}
