import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// PrimeNG Modules
import { Table, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { RippleModule } from 'primeng/ripple';
import { ConfirmationService } from 'primeng/api';


// Application-specific imports
import { Asset } from './models/asset.model';
import { AssetService } from './services/asset.service';

@Component({
  selector: 'app-edc-assets-and-policies',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    ConfirmDialogModule,
    InputTextModule,
    InputTextarea,
    TagModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    RippleModule,
  ],
  templateUrl: './edc-assets-and-policies.component.html',
  styleUrls: ['./edc-assets-and-policies.component.css'],
  providers: [ConfirmationService],
})
export class EdcAssetsAndPoliciesComponent implements OnInit {
  @ViewChild('dtAssets') dtAssets: Table | undefined;

  assets: Asset[] = [];
  loading: boolean = true;

  // Dialog properties
  displayNewAssetDialog: boolean = false;
  newAsset: Asset = this.createEmptyAsset();
  displayEditAssetDialog: boolean = false;
  assetToEdit: Asset | null = null;

  constructor(
    private assetService: AssetService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.assetService.getAssets().then((data) => {
      this.assets = data;
      this.loading = false;
    });
  }

  private createEmptyAsset(): Asset {
    return {
      id: '',
      name: '',
      url: '',
      type: 'HttpData', // A sensible default
      description: '',
      contentType: 'application/json' // A sensible default
    };
  }

  onGlobalFilter(event: Event) {
    const inputElement = event.target as HTMLInputElement;
    if (this.dtAssets) {
      this.dtAssets.filterGlobal(inputElement.value, 'contains');
    }
  }

  openNewAssetDialog() {
    this.newAsset = this.createEmptyAsset();
    this.displayNewAssetDialog = true;
  }

  hideNewAssetDialog() {
    this.displayNewAssetDialog = false;
  }

  saveNewAsset() {
    if (this.newAsset.name && this.newAsset.url) {
      this.newAsset.id = 'asset-' + Math.random().toString(36).substring(2, 9);
      this.assets = [...this.assets, this.newAsset];
      this.hideNewAssetDialog();
    }
  }

  // --- Edit Asset Methods ---
  editAsset(asset: Asset) {
    this.assetToEdit = { ...asset };
    this.displayEditAssetDialog = true;
  }

  hideEditAssetDialog() {
    this.displayEditAssetDialog = false;
    this.assetToEdit = null;
  }

  saveEditedAsset() {
    if (this.assetToEdit) {
      const index = this.assets.findIndex(a => a.id === this.assetToEdit!.id);
      if (index !== -1) {
        this.assets[index] = this.assetToEdit;
        this.assets = [...this.assets];
      }
      this.hideEditAssetDialog();
    }
  }

  // --- Delete Asset Method ---
  deleteAsset(asset: Asset) {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the asset "${asset.name}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.assets = this.assets.filter(a => a.id !== asset.id);
      },
    });
  }
}
