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

  @Input() jsonData: any | null | undefined = null;
  @Input() dtsCode: string | null | undefined = null;

  treeNodes: TreeNode[] = [];
  isJsonData = true;
  rawTextData = '';
  editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false },
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['jsonData']) {
      if(typeof this.jsonData === 'object' && this.jsonData !== null){
        this.isJsonData = true;
        this.treeNodes = [this.convertPayloadToTreeNode(this.jsonData, 'payload')];
      }
    } else {
      this.isJsonData = false;
      this.rawTextData = String(this.jsonData);
      this.treeNodes = [];
    }
  }

  private convertPayloadToTreeNode(data: any, key: string): TreeNode {
    const dataType = this.getPayloadType(data);

    const node: TreeNode = {
      key: key,
      label: `${key}: ${dataType}`,
      expanded: false,
      icon: this.getPropertyIcon(dataType),
      children: []
    };

    if (dataType === 'object') {
      node.children = Object.keys(data).map((propKey, index) => {
        return this.convertPayloadToTreeNode(data[propKey], propKey);
      });
    } else if (dataType === 'array') {
      node.label = `${key}: array`;
      if(data.length >0){
        const itemNode = this.convertPayloadToTreeNode(data[0], `Item[0]`);
        itemNode.label = itemNode.label?.replace('Item [0]', 'Item');
        node.children = [itemNode];
      }
    }
    
    return node;
  }

  private getPayloadType(data: any): string {
    if(Array.isArray(data)) return 'array';
    if(data === null) return 'null';
    return typeof data;
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
