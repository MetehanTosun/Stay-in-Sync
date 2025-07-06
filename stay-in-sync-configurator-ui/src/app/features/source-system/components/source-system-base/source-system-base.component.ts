import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

// PrimeNG
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ToolbarModule } from 'primeng/toolbar';
import { MessageModule } from 'primeng/message';
import { CardModule } from 'primeng/card';
import { TabViewModule } from 'primeng/tabview';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';

// Create-Dialog-Komponente
import { CreateSourceSystemComponent } from '../create-source-system/create-source-system.component';
import { ManageApiHeadersComponent } from '../manage-api-headers/manage-api-headers.component';
import { ManageEndpointsComponent }   from '../manage-endpoints/manage-endpoints.component';

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
    TabViewModule,
    DropdownModule,
    InputTextModule,
    TextareaModule,
    ReactiveFormsModule,
    CreateSourceSystemComponent,
    ManageApiHeadersComponent,
    ManageEndpointsComponent
  ]
})
export class SourceSystemBaseComponent implements OnInit {
  systems: SourceSystemDTO[] = [];
  loading = false;
  errorMsg?: string;

  /** Steuerung, ob der Create-Dialog angezeigt wird */
  showCreateDialog = false;

  /** Steuerung, ob der Detail-Dialog angezeigt wird */
  showDetailDialog = false;

  /** Temporarily holds a system for editing or viewing */
  selectedSystem: SourceSystemDTO | null = null;
  metadataForm!: FormGroup;

  constructor(private api: SourceSystemResourceService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.loadSystems();
    // Initialize metadata form
    this.metadataForm = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: ['']
    });
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
    // Reset selection when creating new
    this.selectedSystem = null;
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
      this.selectedSystem = null;
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

  /**
   * Öffnet den Create/Edit-Wizard im Manage-Modus für ein bestehendes System
   */
  manageSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    // Öffnet den Create-Component als Edit-Dialog
    this.showCreateDialog = true;
  }

  /** Öffnet den Detail-Dialog, um Header und Endpoints eines Systems zu verwalten */
  viewSourceSystem(system: SourceSystemDTO): void {
    this.selectedSystem = system;
    this.showDetailDialog = true;
    // Load metadata into form
    this.metadataForm.patchValue({
      name: system.name,
      apiUrl: system.apiUrl,
      description: system.description
    });
  }

  /** Schließt den Detail-Dialog und lädt bei Bedarf neu */
  closeDetailDialog(): void {
    this.showDetailDialog = false;
    this.selectedSystem = null;
    this.loadSystems();
  }

  /**
   * Speichert die bearbeiteten Metadaten des ausgewählten Systems
   */
  saveMetadata(): void {
    if (!this.selectedSystem || this.metadataForm.invalid) {
      return;
    }
    const updated: SourceSystemDTO = {
      ...this.selectedSystem,
      ...this.metadataForm.value
    };
    this.api
      .apiConfigSourceSystemIdPut(this.selectedSystem.id!, updated)
      .subscribe({
        next: () => {
          // Nach dem Speichern die Liste neu laden und die Ansicht aktualisieren
          this.selectedSystem = updated;
          this.loadSystems();
        },
        error: err => console.error('Failed to save metadata', err)
      });
  }
}
