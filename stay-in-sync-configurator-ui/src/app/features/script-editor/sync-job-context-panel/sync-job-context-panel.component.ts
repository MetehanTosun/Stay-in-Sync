import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

// PrimeNG Modules
import { PanelModule } from 'primeng/panel';
import { CardModule } from 'primeng/card';
import { AccordionModule } from 'primeng/accordion';
import { FieldsetModule } from 'primeng/fieldset';
import { TagModule } from 'primeng/tag';
import { TreeModule } from 'primeng/tree';
import { TreeNode } from 'primeng/api';
import { ScrollPanelModule } from 'primeng/scrollpanel';

interface DataEntity {
  aliasInScript: string;
  entityName: string;
  schemaSummary: JsonSchema;
}

interface SourceSystemInfo {
  id: string;
  name: string;
  type: string; // AAS / REST_OPENAPI etc.
  dataEntities: DataEntity[];
}

interface DestinationSystemInfo {
  id: string;
  name: string;
  targetEntity: string;
}

export interface SyncJobContextData {
  syncJobId: string;
  syncJobName: string;
  syncJobDescription?: string;
  sourceSystems: SourceSystemInfo[];
  destinationSystem?: DestinationSystemInfo;
// additional metadata to be implemented
}

interface JsonSchema {
    [key: string]: any;
}


@Component({
  selector: 'app-sync-job-context-panel',
  standalone: true,
  imports: [
    CommonModule, PanelModule, CardModule, AccordionModule, FieldsetModule, TagModule, TreeModule, ScrollPanelModule
  ],
  templateUrl: './sync-job-context-panel.component.html',
  styleUrls: ['./sync-job-context-panel.component.css']
})
export class SyncJobContextPanelComponent implements OnChanges {
  @Input() syncJobData: SyncJobContextData | null = null;

  sourceSystemTreeNodes: { systemName: string, nodes: TreeNode[] }[] = [];

  constructor() { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['syncJobData'] && this.syncJobData) {
      this.buildTreeNodes();
    } else if (changes['syncJobData'] && !this.syncJobData) {
        this.sourceSystemTreeNodes = [];
    }
  }

  private buildTreeNodes(): void {
    this.sourceSystemTreeNodes = [];
    if (!this.syncJobData?.sourceSystems) return;

    this.syncJobData.sourceSystems.forEach(system => {
      const systemRootNode: TreeNode = {
        key: system.id,
        label: system.name,
        data: system,
        type: 'system',
        expanded: true,
        children: system.dataEntities.map(entity => this.convertSchemaToTreeNode(entity.schemaSummary, entity.aliasInScript, entity.entityName))
      };
      this.sourceSystemTreeNodes.push({ systemName: system.name, nodes: [systemRootNode]});
    });
  }

  private convertSchemaToTreeNode(schema: JsonSchema, alias: string, entityName: string): TreeNode {
    const rootLabel = `${alias} (as ${entityName})`;
    const rootNode: TreeNode = {
      key: alias,
      label: rootLabel,
      data: schema,
      type: schema['type'] || 'object',
      expanded: true,
      children: []
    };

    if (schema['type'] === 'object' && schema['properties']) {
      rootNode.children = Object.keys(schema['properties']).map(key => {
        const propSchema = schema['properties'][key];
        const childNode: TreeNode = {
          key: `${alias}-${key}`,
          label: `${key}: ${propSchema.type}`,
          data: propSchema,
          type: 'property',
          icon: this.getPropertyIcon(propSchema.type),
          children: []
        };
        if (propSchema.description) {
            childNode.label += ` (${propSchema.description})`;
        }
        if (schema['required'] && schema['required'].includes(key)) {
            childNode.label += ' (Required)';
        }
        // Recursively build for nested objects/arrays if needed
        if (propSchema.type === 'object' || propSchema.type === 'array' && propSchema.items?.type === 'object') {
            // Simplified for now, full recursion can be complex. To be tested
            // childNode.children = [this.convertSchemaToTreeNode(propSchema.items || propSchema, key, key)];
        }
        return childNode;
      });
    } else if (schema['type'] === 'array' && schema['items']) {
      const itemSchema = schema['items'];
      const itemNodeLabel = `Items: ${itemSchema.type}`;
      const itemNode: TreeNode = {
        key: `${alias}-items`,
        label: itemNodeLabel,
        data: itemSchema,
        type: 'arrayItem',
        icon: this.getPropertyIcon(itemSchema.type),
        expanded: true,
        children: []
      };
      if (itemSchema.type === 'object' && itemSchema.properties) {
        itemNode.children = Object.keys(itemSchema.properties).map(key => {
          const propSchema = itemSchema.properties[key];
          return {
            key: `${alias}-item-${key}`,
            label: `${key}: ${propSchema.type}`,
            data: propSchema,
            type: 'property',
            icon: this.getPropertyIcon(propSchema.type)
          };
        });
      }
      rootNode.children = [itemNode];
    }
    return rootNode;
  }

  findTreeNodesForEntity(systemId: string, entityAlias: string): TreeNode[] {
    const systemNodeContainer = this.sourceSystemTreeNodes.find(s => s.nodes[0]?.key === systemId);
    if (systemNodeContainer && systemNodeContainer.nodes[0]?.children) {
        const entityNode = systemNodeContainer.nodes[0].children.find(child => child.key === entityAlias);
        return entityNode ? [entityNode] : [];
    }
    return [];
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
