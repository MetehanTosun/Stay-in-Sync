import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

// PrimeNG
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ToolbarModule } from 'primeng/toolbar';
import { MessageModule } from 'primeng/message';
import { CardModule } from 'primeng/card';

// Create-Dialog-Komponente
import { CreateSourceSystemComponent } from '../create-source-system/create-source-system.component';

// Service und DTOs aus dem `generated`-Ordner
import { SourceSystemResourceService } from '../../../../generated/api/sourceSystemResource.service';
import { SourceSystemDTO } from '../../../../generated/model/sourceSystemDTO';
import { SourceSystem } from '../../../../generated';

@Component({
  standalone: true,
  selector: 'app-source-system-base',
  templateUrl: './source-system-base.component.html',
  styleUrls: ['./source-system-base.component.css'],
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    DialogModule,
    ToolbarModule,
    MessageModule,
    CardModule,
    CreateSourceSystemComponent
  ]
})
export class SourceSystemBaseComponent implements OnInit {
  systems: SourceSystemDTO[] = [];
  loading = false;
  errorMsg?: string;

  /** Steuerung, ob der Create-Dialog angezeigt wird */
  showCreateDialog = false;

  /** Temporarily holds a system for editing */
  selectedSystem: SourceSystemDTO | null = null;

  constructor(private api: SourceSystemResourceService) {}

  ngOnInit(): void {
    this.loadSystems();
  }

  /** Lade alle Quellsysteme vom Backend */
  loadSystems(): void {
    this.loading = true;
    this.api.apiConfigSourceSystemGet().subscribe({
      next: (list: SourceSystem[]) => {
        // Transformiere die Daten, um sie mit SourceSystemDTO kompatibel zu machen
        this.systems = list.map(system => ({
          id: system.id,
          name: system.name || '', // Fallback für undefined
          apiUrl: system.apiUrl || '', // Fallback für undefined
          description: system.description || '', // Fallback für undefined
          apiType: system.apiType || '', // Fallback für undefined
          openApiSpec: undefined // Entferne Blob-Daten, falls nicht benötigt
        } as SourceSystemDTO));
        this.loading = false;
      },
      error: err => {
        console.error('Failed to load source systems', err);
        this.errorMsg = 'Failed to load source systems';
        this.loading = false;
      }
    });
  }
  /** Öffnet das Create-Dialog */
  openCreate(): void {
    this.showCreateDialog = true;
  }

  /**
   * Wird vom <app-create-source-system> emittet, wenn der Dialog geschlossen wird.
   * Lädt dann die Liste neu.
   */
  onCreateDialogClose(visible: boolean): void {
    this.showCreateDialog = visible;
    if (!visible) {
      this.loadSystems();
    }
  }

  /** Löscht ein Quellsystem und lädt die Liste neu */
  deleteSourceSystem(system: SourceSystemDTO): void {
    if (!system.id) {
      console.warn('Keine ID vorhanden, Löschen übersprungen');
      return;
    }
    this.api.apiConfigSourceSystemIdDelete(system.id).subscribe({
      next: () => this.loadSystems(),
      error: err => console.error('Löschen des Source System fehlgeschlagen', err)
    });
  }

  /** Öffnet den Dialog zum Bearbeiten eines bestehenden Quellsystems */
  editSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    this.showCreateDialog = true;
  }
}