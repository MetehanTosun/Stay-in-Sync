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
import { DividerModule } from 'primeng/divider';

// Application-specific imports
import { Asset } from './models/asset.model';
import { AccessPolicy, ContractPolicy } from './models/policy.model';
import { AssetService } from './services/asset.service';
import { PolicyService } from './services/policy.service';

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
    DividerModule,
  ],
  templateUrl: './edc-assets-and-policies.component.html',
  styleUrls: ['./edc-assets-and-policies.component.css'],
  providers: [ConfirmationService],
})
export class EdcAssetsAndPoliciesComponent implements OnInit {
  @ViewChild('dtAssets') dtAssets: Table | undefined;
  @ViewChild('dtPolicies') dtPolicies: Table | undefined;

  // Asset properties
  assets: Asset[] = [];
  assetLoading: boolean = true;
  displayNewAssetDialog: boolean = false;
  newAsset: Asset = this.createEmptyAsset();
  displayEditAssetDialog: boolean = false;
  assetToEdit: Asset | null = null;

  // Policy properties
  accessPolicies: AccessPolicy[] = [];
  policyLoading: boolean = true;
  manuallyExpandedRows: Set<string> = new Set<string>();

  //dialog properties for Policies
  displayNewAccessPolicyDialog: boolean = false;
  newAccessPolicy: AccessPolicy = this.createEmptyAccessPolicy();

  displayNewContractPolicyDialog: boolean = false;
  newContractPolicy: ContractPolicy = this.createEmptyContractPolicy();
  targetAccessPolicy: AccessPolicy | null = null; // To know which access policy to add a contract to

  constructor(
    private assetService: AssetService,
    private policyService: PolicyService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.loadAssets();
    this.assetLoading = true;
    this.policyLoading = true;

    Promise.all([
      this.assetService.getAssets(),
      this.policyService.getAccessPolicies()
    ]).then(([assetsData, policiesData]) => {
      this.assets = assetsData;
      this.accessPolicies = policiesData;
    }).catch(error => {
      console.error('Failed to load data', error);
    }).finally(() => {
      this.assetLoading = false;
      this.policyLoading = false;
    });
  }

  //global filter methods
  onGlobalFilter(event: Event, table: Table | undefined) {
    const inputElement = event.target as HTMLInputElement;
    if (table) {
      table.filterGlobal(inputElement.value, 'contains');
    }
  }

  //Asset methods
  private createEmptyAsset(): Asset {
    return { id: '', name: '', url: '', type: 'HttpData', description: '', contentType: 'application/json' };
  }

  openNewAssetDialog() {
    this.newAsset = this.createEmptyAsset();
    this.displayNewAssetDialog = true;
  }

  hideNewAssetDialog() {
    this.displayNewAssetDialog = false;
  }

  async saveNewAsset() {
    if (!this.newAsset.name || !this.newAsset.url) {
      console.error('Asset name and URL are required.');
      return;
    }

    try {
      await this.assetService.createAsset(this.newAsset);
      // Great success! Refresh the list and close the dialog
      this.loadAssets(); // Assuming you have a method to reload assets
      this.hideNewAssetDialog();
    } catch (error) {
      console.error('Failed to create asset:', error);
    }
  }

  /**
   * Handles the selection of a JSON file for upload.
   */
  async onFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (fileList && fileList.length > 0) {
      const file = fileList[0];
      const fileContent = await file.text();

      try {
        const assetJson = JSON.parse(fileContent);
        if (!assetJson['@id'] || !assetJson.properties || !assetJson.dataAddress) {
          throw new Error('Invalid asset JSON format.');
        }

        await this.assetService.uploadAsset(assetJson);

        // Refresh the list and close the dialog
        this.loadAssets();
        this.hideNewAssetDialog();

      } catch (error) {
        console.error('Failed to upload and create asset from file:', error);
      }

      // Reset the file input
      element.value = '';
    }
  }


  private loadAssets(): void {
    this.assetLoading = true;
    this.assetService.getAssets()
      .then(data => this.assets = data)
      .catch(error => console.error('Failed to load assets:', error))
      .finally(() => this.assetLoading = false);
  }


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
      const index = this.assets.findIndex((a) => a.id === this.assetToEdit!.id);
      if (index !== -1) {
        this.assets[index] = this.assetToEdit;
        this.assets = [...this.assets];
      }
      this.hideEditAssetDialog();
    }
  }

  deleteAsset(asset: Asset) {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the asset "${asset.name}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.assets = this.assets.filter((a) => a.id !== asset.id);
      },
    });
  }

  //policy row expansion
  togglePolicyRow(policyId: string): void {
    if (this.manuallyExpandedRows.has(policyId)) {
      this.manuallyExpandedRows.delete(policyId);
    } else {
      this.manuallyExpandedRows.add(policyId);
    }
  }

  isPolicyRowExpanded(policyId: string): boolean {
    return this.manuallyExpandedRows.has(policyId);
  }


  private createEmptyAccessPolicy(): AccessPolicy {
    return { id: '', bpn: '', description: '', contractPolicies: [] };
  }


  openNewAccessPolicyDialog() {
    this.newAccessPolicy = this.createEmptyAccessPolicy();
    this.displayNewAccessPolicyDialog = true;
  }

  hideNewAccessPolicyDialog() {
    this.displayNewAccessPolicyDialog = false;
  }

  saveNewAccessPolicy() {
    if (this.newAccessPolicy.bpn) {
      this.newAccessPolicy.id = 'ap-' + Math.random().toString(36).substring(2, 9);
      this.accessPolicies = [...this.accessPolicies, this.newAccessPolicy];
      this.hideNewAccessPolicyDialog();
    }
  }


  private createEmptyContractPolicy(): ContractPolicy {
    return { id: '', assetId: '' };
  }

  openNewContractPolicyDialog(accessPolicy: AccessPolicy) {
    this.targetAccessPolicy = accessPolicy;
    this.newContractPolicy = this.createEmptyContractPolicy();
    this.displayNewContractPolicyDialog = true;
  }

  hideNewContractPolicyDialog() {
    this.displayNewContractPolicyDialog = false;
    this.targetAccessPolicy = null;
  }

  saveNewContractPolicy() {

    if (this.newContractPolicy.assetId && this.targetAccessPolicy) {
      this.newContractPolicy.id = 'cp-' + Math.random().toString(36).substring(2, 9);
      const policyIndex = this.accessPolicies.findIndex(p => p.id === this.targetAccessPolicy!.id);
      if (policyIndex !== -1) {
        this.accessPolicies[policyIndex].contractPolicies.push(this.newContractPolicy);
        this.accessPolicies = [...this.accessPolicies];
      }
      this.hideNewContractPolicyDialog();
    }
  }
}
