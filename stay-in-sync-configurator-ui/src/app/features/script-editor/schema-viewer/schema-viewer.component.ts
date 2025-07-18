import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { TreeNode } from 'primeng/api';
import { TabViewModule } from 'primeng/tabview';
import { TreeModule } from 'primeng/tree';

@Component({
  selector: 'app-schema-viewer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TabViewModule,
    TreeModule,
    MonacoEditorModule,
  ],
  templateUrl: './schema-viewer.component.html',
  styleUrl: './schema-viewer.component.css'
})
export class SchemaViewerComponent implements OnChanges {

  @Input() jsonData: Record<string, any> | null | undefined = null;
  @Input() dtsCode: string | null | undefined = null;

  treeNodes: TreeNode[] = []
  editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false },
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['jsonData'] && this.jsonData) {
      this.treeNodes = [this.convertSchemaToTreeNode(this.jsonData, 'payload', 'Response Payload')];
    } else {
      this.treeNodes = [];
    }
  }

  private convertSchemaToTreeNode(schema: any, key: string, label: string): TreeNode {
    const node: TreeNode = {
      key: key,
      label: label,
      data: schema,
      type: schema.type || 'object',
      expanded: true, // Auto-expand first level for visibility
      icon: this.getPropertyIcon(schema.type),
      children: []
    };

    if (schema.type === 'object' && schema.properties) {
      node.label = label;
      node.children = Object.keys(schema.properties).map(propKey => {
        const propSchema = schema.properties[propKey];
        const isRequired = schema.required?.includes(propKey);
        const propLabel = `${propKey}: ${propSchema.type}${isRequired ? ' (Required)' : ''}`;

        const childNode = this.convertSchemaToTreeNode(propSchema, `${key}-${propKey}`, propLabel);
        childNode.expanded = false; 
        return childNode;
      });
    } else if (schema.type === 'array' && schema.items) {
      node.label = `${label}[]`;
      const itemNode = this.convertSchemaToTreeNode(schema.items, `${key}-items`, `Item: ${schema.items.type}`);
      itemNode.expanded = true;
      node.children = [itemNode];
    }
    
    return node;
  }

  private getPropertyIcon(type: string): string {
    switch (type) {
      case 'string': return 'pi pi-fw pi-code';
      case 'number': return 'pi pi-fw pi-hashtag';
      case 'boolean': return 'pi pi-fw pi-check-circle';
      case 'object': return 'pi pi-fw pi-sitemap';
      case 'array': return 'pi pi-fw pi-list';
      default: return 'pi pi-fw pi-file';
    }
  }
}
