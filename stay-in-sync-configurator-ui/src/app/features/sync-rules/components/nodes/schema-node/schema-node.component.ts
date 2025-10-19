import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CustomNodeComponent, HandleComponent, SelectableDirective } from 'ngx-vflow';
import { SchemaPreviewPipe } from './schema-preview.pipe';

/**
 * A node representing a JSON Schema within a vflow graph
 */
@Component({
  selector: 'app-schema-node',
  standalone: true,
  imports: [HandleComponent, SelectableDirective, CommonModule],
  templateUrl: './schema-node.component.html',
  styleUrls: ['./schema-node.component.css']
})
export class SchemaNodeComponent extends CustomNodeComponent {
  displayTooltips = false;

  /**
   * Returns a shortened preview of the stored schema
   */
  getSchemaPreview(): string {
    const schema = String(this.node()?.data?.value || '');
    return new SchemaPreviewPipe().transform(schema);
  }
}
