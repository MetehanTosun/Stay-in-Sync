import { Pipe, PipeTransform } from '@angular/core';

/**
 * This pipe creates short previews of JSON schemas
 */
@Pipe({
  name: 'schemaPreview',
  pure: true,
})
export class SchemaPreviewPipe implements PipeTransform {
  /**
   * Transform a schema string into a short preview.
   *
   * @param schema The input schema as string
   * @param maxLines Maximum number of lines to include in the preview (default 3)
   * @returns A short, line-limited preview. Returns 'no schema' for empty input.
   */
  transform(schema: string, maxLines = 3): string {
    const rawSchema = String(schema ?? '');
    if (!rawSchema) return 'no schema';

    // Helper: truncate to maxLines and append an ellipsis when needed.
    // Returns '{}' for zero lines to indicate an empty JSON object preview.
    const truncate = (lines: string[]): string => {
      if (lines.length === 0) return '{}';
      return lines.length > maxLines ? lines.slice(0, maxLines).join('\n') + '\n…' : lines.join('\n');
    };

    try {
      const parsed = JSON.parse(rawSchema as string);

      if (parsed && typeof parsed === 'object') {
        const prettyJSON = JSON.stringify(parsed, null, 2);
        let lines = prettyJSON.split(/\r?\n/);

        // Remove outer braces if printed on their own lines and dedent inner lines
        if (lines.length >= 2 && lines[0].trim() === '{' && lines[lines.length - 1].trim() === '}') {
          lines = lines.slice(1, -1).map((l) => l.replace(/^ {2}/, ''));
        }

        return truncate(lines);
      }

      // For JSON primitives (number, string, boolean, null) convert to string and return
      const previewSchema = String(parsed);
      return truncate(previewSchema.split(/\r?\n/));
    } catch {
      // Non-JSON content: split into lines, trim whitespace, drop empty lines, then truncate
      const lines = rawSchema.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
      return truncate(lines);
    }
  }
}
