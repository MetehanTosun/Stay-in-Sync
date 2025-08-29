import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Table, TableModule, TableRowSelectEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { TooltipModule } from 'primeng/tooltip';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { Template, TemplateType } from '../../models/template.model';
import { TemplateService } from '../../services/template.service';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-templates',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputTextarea,
    TooltipModule,
    RippleModule,
    ConfirmDialogModule,
    ToastModule,
    MonacoEditorModule,
    DropdownModule,
    TagModule,
  ],
  templateUrl: './templates.component.html',
  styleUrls: ['./templates.component.css'],
  providers: [ConfirmationService, MessageService],
})
export class TemplatesComponent implements OnInit {
  @ViewChild('dt') dt!: Table;

  templates: Template[] = [];
  loading = true;

  // Dialogs
  displayEditDialog = false;
  displayViewDialog = false;

  templateToEdit: Template | null = null;
  templateToView: Template | null = null;
  isNewTemplate = false;

  // Editor
  editorOptions = { theme: 'vs-dark', language: 'json', automaticLayout: true, minimap: { enabled: false } };
  readOnlyEditorOptions = { ...this.editorOptions, readOnly: true };
  templateJsonContent = '';

  // Template Types
  templateTypes: { label: string; value: TemplateType }[] = [
    { label: 'Access Policy', value: 'AccessPolicy' },
    { label: 'Asset', value: 'Asset' },
    { label: 'Contract Definition', value: 'ContractDefinition' },
  ];

  constructor(
    private templateService: TemplateService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.templateService.getTemplates().subscribe(templates => {
      this.templates = templates;
      this.loading = false;
    });
  }

  openNewTemplateDialog(): void {
    this.isNewTemplate = true;
    this.templateToEdit = { id: '', name: '', description: '', type: 'AccessPolicy', content: {} };
    this.templateJsonContent = JSON.stringify({}, null, 2);
    this.displayEditDialog = true;
  }

  editTemplate(template: Template): void {
    this.isNewTemplate = false;
    this.templateToEdit = { ...template };
    this.templateJsonContent = JSON.stringify(template.content, null, 2);
    this.displayEditDialog = true;
  }

  viewTemplate(event: TableRowSelectEvent): void {
    this.templateToView = event.data;
    this.displayViewDialog = true;
  }

  hideEditDialog(): void {
    this.displayEditDialog = false;
    this.templateToEdit = null;
  }

  hideViewDialog(): void {
    this.displayViewDialog = false;
    this.templateToView = null;
  }

  saveTemplate(): void {
    if (!this.templateToEdit || !this.templateToEdit.name) {
      this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'Template name is required.' });
      return;
    }

    try {
      this.templateToEdit.content = JSON.parse(this.templateJsonContent);
    } catch (e) {
      this.messageService.add({ severity: 'error', summary: 'Invalid JSON', detail: 'The template content is not valid JSON.' });
      return;
    }

    const allTemplates = [...this.templates];
    if (this.isNewTemplate) {
      this.templateToEdit.id = `template-${this.generateUuid()}`;
      allTemplates.push(this.templateToEdit);
    } else {
      const index = allTemplates.findIndex(t => t.id === this.templateToEdit!.id);
      if (index > -1) {
        allTemplates[index] = this.templateToEdit;
      }
    }

    this.templateService.saveTemplates(allTemplates).subscribe(() => {
      this.templates = allTemplates;
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Template saved successfully.' });
      this.hideEditDialog();
    });
  }

  deleteTemplate(template: Template): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the template "${template.name}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        const updatedTemplates = this.templates.filter(t => t.id !== template.id);
        this.templateService.saveTemplates(updatedTemplates).subscribe(() => {
          this.templates = updatedTemplates;
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Template deleted successfully.' });
        });
      },
    });
  }

  async onTemplateJsonUpload(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const file = fileList[0]; // Only one file
    try {
      this.templateJsonContent = await file.text();
      this.messageService.add({ severity: 'info', summary: 'Content Loaded', detail: `JSON from ${file.name} loaded into editor.` });
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Read Error', detail: 'Could not read the selected file.' });
    } finally {
      element.value = ''; // Reset file input
    }
  }

  async onFastImport(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (!fileList || fileList.length === 0) return;

    let importedCount = 0;
    const allTemplates = [...this.templates];

    for (const file of Array.from(fileList)) {
      try {
        const fileContent = await file.text();
        const template = JSON.parse(fileContent) as Template;

        // Basic validation
        if (template.name && template.type && template.content) {
          template.id = `template-${this.generateUuid()}`; // Assign new ID
          allTemplates.push(template);
          importedCount++;
        }
      } catch (e) {
        console.error(`Could not import file ${file.name}`, e);
      }
    }

    if (importedCount > 0) {
      this.templateService.saveTemplates(allTemplates).subscribe(() => {
        this.templates = allTemplates;
        this.messageService.add({ severity: 'success', summary: 'Import Complete', detail: `${importedCount} template(s) imported.` });
      });
    } else {
      this.messageService.add({ severity: 'warn', summary: 'Import Failed', detail: 'No valid templates found in selected files.' });
    }

    element.value = ''; // Reset file input
  }

  private generateUuid(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  getSeverityForType(type: TemplateType) {
    switch (type) {
      case 'AccessPolicy': return 'info';
      case 'Asset': return 'success';
      case 'ContractDefinition': return 'warning';
      default: return 'secondary';
    }
  }
}
