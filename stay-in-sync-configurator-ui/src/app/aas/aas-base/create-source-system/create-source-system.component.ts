// biome-ignore lint/style/useImportType: <explanation>
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
// biome-ignore lint/style/useImportType: <explanation>
import { AasService } from '../../../aas.service';

import { TreeModule } from 'primeng/tree';
// biome-ignore lint/style/useImportType: <explanation>
import { TreeNode } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';

@Component({
  selector: 'app-source-system-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    InputTextModule,
    ButtonModule,
    TreeModule,
    SelectModule
  ],
  templateUrl: './create-source-system.component.html',
})
export class CreateSourceSystemComponent implements OnInit {
  aasList: any[] = [];
  selectedAasId = '';
  treeSubmodels: TreeNode[] = [];
  selectedTreeNodes: TreeNode[] = [];

  source = {
    name: '',
    address: '',
    apiKey: ''
  };

  constructor(private aasService: AasService) {}

  ngOnInit() {
    // biome-ignore lint/suspicious/noExplicitAny: <explanation>
    this.aasService.getAllShells().subscribe((data: any) => {
      this.aasList = data;
    });
  }

  onAasChange() {
    if (!this.selectedAasId) {
      this.treeSubmodels = [];
      return;
    }

    const selectedAas = this.aasList.find(
      (aas: any) => aas.identification.id === this.selectedAasId
    );

    if (selectedAas && selectedAas.submodels) {
      this.treeSubmodels = selectedAas.submodels.map((submodel: any) => {
        const key = submodel.keys.find((k: any) => k.type === 'Submodel');
        const id = key?.value || 'Unknown';
        return {
          label: id,
          data: { id },
          expanded: true,
          children: [
            {
              label: `ID: ${id}`,
              icon: 'pi pi-file'
            }
          ]
        };
      });
    } else {
      this.treeSubmodels = [];
    }

    this.selectedTreeNodes = [];
    console.log('TreeSubmodels:', this.treeSubmodels);
  }

  onSubmit() {
    console.log('Formulardaten:', this.source);
    console.log('Ausgewählte AAS ID:', this.selectedAasId);
    console.log('Ausgewählte Submodelle (Tree Selection):', this.selectedTreeNodes);
  }
}
