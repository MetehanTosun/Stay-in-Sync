import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';


import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { RippleModule } from 'primeng/ripple';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';


export interface PolicyTemplate {
  id: string;
  name: string;
  description: string;
  templateJson: string;
}

@Component({
  selector: 'app-policy-templates',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputTextarea,
    ToastModule,
    RippleModule,
    IconFieldModule,
    InputIconModule,
    MonacoEditorModule,
  ],
  templateUrl: './policy-templates.component.html',
  styleUrls: ['./policy-templates.component.css'],
  providers: [MessageService],
})
export class PolicyTemplatesComponent implements OnInit {
  templates: PolicyTemplate[] = [];
  loading: boolean = true;

  displayNewTemplateDialog: boolean = false;
  newTemplate: PolicyTemplate | null = null;

  editorOptions = {
    theme: 'vs-dark',
    language: 'json',
    automaticLayout: true,
    minimap: { enabled: false },
  };

  constructor(private messageService: MessageService) {}

  ngOnInit(): void {

    this.templates = [
      {
        id: 'template-001',
        name: 'test',
        description: 'this is a test.',
        templateJson: '{\n  "permission": {\n    "action": "use",\n    "constraint": {\n      "leftOperand": "BusinessPartnerNumber",\n      "operator": "eq",\n      "rightOperand": "${bpn_number}"\n    }\n  }\n}',
      },
    ];
    this.loading = false;
  }

  openNewTemplateDialog() {
    // Provide a helpful default structure for the user
    const defaultTemplate = {
      "permission": {
        "action": "use",
        "constraint": {
          "leftOperand": "BusinessPartnerNumber",
          "operator": "eq",
          "rightOperand": "${bpn_number}"
        }
      }
    };
    this.newTemplate = { id: '', name: '', description: '', templateJson: JSON.stringify(defaultTemplate, null, 2) };
    this.displayNewTemplateDialog = true;
  }

  hideNewTemplateDialog() {
    this.displayNewTemplateDialog = false;
    this.newTemplate = null;
  }

  saveNewTemplate() {
    this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Template saved (mock)' });
    this.hideNewTemplateDialog();
  }
}
