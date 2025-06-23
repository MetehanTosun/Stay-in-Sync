import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
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

interface JsonSchema {[key: string]: any;}
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

export interface SyncJobContextData {
  syncJobId: string;
  syncJobName: string;
  syncJobDescription?: string;
  sourceSystems: SourceSystemInfo[];
  destinationSystem?: any;
// additional metadata to be implemented
}


interface ViewModelDataEntity extends DataEntity {
  treeNodes: TreeNode[];
}
interface ViewModelSourceSystem extends Omit<SourceSystemInfo, 'dataEntities'> {
  dataEntities: ViewModelDataEntity[];
}
export interface SyncJobContextViewModel extends Omit<SyncJobContextData, 'sourceSystems'> {
  sourceSystems: ViewModelSourceSystem[];
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
export class SyncJobContextPanelComponent implements OnChanges{
  @Input() syncJobData: SyncJobContextData | null = null;
  viewModel: SyncJobContextViewModel | null = null;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['syncJobData'] && this.syncJobData) {
      this.buildViewModel(this.syncJobData);
    } else if (changes['syncJobData'] && !this.syncJobData) {
      this.viewModel = null;
    }
  }

  private buildViewModel(data: SyncJobContextData): void {
    this.viewModel = {
      ...data, // Copy top-level properties like syncJobId, syncJobName, etc.
      sourceSystems: data.sourceSystems.map(system => ({
        ...system, // Copy system properties like id, name, type
        dataEntities: system.dataEntities.map(entity => ({
          ...entity, // Copy entity properties like aliasInScript, entityName
          // This is where we attach the pre-built, stable tree node array
          treeNodes: [this.convertSchemaToTreeNode(entity.schemaSummary, entity.aliasInScript, entity.entityName)]
        }))
      }))
    };

    // --- FIX: Manually trigger change detection after building the view model ---
    // This helps ensure the tree is responsive on initial load.
    this.cdr.detectChanges();
  }

  private convertSchemaToTreeNode(schema: any, key: string, label: string): TreeNode {
    // ... this function's implementation remains exactly the same as before ...
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
        // Set children to not be expanded by default to avoid huge trees
        const childNode = this.convertSchemaToTreeNode(propSchema, `${key}-${propKey}`, propLabel);
        childNode.expanded = false; 
        return childNode;
      });
    } else if (schema.type === 'array' && schema.items) {
      node.label = `${label}[]`;
      const itemNode = this.convertSchemaToTreeNode(schema.items, `${key}-items`, `Item: ${schema.items.type}`);
      itemNode.expanded = true; // Expand the 'Item' node
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
