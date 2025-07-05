import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

// PrimeNG
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ToolbarModule } from 'primeng/toolbar';
import { MessageModule } from 'primeng/message';
import { CardModule } from 'primeng/card';

// Dein Service + Models
import { SourceSystemService } from '../../../../services/source-system-api.service';
import { SourceSystemDTO } from '../../../../models/source-system.dto';

// Create-Dialog-Komponente
import { CreateSourceSystemComponent } from '../create-source-system/create-source-system.component';

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

  constructor(private api: SourceSystemService) {}

  ngOnInit(): void {
    this.loadSystems();
  }

  /** Lade alle Quellsysteme vom Backend */
  loadSystems(): void {
    this.loading = true;
    this.api.getAll().subscribe({
      next: list => {
        this.systems = list;
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
}