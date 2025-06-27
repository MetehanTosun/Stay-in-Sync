import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { MessageModule } from 'primeng/message';
import { SourceSystemApiService } from '../../../../services/source-system-api.service';
import { SourceSystemDto } from '../../../../models/source-system.model';
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
    CardModule,
    MessageModule,
    ToolbarModule,
    CreateSourceSystemComponent
  ]
})
export class SourceSystemBaseComponent implements OnInit {
  availableSystems: { id: number; name: string }[] = [];
  loading = true;
  errorMsg?: string;
  showCreateDialog = false;

  constructor(private api: SourceSystemApiService) {}

  ngOnInit(): void {
    this.loadSystems();
  }

  loadSystems(): void {
    this.loading = true;
    this.api.getAll().subscribe({
      next: (list: SourceSystemDto[]) => {
        this.availableSystems = list.map(s => ({ id: s.id!, name: s.name }));
        this.loading = false;
      },
      error: err => {
        console.error(err);
        this.errorMsg = 'Laden fehlgeschlagen.';
        this.loading = false;
      }
    });
  }

  openCreate(): void {
    this.showCreateDialog = true;
  }

  /**
   * Wird aufgerufen, sobald das CreateComponent auf `visibleChange(false)` feuert.
   * Schließt den Dialog und lädt die Liste neu.
   */
  onCreateDialogClose(visible: boolean): void {
    this.showCreateDialog = visible;
    if (!visible) {
      this.loadSystems();
    }
  }
}