import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MessageService } from 'primeng/api';
import { AasService } from './aas.service';
import { HttpErrorService } from '../../../core/services/http-error.service';
import { CreateSourceSystemAasService } from './create-source-system-aas.service';

@Injectable({
  providedIn: 'root'
})
export class CreateSourceSystemDialogService {

  constructor(
    private aasService: AasService,
    private messageService: MessageService,
    private errorService: HttpErrorService,
    private aasManagementService: CreateSourceSystemAasService
  ) {}

  /**
   * AASX Upload Management
   */
  openAasxUpload(): void {
    // Implementation for opening AASX upload dialog
  }

  onAasxFileSelected(event: any, aasxPreview: any, aasxSelection: any): void {
    const file = event.files[0];
    if (file) {
      // Preview AASX file logic would go here
      console.log('AASX file selected:', file.name);
    }
  }

  getOrInitAasxSelFor(smId: string, aasxSelection: any): any {
    if (!aasxSelection.submodels) {
      aasxSelection.submodels = [];
    }
    let found = aasxSelection.submodels.find((s: any) => s.id === smId);
    if (!found) {
      found = { id: smId, full: false, elements: [] };
      aasxSelection.submodels.push(found);
    }
    return found;
  }

  toggleAasxSubmodelFull(smId: string, aasxSelection: any): void {
    const sel = this.getOrInitAasxSelFor(smId, aasxSelection);
    sel.full = !sel.full;
    if (sel.full) {
      sel.elements = [];
    }
  }

  isAasxElementSelected(smId: string, elIdShort: string, aasxSelection: any): boolean {
    const sel = this.getOrInitAasxSelFor(smId, aasxSelection);
    return sel.full || sel.elements.includes(elIdShort);
  }

  toggleAasxElement(smId: string, elIdShort: string, aasxSelection: any): void {
    const sel = this.getOrInitAasxSelFor(smId, aasxSelection);
    if (sel.full) return;
    
    const idx = sel.elements.indexOf(elIdShort);
    if (idx >= 0) {
      sel.elements.splice(idx, 1);
    } else {
      sel.elements.push(elIdShort);
    }
  }

  async uploadAasx(sourceSystemId: number, file: File, selection: any): Promise<any> {
    try {
      const result = await this.aasService.attachSelectedAasx(sourceSystemId, file, selection).toPromise();
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'AASX uploaded successfully!' });
      return result;
    } catch (error) {
      this.errorService.handleError(error as any);
      throw error;
    }
  }

  /**
   * Submodel Creation
   */
  openCreateSubmodel(): { 
    showDialog: boolean, 
    template: string 
  } {
    return {
      showDialog: true,
      template: this.getMinimalSubmodelTemplate()
    };
  }

  async createSubmodel(sourceSystemId: number, submodelJson: string): Promise<any> {
    try {
      const body = JSON.parse(submodelJson);
      const result = await this.aasService.createSubmodel(sourceSystemId, body).toPromise();
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Submodel created successfully!' });
      return result;
    } catch (error) {
      if (error instanceof SyntaxError) {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Invalid JSON format' });
      } else {
        this.errorService.handleError(error as any);
      }
      throw error;
    }
  }

  /**
   * Element Creation
   */
  openCreateElement(smId: string, parent?: string): {
    showDialog: boolean,
    targetSubmodelId: string,
    parentPath: string,
    template: string
  } {
    return {
      showDialog: true,
      targetSubmodelId: smId,
      parentPath: parent || '',
      template: this.getPropertyElementTemplate()
    };
  }

  onElementJsonFileSelected(event: any): string {
    const file = event.files[0];
    if (file) {
      const reader = new FileReader();
      return new Promise((resolve) => {
        reader.onload = (e) => {
          const text = e.target?.result as string;
          resolve(text);
        };
        reader.readAsText(file);
      }) as any;
    }
    return '';
  }

  async createElement(
    sourceSystemId: number, 
    submodelId: string, 
    elementJson: string, 
    parentPath?: string
  ): Promise<any> {
    try {
      const body = JSON.parse(elementJson);
      const smIdB64 = this.aasManagementService.encodeIdToBase64Url(submodelId);
      const result = await this.aasService.createElement(sourceSystemId, smIdB64, body, parentPath).toPromise();
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Element created successfully!' });
      return result;
    } catch (error) {
      if (error instanceof SyntaxError) {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Invalid JSON format' });
      } else {
        this.errorService.handleError(error as any);
      }
      throw error;
    }
  }

  /**
   * Delete Operations
   */
  async deleteSubmodel(sourceSystemId: number, submodelId: string): Promise<any> {
    try {
      const smIdB64 = this.aasManagementService.encodeIdToBase64Url(submodelId);
      const result = await this.aasService.deleteSubmodel(sourceSystemId, smIdB64).toPromise();
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Submodel deleted successfully!' });
      return result;
    } catch (error) {
      this.errorService.handleError(error as any);
      throw error;
    }
  }

  proceedDeleteSubmodel(sourceSystemId: number, submodelId: string): Promise<any> {
    return this.deleteSubmodel(sourceSystemId, submodelId);
  }

  async deleteElement(sourceSystemId: number, submodelId: string, idShortPath: string): Promise<any> {
    try {
      const smIdB64 = this.aasManagementService.encodeIdToBase64Url(submodelId);
      const result = await this.aasService.deleteElement(sourceSystemId, smIdB64, idShortPath).toPromise();
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Element deleted successfully!' });
      return result;
    } catch (error) {
      this.errorService.handleError(error as any);
      throw error;
    }
  }

  /**
   * Value Setting
   */
  openSetValue(smId: string, element: any): {
    showDialog: boolean,
    submodelId: string,
    elementPath: string,
    typeHint: string,
    currentValue: string
  } {
    return {
      showDialog: true,
      submodelId: smId,
      elementPath: element.idShortPath || '',
      typeHint: element.raw?.valueType || 'xs:string',
      currentValue: element.raw?.value?.toString() || ''
    };
  }

  async setValue(
    sourceSystemId: number, 
    submodelId: string, 
    elementPath: string, 
    value: string, 
    typeHint: string
  ): Promise<any> {
    try {
      const parsedValue = this.parseValueForType(value, typeHint);
      const smIdB64 = this.aasManagementService.encodeIdToBase64Url(submodelId);
      const result = await this.aasService.setPropertyValue(sourceSystemId, smIdB64, elementPath, parsedValue).toPromise();
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Value updated successfully!' });
      return result;
    } catch (error) {
      this.errorService.handleError(error as any);
      throw error;
    }
  }

  parseValueForType(raw: string, valueType?: string): any {
    if (!valueType || valueType === 'xs:string') return raw;
    if (valueType === 'xs:int' || valueType === 'xs:integer') return parseInt(raw, 10);
    if (valueType === 'xs:double' || valueType === 'xs:float') return parseFloat(raw);
    if (valueType === 'xs:boolean') return raw.toLowerCase() === 'true';
    return raw;
  }

  /**
   * Template Management
   */
  getMinimalSubmodelTemplate(): string {
    return `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "kind": "Instance"
}`;
  }

  getPropertySubmodelTemplate(): string {
    return `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "submodelElements": [
    {
      "modelType": "Property",
      "idShort": "Name",
      "valueType": "xs:string",
      "value": "Foo"
    }
  ]
}`;
  }

  getCollectionSubmodelTemplate(): string {
    return `{
  "id": "https://example.com/ids/sm/new",
  "idShort": "NewSubmodel",
  "submodelElements": [
    {
      "modelType": "SubmodelElementCollection",
      "idShort": "Collection",
      "value": [
        {
          "modelType": "Property",
          "idShort": "Prop1",
          "valueType": "xs:string",
          "value": "Val1"
        }
      ]
    }
  ]
}`;
  }

  getPropertyElementTemplate(): string {
    return `{
  "modelType": "Property",
  "idShort": "NewProp",
  "valueType": "xs:string",
  "value": "42"
}`;
  }

  getRangeElementTemplate(): string {
    return `{
  "modelType": "Range",
  "idShort": "NewRange",
  "valueType": "xs:int",
  "min": 0,
  "max": 100
}`;
  }

  getMLPElementTemplate(): string {
    return `{
  "modelType": "MultiLanguageProperty",
  "idShort": "NewMLP",
  "value": [{"language": "en", "text": "Hello"}]
}`;
  }

  getReferenceElementTemplate(): string {
    return `{
  "modelType": "ReferenceElement",
  "idShort": "NewRef",
  "value": {
    "type": "ModelReference",
    "keys": [{"type": "Submodel", "value": "https://example.com/submodel/ref"}]
  }
}`;
  }

  getRelationshipElementTemplate(): string {
    return `{
  "modelType": "RelationshipElement",
  "idShort": "NewRel",
  "first": {"type": "ModelReference", "keys": []},
  "second": {"type": "ModelReference", "keys": []}
}`;
  }

  getAnnotatedRelationshipElementTemplate(): string {
    return `{
  "modelType": "AnnotatedRelationshipElement",
  "idShort": "NewAnnRel",
  "first": {"type": "ModelReference", "keys": []},
  "second": {"type": "ModelReference", "keys": []},
  "annotations": []
}`;
  }

  getCollectionElementTemplate(): string {
    return `{
  "modelType": "SubmodelElementCollection",
  "idShort": "NewColl",
  "value": []
}`;
  }

  getListElementTemplate(): string {
    return `{
  "modelType": "SubmodelElementList",
  "idShort": "NewList",
  "typeValueListElement": "Property",
  "value": []
}`;
  }

  getFileElementTemplate(): string {
    return `{
  "modelType": "File",
  "idShort": "NewFile",
  "contentType": "application/pdf",
  "value": "/path/to/file.pdf"
}`;
  }

  getOperationElementTemplate(): string {
    return `{
  "modelType": "Operation",
  "idShort": "NewOp",
  "inputVariables": [],
  "outputVariables": []
}`;
  }

  getEntityElementTemplate(): string {
    return `{
  "modelType": "Entity",
  "idShort": "NewEnt",
  "entityType": "SelfManagedEntity",
  "statements": []
}`;
  }

  setSubmodelTemplate(kind: 'minimal'|'property'|'collection'): string {
    switch (kind) {
      case 'minimal': return this.getMinimalSubmodelTemplate();
      case 'property': return this.getPropertySubmodelTemplate();
      case 'collection': return this.getCollectionSubmodelTemplate();
      default: return this.getMinimalSubmodelTemplate();
    }
  }

  setElementTemplate(kind: string): string {
    switch (kind) {
      case 'property': return this.getPropertyElementTemplate();
      case 'range': return this.getRangeElementTemplate();
      case 'mlp': return this.getMLPElementTemplate();
      case 'ref': return this.getReferenceElementTemplate();
      case 'rel': return this.getRelationshipElementTemplate();
      case 'annrel': return this.getAnnotatedRelationshipElementTemplate();
      case 'collection': return this.getCollectionElementTemplate();
      case 'list': return this.getListElementTemplate();
      case 'file': return this.getFileElementTemplate();
      case 'operation': return this.getOperationElementTemplate();
      case 'entity': return this.getEntityElementTemplate();
      default: return this.getPropertyElementTemplate();
    }
  }
}
