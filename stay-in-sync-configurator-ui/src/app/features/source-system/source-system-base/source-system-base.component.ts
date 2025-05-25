// src/app/features/source-system/source-system-base/source-system-base.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
// biome-ignore lint/style/useImportType: <explanation>
import { AasService } from '../../../aas.service';

interface AasInstance {
  id: string;
  idShort: string;
}

@Component({
  selector: 'app-source-system-base',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonModule, TableModule],
  templateUrl: './source-system-base.component.html',
  styleUrls: ['./source-system-base.component.css'],
})
export class SourceSystemBaseComponent implements OnInit {
  availableSystems: AasInstance[] = [];
  loading = true;
  errorMessage = '';

  constructor(private aas: AasService) {}

  ngOnInit(): void {
    this.loadSystems();
  }

  private loadSystems(): void {
    this.loading = true;
    this.errorMessage = '';
    this.aas.getAll().subscribe({
      next: (systems) => {
        // AAS-Service liefert ein Array mit id/idShort
        this.availableSystems = systems.map(s => ({
          id: s.id,
          idShort: s.name || s.idShort || s.id,
        }));
        this.loading = false;
      },
      error: (err) => {
        console.error('Fehler in AasService.getAll():', err);
        this.errorMessage = 'Konnte AAS-Instanzen nicht laden.';
        this.loading = false;
      }
    });
  }
}