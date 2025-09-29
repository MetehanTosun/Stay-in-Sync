import { Component, Input, Output, EventEmitter, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';

export interface AasElementDialogData {
  submodelId: string;
  parentPath?: string;
  systemId: number;
  systemType: 'source' | 'target';
}

export interface AasElementDialogResult {
  success: boolean;
  element?: any;
  error?: string;
}

@Component({
  standalone: true,
  selector: 'app-aas-element-dialog',
  templateUrl: './aas-element-dialog.component.html',
  styleUrls: ['./aas-element-dialog.component.css'],
  imports: [
    CommonModule,
    FormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    FileUploadModule,
    MessageModule
  ]
})
export class AasElementDialogComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() data: AasElementDialogData | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() result = new EventEmitter<AasElementDialogResult>();

  // Element creation properties
  elementJson = '';
  parentPath = '';
  submodelId = '';
  systemId = 0;
  systemType: 'source' | 'target' = 'source';

  // Templates
  elementTemplateProperty = `{
  "modelType": "Property",
  "idShort": "NewProp",
  "valueType": "xs:string",
  "value": "Foo"
}`;

  elementTemplateRange = `{
  "modelType": "Range",
  "idShort": "NewRange",
  "valueType": "xs:double",
  "min": 0,
  "max": 100
}`;

  elementTemplateMLP = `{
  "modelType": "MultiLanguageProperty",
  "idShort": "Title",
  "value": [{"language": "en", "text": "Example"}]
}`;

  elementTemplateRef = `{
  "modelType": "ReferenceElement",
  "idShort": "Ref",
  "value": {"type": "ModelReference", "keys": [{"type": "Submodel", "value": "https://example.com/ids/sm"}]}
}`;

  elementTemplateRel = `{
  "modelType": "RelationshipElement",
  "idShort": "Rel",
  "first": {"type": "ModelReference", "keys": [{"type": "Submodel", "value": "https://example.com/ids/sm1"}]},
  "second": {"type": "ModelReference", "keys": [{"type": "Submodel", "value": "https://example.com/ids/sm2"}]}
}`;

  elementTemplateAnnRel = `{
  "modelType": "AnnotatedRelationshipElement",
  "idShort": "AnnRel",
  "first": {"type": "ModelReference", "keys": [{"type": "Submodel", "value": "https://example.com/ids/sm1"}]},
  "second": {"type": "ModelReference", "keys": [{"type": "Submodel", "value": "https://example.com/ids/sm2"}]},
  "annotations": [{"modelType": "Property", "idShort": "note", "valueType": "xs:string", "value": "Hello"}]
}`;

  elementTemplateCollection = `{
  "modelType": "SubmodelElementCollection",
  "idShort": "group",
  "value": []
}`;

  elementTemplateList = `{
  "modelType": "SubmodelElementList",
  "idShort": "items",
  "typeValueListElement": "Property",
  "valueTypeListElement": "xs:string",
  "value": []
}`;

  elementTemplateFile = `{
  "modelType": "File",
  "idShort": "file1",
  "contentType": "text/plain",
  "value": "path-or-url.txt"
}`;

  elementTemplateOperation = `{
  "modelType": "Operation",
  "idShort": "Op",
  "inputVariables": [{"value": {"modelType": "Property", "idShort": "in", "valueType": "xs:string"}}],
  "outputVariables": []
}`;

  elementTemplateEntity = `{
  "modelType": "Entity",
  "idShort": "Ent",
  "entityType": "SelfManagedEntity",
  "statements": []
}`;

  constructor(private messageService: MessageService) {}

  ngOnInit(): void {
    // Initialize with default template
    this.elementJson = this.elementTemplateProperty;
  }

  ngOnChanges(): void {
    if (this.data) {
      this.submodelId = this.data.submodelId;
      this.parentPath = this.data.parentPath || '';
      this.systemId = this.data.systemId;
      this.systemType = this.data.systemType;
    }
  }

  /**
   * Set element template
   */
  setElementTemplate(kind: string): void {
    switch (kind) {
      case 'property': this.elementJson = this.elementTemplateProperty; break;
      case 'range': this.elementJson = this.elementTemplateRange; break;
      case 'mlp': this.elementJson = this.elementTemplateMLP; break;
      case 'ref': this.elementJson = this.elementTemplateRef; break;
      case 'rel': this.elementJson = this.elementTemplateRel; break;
      case 'annrel': this.elementJson = this.elementTemplateAnnRel; break;
      case 'collection': this.elementJson = this.elementTemplateCollection; break;
      case 'list': this.elementJson = this.elementTemplateList; break;
      case 'file': this.elementJson = this.elementTemplateFile; break;
      case 'operation': this.elementJson = this.elementTemplateOperation; break;
      case 'entity': this.elementJson = this.elementTemplateEntity; break;
      default: this.elementJson = '{}';
    }
  }

  /**
   * Handle JSON file selection
   */
  onElementJsonFileSelected(event: any): void {
    const file = event.files?.[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = String(reader.result || '').trim();
        if (text) {
          JSON.parse(text);
          this.elementJson = text;
        }
      } catch {
        // ignore parse error and keep current JSON
      }
    };
    reader.readAsText(file);
  }

  /**
   * Create element
   */
  createElement(): void {
    if (!this.systemId || !this.submodelId) {
      this.result.emit({ success: false, error: 'Missing system ID or submodel ID' });
      return;
    }
    
    try {
      const body = JSON.parse(this.elementJson);
      const effectiveParentPath = this.parentPath && this.parentPath.trim() ? this.parentPath : undefined;
      
      console.log('[AasElementDialog] createElement: Creating element', {
        systemId: this.systemId,
        submodelId: this.submodelId,
        parentPath: effectiveParentPath,
        systemType: this.systemType,
        body: body
      });
      
      // Emit the result for the parent component to handle
      this.result.emit({
        success: true,
        element: {
          systemId: this.systemId,
          submodelId: this.submodelId,
          parentPath: effectiveParentPath,
          systemType: this.systemType,
          body: body
        }
      });
      
      this.closeDialog();
    } catch (e) {
      console.error('[AasElementDialog] createElement: JSON parse error', e);
      this.result.emit({ success: false, error: 'Invalid JSON format' });
    }
  }

  /**
   * Handle element creation result with toast notifications
   */
  handleElementCreationResult(result: AasElementDialogResult): void {
    if (result.success) {
      this.messageService.add({
        severity: 'success',
        summary: 'Element Created',
        detail: 'Element has been successfully created.',
        life: 3000
      });
    } else if (result.error) {
      // Check if it's a duplicate idShort error
      if (result.error.includes('Duplicate entry') || result.error.includes('uk_element_submodel_idshortpath')) {
        this.messageService.add({
          severity: 'error',
          summary: 'Duplicate Element',
          detail: 'An element with this idShort already exists. Please use a different idShort.',
          life: 5000
        });
      } else {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: result.error,
          life: 5000
        });
      }
    }
  }

  /**
   * Close dialog
   */
  closeDialog(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  /**
   * Cancel dialog
   */
  cancel(): void {
    this.closeDialog();
  }
}
