// src/app/features/source-system/components/source-system-base/source-system-base.component.ts

import { Component, OnInit }            from '@angular/core';
import { CommonModule }                 from '@angular/common';
import { ButtonModule }                 from 'primeng/button';
import { TableModule }                  from 'primeng/table';
import { CreateSourceSystemComponent }  from '../create-source-system/create-source-system.component';
import { AasService }                   from '../../services/aas.service';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner'; 

interface AasInstance { id: string; name: string; }

@Component({
  selector: 'app-source-system-base',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    TableModule,
    CreateSourceSystemComponent,
    ProgressSpinnerModule,
    MessageModule

  ],
  templateUrl: './source-system-base.component.html',
  styleUrls: ['./source-system-base.component.css']
})
export class SourceSystemBaseComponent implements OnInit {
  availableSystems: AasInstance[] = [];
  loading  = true;
  errorMsg = '';
  showCreateDialog = false;

  constructor(private aas: AasService) {}

  ngOnInit() {
    this.loadSystems();
  }

  private loadSystems() {
    this.loading = true;
    this.errorMsg = '';
    this.aas.getAll().subscribe({
      next: list => {
        this.availableSystems = list.map(s => ({ id: s.id, name: s.name }));
        this.loading = false;
      },
      error: err => {
        console.error(err);
        this.errorMsg = 'Laden fehlgeschlagen.';
        this.loading = false;
      }
    });
  }
}