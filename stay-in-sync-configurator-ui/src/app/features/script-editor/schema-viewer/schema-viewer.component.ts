import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { TreeNode } from 'primeng/api';
import { TabViewModule } from 'primeng/tabview';
import { TreeModule } from 'primeng/tree';

/**
 * @description
 * A component designed to visualize data in multiple formats. It can display:
 * 1. A structured JSON object as an expandable PrimeNG Tree.
 * 2. Any non-object data as raw text.
 * 3. A TypeScript declaration file (`.d.ts`) in a read-only Monaco editor.
 * It dynamically switches between the tree view and raw text view based on the type of `jsonData` provided.
 */
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
  styleUrl: './schema-viewer.component.css',
})
export class SchemaViewerComponent implements OnChanges {
  /**
   * @description The data payload to be displayed. Can be any JavaScript type.
   * If it's a non-null object, it will be rendered as a tree. Otherwise, it's shown as raw text.
   */
  @Input() jsonData: any | null | undefined = null;

  /**
   * @description A string containing TypeScript type definitions (`.d.ts`) to be displayed
   * in a read-only code editor tab.
   */
  @Input() dtsCode: string | null | undefined = null;

  /**
   * @description The data structure for the PrimeNG Tree component, generated from `jsonData`.
   */
  treeNodes: TreeNode[] = [];

  /**
   * @description A flag to control which view (tree or raw text) is displayed for the payload.
   */
  isJsonData = true;

  /**
   * @description Stores the string representation of `jsonData` when it's not a structured object.
   */
  rawTextData = '';

  /**
   * @description Configuration options for the read-only Monaco editor instance.
   */
  readonly editorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false },
  };

  /**
   * @description Angular lifecycle hook that responds to changes in data-bound input properties.
   * It processes the `jsonData` input to determine how it should be displayed.
   * @param {SimpleChanges} changes - An object representing the changes to the input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['jsonData']) {
      if (typeof this.jsonData === 'object' && this.jsonData !== null) {
        this.isJsonData = true;
        this.treeNodes = [
          this.convertPayloadToTreeNode(this.jsonData, 'payload'),
        ];
      }
    } else {
      this.isJsonData = false;
      this.rawTextData = String(this.jsonData);
      this.treeNodes = [];
    }
  }

  /**
   * @private
   * @description Recursively traverses a data structure (object or array) and converts it into a
   * `TreeNode` structure suitable for the PrimeNG Tree component.
   * @param {any} data - The piece of data (object, array, or primitive) to convert into a node.
   * @param {string} key - The key or property name for the current piece of data.
   * @returns {TreeNode} The generated tree node for the given data.
   */
  private convertPayloadToTreeNode(data: any, key: string): TreeNode {
    const dataType = this.getPayloadType(data);

    const node: TreeNode = {
      key: key,
      label: `${key}: ${dataType}`,
      expanded: false,
      icon: this.getPropertyIcon(dataType),
      children: [],
    };

    if (dataType === 'object') {
      node.children = Object.keys(data).map((propKey, index) => {
        return this.convertPayloadToTreeNode(data[propKey], propKey);
      });
    } else if (dataType === 'array') {
      node.label = `${key}: array`;
      if (data.length > 0) {
        const firstItemRepresentative = 'Item [0]';
        const regularItem = 'Item';
        const itemNode = this.convertPayloadToTreeNode(
          data[0],
          firstItemRepresentative
        );
        itemNode.label = itemNode.label?.replace(
          firstItemRepresentative,
          regularItem
        );
        node.children = [itemNode];
      }
    }

    return node;
  }

  /**
   * @private
   * @description A helper function to determine the JavaScript type of a value, with special
   * handling for `null` and `array` types.
   * @param {any} data - The value to inspect.
   * @returns {string} The string name of the type (e.g., 'string', 'number', 'array', 'null').
   */
  private getPayloadType(data: any): string {
    if (Array.isArray(data)) return 'array';
    if (data === null) return 'null';
    return typeof data;
  }

  /**
   * @private
   * @description A helper function that returns a PrimeIcons CSS class string based on the
   * data type, for providing visual cues in the tree view.
   * @param {string} type - The string name of the data type.
   * @returns {string} The corresponding PrimeIcons icon class.
   */
  private getPropertyIcon(type: string): string {
    switch (type) {
      case 'string':
        return 'pi pi-fw pi-code';
      case 'number':
        return 'pi pi-fw pi-hashtag';
      case 'boolean':
        return 'pi pi-fw pi-check-circle';
      case 'object':
        return 'pi pi-fw pi-sitemap';
      case 'array':
        return 'pi pi-fw pi-list';
      default:
        return 'pi pi-fw pi-file';
    }
  }
}
