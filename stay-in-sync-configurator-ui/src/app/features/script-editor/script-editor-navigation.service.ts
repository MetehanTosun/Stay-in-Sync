import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

export interface ScriptEditorData {
  id: number | string;
  transformationId: number | string;
  scriptName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ScriptEditorNavigationService {

  constructor(private router: Router) { }

  /**
   * Navigates to the Script Editor page.
   * @param data The essential data, including the script ID and the associated tranformationID.
   */
  navigateToScriptEditor(data: ScriptEditorData): void {
    this.router.navigate(['script-editor', data.transformationId], {
      state: {
        scriptData: data
      }
    });
  }
}
