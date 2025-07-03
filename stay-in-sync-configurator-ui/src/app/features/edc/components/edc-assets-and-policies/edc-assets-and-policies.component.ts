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
import { DropdownModule } from 'primeng/dropdown';

// App imports
import { Asset } from './models/asset.model';
import { AccessPolicy, OdrlContractDefinition, ContractPolicy, OdrlPolicyDefinition } from './models/policy.model';
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
    DropdownModule,
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
  displayNewAccessPolicyDialog: boolean = false;
  newAccessPolicy: AccessPolicy = this.createEmptyAccessPolicy();
  displayNewContractPolicyDialog: boolean = false;
  newContractPolicy: ContractPolicy = this.createEmptyContractPolicy();
  targetAccessPolicy: AccessPolicy | null = null;

  operatorOptions: { label: string; value: string; }[];
  actionOptions: { label: string; value: string; }[];

  constructor(
    private assetService: AssetService,
    private policyService: PolicyService,
    private confirmationService: ConfirmationService
  ) {
    // initialize dropdown options
    this.operatorOptions = [
      { label: 'Equals', value: 'eq' },
      { label: 'Not Equals', value: 'neq' },
      { label: 'Is In', value: 'in' },
    ];
    this.actionOptions = [
      { label: 'Use', value: 'use' },
      { label: 'Read', value: 'read' },
      { label: 'Write', value: 'write' },
    ];
  }

  ngOnInit(): void {
    this.loadAssets();
    this.loadPolicies();
  }

  private loadAssets(): void {
    this.assetLoading = true;
    this.assetService
      .getAssets()
      .then((data) => (this.assets = data))
      .catch((error) => console.error('Failed to load assets:', error))
      .finally(() => (this.assetLoading = false));
  }

  private loadPolicies(): void {
    this.policyLoading = true;
    this.policyService
      .getAccessPolicies()
      .then((data) => (this.accessPolicies = data))
      .catch((error) => console.error('Failed to load policies:', error))
      .finally(() => (this.policyLoading = false));
  }

  onGlobalFilter(event: Event, table: Table | undefined) {
    const inputElement = event.target as HTMLInputElement;
    if (table) {
      table.filterGlobal(inputElement.value, 'contains');
    }
  }

  // Asset methods
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
      this.loadAssets();
      this.hideNewAssetDialog();
    } catch (error) {
      console.error('Failed to create asset:', error);
    }
  }

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
        this.loadAssets();
        this.hideNewAssetDialog();
      } catch (error) {
        console.error('Failed to upload and create asset from file:', error);
      }
      element.value = '';
    }
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

  // policy  methods
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

  //access policy methods
  private createEmptyAccessPolicy(): AccessPolicy {
    return {
      id: '',
      bpn: '',
      contractPolicies: [],
      action: 'use', // Default value
      operator: 'eq',  // Default value
    };
  }

  openNewAccessPolicyDialog() {
    this.newAccessPolicy = this.createEmptyAccessPolicy();
    this.displayNewAccessPolicyDialog = true;
  }

  hideNewAccessPolicyDialog() {
    this.displayNewAccessPolicyDialog = false;
  }

  async saveNewAccessPolicy() {
    if (!this.newAccessPolicy.bpn) {
      console.error('BPN is required for manual creation.');
      return;
    }
    try {
      await this.policyService.createAccessPolicy(this.newAccessPolicy);
      this.loadPolicies();
      this.hideNewAccessPolicyDialog();
    } catch (error) {
      console.error('Failed to create access policy:', error);
    }
  }



  /**
   * reads the file, uploads it, and closes the dialog.
   */
  async onPolicyFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList.length > 0) {
      const file = fileList[0];
      const fileContent = await file.text();
      try {
        const policyJson: OdrlPolicyDefinition = JSON.parse(fileContent);
        if (!policyJson['@id'] || !policyJson.policy) {
          throw new Error('Invalid policy definition JSON format.');
        }
        await this.policyService.uploadPolicyDefinition(policyJson);
        this.loadPolicies();
        this.hideNewAccessPolicyDialog(); // Close dialog on success
      } catch (error) {
        console.error('Failed to upload and create policy from file:', error);
      }
      element.value = '';
    }
  }


  //Contract policy methods
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

  async onContractDefFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    // Ensure we have a target access policy context from the expanded row
    if (!this.targetAccessPolicy) {
      console.error('Cannot upload a contract definition without a selected access policy context.');

      return;
    }

    if (fileList && fileList.length > 0) {
      const file = fileList[0];
      const fileContent = await file.text();
      try {
        const contractDefJson: OdrlContractDefinition = JSON.parse(fileContent);

        // Basic validation of the templates structure
        if (!contractDefJson['@id'] || !contractDefJson.assetsSelector) {
          throw new Error('Invalid contract definition JSON format. It must contain at least an @id and an assetsSelector.');
        }

        //We use the access policy the user is currently working with, rather than a potentially incorrect one from the file.
        contractDefJson.accessPolicyId = this.targetAccessPolicy.id;
        contractDefJson.contractPolicyId = this.targetAccessPolicy.id;

        // Call the service to create the definition
        await this.policyService.createContractDefinition(contractDefJson);

        // Update the UI locally for immediate feedback
        const newUiContractPolicy: ContractPolicy = {
          id: contractDefJson['@id'],
          assetId: contractDefJson.assetsSelector[0]?.operandRight || ''
        };
        const policyIndex = this.accessPolicies.findIndex(p => p.id === this.targetAccessPolicy!.id);
        if (policyIndex !== -1) {
          this.accessPolicies[policyIndex].contractPolicies.push(newUiContractPolicy);
          this.accessPolicies = [...this.accessPolicies]; // Trigger change detection
        }

        this.hideNewContractPolicyDialog(); // Close dialog on success
      } catch (error) {
        console.error('Failed to upload and create contract definition from file:', error);
      }
      // Reset the file input to allow selecting the same file again
      element.value = '';
    }
  }

  async saveNewContractPolicy() {
    if (this.newContractPolicy.assetId && this.targetAccessPolicy) {

      const contractDefId = `contract-def-${this.newContractPolicy.assetId}-for-${this.targetAccessPolicy.id}`;

      const payload: OdrlContractDefinition = {
        '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
        '@id': contractDefId,
        accessPolicyId: this.targetAccessPolicy.id,
        contractPolicyId: this.targetAccessPolicy.id, // Per your example, these are the same
        assetsSelector: [
          {
            operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
            operator: '=',
            operandRight: this.newContractPolicy.assetId,
          },
        ],
      };

      try {

        await this.policyService.createContractDefinition(payload);

        const newUiContractPolicy: ContractPolicy = {
          id: contractDefId,
          assetId: this.newContractPolicy.assetId
        };
        const policyIndex = this.accessPolicies.findIndex(p => p.id === this.targetAccessPolicy!.id);
        if (policyIndex !== -1) {
          this.accessPolicies[policyIndex].contractPolicies.push(newUiContractPolicy);
          this.accessPolicies = [...this.accessPolicies]; // Trigger change detection
        }

        this.hideNewContractPolicyDialog();
      } catch (error) {
        console.error('Failed to create contract definition:', error);

      }
    }
  }


}
