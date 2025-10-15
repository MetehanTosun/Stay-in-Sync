import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CreateTargetSystemDialogService {

  constructor() {}

  /**
   * Get submodel templates
   */
  getSubmodelTemplates(): { [key: string]: string } {
    return {
      minimal: `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "kind": "Instance"
}`,
      property: `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "submodelElements": [
    { "modelType": "Property", "idShort": "Name", "valueType": "xs:string", "value": "Foo" }
  ]
}`,
      collection: `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "submodelElements": [
    { "modelType": "SubmodelElementCollection", "idShort": "address", "value": [ { "modelType": "Property", "idShort": "street", "valueType": "xs:string", "value": "Main St" } ] }
  ]
}`
    };
  }

  /**
   * Get element templates
   */
  getElementTemplates(): { [key: string]: string } {
    return {
      property: `{"modelType":"Property","idShort":"NewProp","valueType":"xs:string","value":"Foo"}`,
      range: `{"modelType":"Range","idShort":"NewRange","valueType":"xs:double","min":0,"max":100}`,
      mlp: `{"modelType":"MultiLanguageProperty","idShort":"Title","value":[{"language":"en","text":"Example"}]}`,
      ref: `{"modelType":"ReferenceElement","idShort":"Ref","value":{"type":"ModelReference","keys":[{"type":"Submodel","value":"https://example.com/ids/sm"}]}}`,
      rel: `{"modelType":"RelationshipElement","idShort":"Rel","first":{"type":"ModelReference","keys":[{"type":"Submodel","value":"https://example.com/ids/sm1"}]},"second":{"type":"ModelReference","keys":[{"type":"Submodel","value":"https://example.com/ids/sm2"}]}}`,
      annrel: `{"modelType":"AnnotatedRelationshipElement","idShort":"AnnRel","first":{"type":"ModelReference","keys":[{"type":"Submodel","value":"https://example.com/ids/sm1"}]},"second":{"type":"ModelReference","keys":[{"type":"Submodel","value":"https://example.com/ids/sm2"}]},"annotations":[{"modelType":"Property","idShort":"note","valueType":"xs:string","value":"Hello"}]}`,
      collection: `{"modelType":"SubmodelElementCollection","idShort":"group","value":[]}`,
      list: `{"modelType":"SubmodelElementList","idShort":"items","typeValueListElement":"Property","valueTypeListElement":"xs:string","value":[]}`,
      file: `{"modelType":"File","idShort":"file1","contentType":"text/plain","value":"path-or-url.txt"}`,
      operation: `{"modelType":"Operation","idShort":"Op","inputVariables":[{"value":{"modelType":"Property","idShort":"in","valueType":"xs:string"}}],"outputVariables":[]}`,
      entity: `{"modelType":"Entity","idShort":"Ent","entityType":"SelfManagedEntity","statements":[]}`
    };
  }

  /**
   * Set template content
   */
  setTemplate(templates: { [key: string]: string }, kind: string, defaultContent: string = '{}'): string {
    return templates[kind] || defaultContent;
  }

  /**
   * Read file content as text
   */
  readFileAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const text = String(reader.result || '').trim();
          if (text) {
            JSON.parse(text); 
            resolve(text);
          } else {
            reject(new Error('Empty file content'));
          }
        } catch (error) {
          reject(new Error('Invalid JSON format'));
        }
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsText(file);
    });
  }

  /**
   * Get AASX selection for submodel
   */
  getOrInitAasxSelection(selection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> }, sm: any): { id: string; full: boolean; elements: string[] } {
    const id = this.getSmId(sm);
    let found = selection.submodels.find(s => s.id === id);
    if (!found) {
      found = { id, full: true, elements: [] };
      selection.submodels.push(found);
    }
    return found;
  }

  /**
   * Toggle AASX submodel full selection
   */
  toggleAasxSubmodelFull(selection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> }, sm: any, checked: boolean): void {
    const sel = this.getOrInitAasxSelection(selection, sm);
    sel.full = !!checked;
    if (sel.full) sel.elements = [];
  }

  /**
   * Check if AASX element is selected
   */
  isAasxElementSelected(selection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> }, sm: any, idShort: string): boolean {
    const sel = this.getOrInitAasxSelection(selection, sm);
    return sel.elements.includes(idShort);
  }

  /**
   * Toggle AASX element selection
   */
  toggleAasxElement(selection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> }, sm: any, idShort: string, checked: boolean): void {
    const sel = this.getOrInitAasxSelection(selection, sm);
    sel.full = false;
    const exists = sel.elements.includes(idShort);
    if (checked) {
      if (!exists) sel.elements.push(idShort);
    } else {
      if (exists) sel.elements = sel.elements.filter(x => x !== idShort);
    }
  }

  /**
   * Get selected submodel IDs for AASX upload
   */
  getSelectedSubmodelIds(selection: { submodels: Array<{ id: string; full: boolean; elements: string[] }> }): string[] {
    const hasSelection = selection?.submodels?.some(s => s.full || (s.elements && s.elements.length > 0));
    return hasSelection ? selection.submodels.map(s => s.id).filter(Boolean) : [];
  }

  /**
   * Get submodel ID from submodel object
   */
  private getSmId(sm: any): string {
    return sm?.id || sm?.submodelId || '';
  }
}
