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
import { TemplateService } from './services/template.service';
import { TagModule } from 'primeng/tag';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { Template } from './models/template.model';

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
    TagModule,
    IconFieldModule,
    InputIconModule,
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
    this.templateService.getTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading templates:', error);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load templates.' });
        this.loading = false;
      }
    });
  }

  onGlobalFilter(event: Event) {
    const inputElement = event.target as HTMLInputElement;
    if (this.dt) {
      this.dt.filterGlobal(inputElement.value, 'contains');
    }
  }

  openNewTemplateDialog(): void {
    this.isNewTemplate = true;
    this.templateToEdit = { id: '', name: '', description: '', content: {} };
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

    if (this.isNewTemplate) {
      // For new templates, don't set ID - let the backend generate it
      this.templateToEdit.id = '';
      this.templateService.createTemplate(this.templateToEdit).subscribe({
        next: (createdTemplate) => {
          this.templates.push(createdTemplate);
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Template created successfully.' });
          this.hideEditDialog();
        },
        error: (error) => {
          console.error('Error creating template:', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to create template.' });
        }
      });
    } else {
      // For existing templates, update using the ID
      this.templateService.updateTemplate(this.templateToEdit).subscribe({
        next: (updatedTemplate) => {
          const index = this.templates.findIndex(t => t.id === updatedTemplate.id);
          if (index > -1) {
            this.templates[index] = updatedTemplate;
          }
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Template updated successfully.' });
          this.hideEditDialog();
        },
        error: (error) => {
          console.error('Error updating template:', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update template.' });
        }
      });
    }
  }

  deleteTemplate(template: Template): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the template "${template.name}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.templateService.deleteTemplate(template.id).subscribe({
          next: () => {
            this.templates = this.templates.filter(t => t.id !== template.id);
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Template deleted successfully.' });
          },
          error: (error) => {
            console.error('Error deleting template:', error);
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete template.' });
          }
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

    let importPromises: Promise<void>[] = [];

    for (const file of Array.from(fileList)) {
      const importPromise = new Promise<void>(async (resolve, reject) => {
        try {
          const fileContent = await file.text();
          const template = JSON.parse(fileContent) as Template;

          // Basic validation
          if (template.name && template.content) {
            // Don't set ID - let the backend generate it
            template.id = '';
            
            // Create the template using the service
            this.templateService.createTemplate(template).subscribe({
              next: (createdTemplate) => {
                this.templates.push(createdTemplate);
                this.messageService.add({ 
                  severity: 'success', 
                  summary: 'Template Imported', 
                  detail: `Successfully imported ${template.name}` 
                });
                resolve();
              },
              error: (error) => {
                console.error(`Error importing template ${template.name}:`, error);
                this.messageService.add({ 
                  severity: 'error', 
                  summary: 'Import Error', 
                  detail: `Failed to import ${template.name}` 
                });
                reject(error);
              }
            });
          } else {
            this.messageService.add({ 
              severity: 'warn', 
              summary: 'Invalid Template', 
              detail: `File ${file.name} does not contain a valid template structure` 
            });
            resolve();
          }
        } catch (e) {
          console.error(`Could not import file ${file.name}`, e);
          this.messageService.add({ 
            severity: 'error', 
            summary: 'Parse Error', 
            detail: `Could not parse ${file.name} as JSON` 
          });
          resolve(); // Resolve anyway to continue with other files
        }
      });
      
      importPromises.push(importPromise);
    }

    // Wait for all imports to complete
    try {
      await Promise.all(importPromises);
    } catch (error) {
      console.error('Some template imports failed', error);
    }

    element.value = ''; // Reset file input
  }  private generateUuid(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }


}
