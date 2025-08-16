import { Component, OnInit } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { FormsModule }       from '@angular/forms';

// PrimeNG-Komponenten/Directives standalone
import { TableModule }       from 'primeng/table';          // TableModule bleibt
import { DialogModule }      from 'primeng/dialog';         // DialogModule bleibt
import { ButtonModule }      from 'primeng/button';         // ButtonModule bleibt
import { InputTextModule }   from 'primeng/inputtext';      // InputTextModule bleibt
import { InputTextarea }     from 'primeng/inputtextarea';  // ← hier die Komponente
import { ToastModule }       from 'primeng/toast';          // ToastModule bleibt
import { ConfirmDialogModule } from 'primeng/confirmdialog'; // ConfirmDialogModule bleibt
import { TabView }           from 'primeng/tabview';        // ← TabView-Komponente
import { TabPanel }          from 'primeng/tabview';        // ← TabPanel-Komponente
import { MessageService, ConfirmationService } from 'primeng/api';

import { Asset }        from './models/asset.model';
import { AssetService } from './services/asset.service';

@Component({
  selector: 'app-edc-assets',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    InputTextarea,        // ← pInputTextarea
    ToastModule,
    ConfirmDialogModule,
    TabView,              // ← p-tabView
    TabPanel              // ← p-tabPanel
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './edc-assets.component.html',
  styleUrls: ['./edc-assets.component.css']
})

export class EdcAssetsComponent implements OnInit {
  assets: Asset[] = [];
  newAsset: Partial<Asset> = {};
  displayNewAssetDialog = false;
  isEdit = false;

  // TabState für Formular vs JSON
  activeTab = 0;
  jsonInput = '';
  jsonError: string | null = null;

  constructor(
    private assetService: AssetService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.loadAssets();
  }

  private loadAssets(): void {
    this.assetService.getAssets().subscribe({
      next: assets => (this.assets = assets),
      error: () =>
        this.messageService.add({
          severity: 'error',
          summary: 'Fehler',
          detail: 'Assets konnten nicht geladen werden'
        })
    });
  }

  openNewAssetDialog(): void {
    this.newAsset = {};
    this.isEdit = false;
    this.jsonInput = '';
    this.jsonError = null;
    this.displayNewAssetDialog = true;
    this.activeTab = 0;
  }

  editAsset(asset: Asset): void {
    this.newAsset = { ...asset };
    this.isEdit = true;
    this.jsonInput = JSON.stringify(asset, null, 2);
    this.jsonError = null;
    this.displayNewAssetDialog = true;
    this.activeTab = 0;
  }

  saveByTab(): void {
    this.activeTab === 0 ? this.saveFromForm() : this.saveFromJson();
  }

  private saveFromForm(): void {
    if (!this.newAsset.assetId || !this.newAsset.targetEDCId) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Validierung',
        detail: 'Asset ID und Ziel-EDC (targetEDCId) müssen gesetzt sein.'
      });
      return;
    }
    const op = this.isEdit
      ? this.assetService.updateAsset(this.newAsset.id!, this.newAsset as Asset)
      : this.assetService.createAsset(this.newAsset as Asset);

    op.subscribe({
      next: (asset: Asset) => {
        if (this.isEdit) {
          const idx = this.assets.findIndex(a => a.id === asset.id);
          if (idx > -1) this.assets[idx] = asset;
        } else {
          this.assets.push(asset);
        }
        this.messageService.add({
          severity: 'success',
          summary: this.isEdit ? 'Aktualisiert' : 'Erstellt',
          detail: `Asset "${asset.assetId}" wurde ${
            this.isEdit ? 'aktualisiert' : 'erstellt'
          }.`
        });
        this.cancel();
      },
      error: err =>
        this.messageService.add({
          severity: 'error',
          summary: 'Fehler',
          detail: err.error?.message || 'Speichern fehlgeschlagen'
        })
    });
  }

  private saveFromJson(): void {
    this.jsonError = null;
    let parsed: any;
    try {
      parsed = JSON.parse(this.jsonInput);
    } catch (e) {
      this.jsonError = 'Ungültiges JSON: ' + (e as Error).message;
      return;
    }
    // Pflichtfelder prüfen
    for (const field of ['assetId', 'url', 'type', 'contentType', 'targetEDCId']) {
      if (!parsed[field]) {
        this.jsonError = `Feld "${field}" fehlt.`;
        return;
      }
    }
    this.assetService.createAsset(parsed).subscribe({
      next: (asset: Asset) => {
        this.assets.push(asset);
        this.messageService.add({
          severity: 'success',
          summary: 'Erstellt',
          detail: `Asset "${asset.assetId}" angelegt.`
        });
        this.cancel();
      },
      error: err =>
        this.messageService.add({
          severity: 'error',
          summary: 'Fehler',
          detail: err.error?.message || 'Speichern fehlgeschlagen'
        })
    });
  }

  confirmDelete(asset: Asset): void {
    this.confirmationService.confirm({
      message: `Soll Asset "${asset.assetId}" wirklich gelöscht werden?`,
      accept: () => this.deleteAsset(asset)
    });
  }

  private deleteAsset(asset: Asset): void {
    this.assetService.deleteAsset(asset.id!).subscribe({
      next: () => {
        this.assets = this.assets.filter(a => a.id !== asset.id);
        this.messageService.add({
          severity: 'success',
          summary: 'Gelöscht',
          detail: `Asset "${asset.assetId}" wurde gelöscht.`
        });
      },
      error: () =>
        this.messageService.add({
          severity: 'error',
          summary: 'Fehler',
          detail: 'Löschen fehlgeschlagen'
        })
    });
  }

  cancel(): void {
    this.newAsset = {};
    this.jsonInput = '';
    this.jsonError = null;
    this.displayNewAssetDialog = false;
  }
}
