import { Component, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms'; // For ngModel
import { CommonModule } from '@angular/common'; // For *ngIf, json pipe etc.
import { HttpClient } from '@angular/common/http';
import type { editor } from 'monaco-editor'; // Import Monaco types
import * as ts from 'typescript'; // For transpiling

// PrimeNG Modules needed for the template
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';

// Monaco Editor Module
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';

@Component({
  selector: 'app-script-editor-page',
  templateUrl: './script-editor-page.component.html',
  styleUrls: ['./script-editor-page.component.css'],
  standalone: true, // This confirms it's a standalone component
  imports: [
    CommonModule,        // For *ngIf, json pipe
    FormsModule,         // For [(ngModel)]
    MonacoEditorModule,  // For <ngx-monaco-editor>
    PanelModule,         // For <p-panel>
    ButtonModule         // For <p-button>
  ]
})
export class ScriptEditorPageComponent implements OnDestroy {
  editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    minimap: { enabled: true }
  };
  code: string = `// Welcome to the TypeScript Script Editor!

// Example: Accessing a globally available object (from your .d.ts)
// declare var myApi: { getData: (id: string) => Promise<{ message: string }> };

async function processData(id: string): Promise<string> {
    console.log('Fetching data for ID:', id);
    // const result = await myApi.getData(id);
    // return \`Processed: \${result.message}\`;
    return \`Processed ID: \${id}\`; // Placeholder
}

processData('test-123')
    .then(console.log)
    .catch(console.error);
`;

  private monacoInstance: editor.IStandaloneCodeEditor | undefined;
  executionResult: any = null;
  executionError: any = null;

  constructor() {}

  onEditorInit(editorInstance: editor.IStandaloneCodeEditor): void {
    this.monacoInstance = editorInstance;
    console.log('Monaco editor instance initialized:', editorInstance);
  }

  runScript(): void {
    this.executionResult = null;
    this.executionError = null;

    if (!this.code) {
      this.executionError = 'No script to run.';
      return;
    }

    try {
      const transpileOutput = ts.transpileModule(this.code, {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ESNext,
          esModuleInterop: true,
        }
      });

      if (transpileOutput.diagnostics && transpileOutput.diagnostics.length > 0) {
        console.error('Transpilation errors:', transpileOutput.diagnostics);
        this.executionError = {
          message: 'Transpilation failed. Check console for details.',
          diagnostics: transpileOutput.diagnostics.map(d => ({
            // Ensure ts.flattenDiagnosticMessageText is correctly used
            message: typeof d.messageText === 'string' ? d.messageText : ts.flattenDiagnosticMessageText(d.messageText, '\n'),
            category: ts.DiagnosticCategory[d.category],
            code: d.code
          }))
        };
        return;
      }

      const javascriptCode = transpileOutput.outputText;
      console.log('Transpiled JavaScript:', javascriptCode);

      // API call here for script saving

    } catch (e) {
      this.executionError = { message: 'Error during transpilation or sending.', details: e };
      console.error('Error in runScript:', e);
    }
  }

  ngOnDestroy(): void {
    if (this.monacoInstance) {
      // ngx-monaco-editor should handle disposal of the editor instance it creates.
      // If you manually created other Monaco resources, dispose them here.
      // this.monacoInstance.dispose(); // Usually not needed for the main editor from the component
    }
  }
}
