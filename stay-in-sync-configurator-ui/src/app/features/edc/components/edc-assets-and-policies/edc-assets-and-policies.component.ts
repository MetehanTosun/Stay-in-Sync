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
import {ConfirmationService, MessageService} from 'primeng/api';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { ToastModule } from 'primeng/toast';

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
    ToastModule,
  ],
  templateUrl: './edc-assets-and-policies.component.html',
  styleUrls: ['./edc-assets-and-policies.component.css'],
  providers: [ConfirmationService, MessageService],
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
    private confirmationService: ConfirmationService,
    private messageService: MessageService

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
      .catch((error) => {
        console.error('Failed to load assets:', error);
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Could not load assets.'});
      })
      .finally(() => (this.assetLoading = false));
  }

  private loadPolicies(): void {
    this.policyLoading = true;
    this.policyService
      .getAccessPolicies()
      .then((data) => (this.accessPolicies = data))
      .catch((error) => {
        console.error('Failed to load policies:', error);
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Could not load policies.'});
      })
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
    // Initialize with empty strings to allow for placeholder text
    return { id: '', name: '', url: '', type: '', description: '', contentType: '' };
  }

  openNewAssetDialog() {
    this.newAsset = this.createEmptyAsset();
    this.displayNewAssetDialog = true;
  }

  hideNewAssetDialog() {
    this.displayNewAssetDialog = false;
  }

  async saveNewAsset() {

    if (
      !this.newAsset.name ||
      !this.newAsset.url ||
      !this.newAsset.type ||
      !this.newAsset.contentType
    ) {

      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'Name, URL, Type, and Content Type are required.',
        life: 4000
      });
      return;
    }

    try {

      await this.assetService.createAsset(this.newAsset);
      this.messageService.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Asset created successfully.',
      });
      this.loadAssets();
      this.hideNewAssetDialog();
    } catch (error) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Failed to create asset.',
      });
      console.error('Failed to create asset:', error);
    }
  }

  /**
   * Handles asset file uploads with validation.
   */
  async onFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const uploadPromises: Promise<void>[] = [];
    const successfulUploads: string[] = [];
    const failedUploads: { name: string; reason: string }[] = [];

    // Loop through all selected files and create a processing promise for each
    for (const file of Array.from(fileList)) {
      const promise = (async () => {
        try {
          const fileContent = await file.text();
          const assetJson = JSON.parse(fileContent);


          if (!assetJson['@id']) {
            throw new Error("Validation failed: The '@id' field is missing or empty.");
          }
          if (!assetJson['@context'] || !assetJson['@context'].edc) {
            throw new Error("Validation failed: The '@context' with a non-empty 'edc' property is required.");
          }
          if (!assetJson.properties) {
            throw new Error("Validation failed: The 'properties' object is missing.");
          }
          if (!assetJson.dataAddress) {
            throw new Error("Validation failed: The 'dataAddress' object is missing.");
          }

          const props = assetJson.properties;
          const dataAddr = assetJson.dataAddress;

          // These fields must exist and not be empty
          if (!props['asset:prop:name']) {
            throw new Error("Validation failed: Missing 'asset:prop:name' in properties.");
          }
          if (!props['asset:prop:contenttype']) {
            throw new Error("Validation failed: Missing 'asset:prop:contenttype' in properties.");
          }
          if (!dataAddr.baseUrl) {
            throw new Error("Validation failed: Missing 'baseUrl' in dataAddress.");
          }
          if (!dataAddr.type) {
            throw new Error("Validation failed: Missing 'type' in dataAddress.");
          }

          // description must exist, but can be empty
          if (!('asset:prop:description' in props)) {
            throw new Error("Validation failed: Missing 'asset:prop:description' in properties.");
          }

          await this.assetService.uploadAsset(assetJson);
          successfulUploads.push(file.name);
        } catch (error: any) {
          failedUploads.push({ name: file.name, reason: error.message || 'Could not process file.' });
        }
      })();
      uploadPromises.push(promise);
    }

    // Wait for all files to be processed
    await Promise.all(uploadPromises);

    // Provide a summary of the results to the user
    if (successfulUploads.length > 0) {
      this.messageService.add({
        severity: 'success',
        summary: 'Upload Complete',
        detail: `${successfulUploads.length} asset(s) uploaded successfully.`,
      });
    }

    if (failedUploads.length > 0) {
      this.messageService.add({
        severity: 'error',
        summary: `${failedUploads.length} Upload(s) Failed`,
        detail: 'See browser console for details on failed files.',
        life: 5000,
      });
      console.error('Failed uploads:', failedUploads);
    }

    // If at least one upload was successful, refresh the list and close the dialog
    if (successfulUploads.length > 0) {
      this.loadAssets();
      this.hideNewAssetDialog();
    }

    // Always reset the file input to allow selecting the same files again
    element.value = '';
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
      this.messageService.add({severity: 'warn', summary: 'Validation Error', detail: 'BPN is required for manual creation.'});
      return;
    }
    try {
      await this.policyService.createAccessPolicy(this.newAccessPolicy);
      this.messageService.add({severity: 'success', summary: 'Success', detail: 'Access Policy created successfully.'});
      this.loadPolicies();
      this.hideNewAccessPolicyDialog();
    } catch (error) {
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to create access policy.'});
      console.error('Failed to create access policy:', error);
    }
  }



  /**
   * reads the file, uploads it, and closes the dialog.
   */
  async onPolicyFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const uploadPromises: Promise<void>[] = [];
    const successfulUploads: string[] = [];
    const failedUploads: { name: string; reason: string }[] = [];

    // Loop through all selected files and create a processing promise for each
    for (const file of Array.from(fileList)) {
      const promise = (async () => {
        try {
          const fileContent = await file.text();
          const policyJson: OdrlPolicyDefinition = JSON.parse(fileContent);

          // Validate the structure
          if (!policyJson['@id'] || !policyJson.policy) {
            throw new Error('Invalid format: Missing required EDC properties.');
          }

          await this.policyService.uploadPolicyDefinition(policyJson);
          successfulUploads.push(file.name);
        } catch (error: any) {
          failedUploads.push({ name: file.name, reason: error.message || 'Could not process file.' });
        }
      })();
      uploadPromises.push(promise);
    }

    // Wait for all files to be processed
    await Promise.all(uploadPromises);

    // Provide a summary of the results to the user
    if (successfulUploads.length > 0) {
      this.messageService.add({
        severity: 'success',
        summary: 'Upload Complete',
        detail: `${successfulUploads.length} polic(y/ies) uploaded successfully.`,
      });
    }

    if (failedUploads.length > 0) {
      this.messageService.add({
        severity: 'error',
        summary: `${failedUploads.length} Upload(s) Failed`,
        detail: 'See browser console for details on failed files.',
        life: 5000,
      });
      console.error('Failed policy uploads:', failedUploads);
    }

    // If at least one upload was successful, refresh the list and close the dialog
    if (successfulUploads.length > 0) {
      this.loadPolicies();
      this.hideNewAccessPolicyDialog();
    }

    // Always reset the file input to allow selecting the same files again
    element.value = '';
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
      this.messageService.add({
        severity: 'warn',
        summary: 'Action Blocked',
        detail: 'You must expand an Access Policy row before adding a contract definition.',
        life: 5000,
      });
      return;
    }

    // Capture the non-null policy in a local constant
    const currentTargetPolicy = this.targetAccessPolicy;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const uploadPromises: Promise<ContractPolicy>[] = [];
    const failedUploads: { name: string; reason: string }[] = [];

    // Loop through all selected files and create a processing promise for each
    for (const file of Array.from(fileList)) {
      const promise = (async (): Promise<ContractPolicy> => {
        try {
          const fileContent = await file.text();
          const contractDefJson: OdrlContractDefinition = JSON.parse(fileContent);

          // Validate the structure of the template
          if (!contractDefJson['@id'] || !contractDefJson.assetsSelector) {
            throw new Error('Invalid format: Missing @id or assetsSelector.');
          }

          // --- NEW: Strict Validation ---
          // Check if the file specifies an access policy ID and if it mismatches the current context.
          if (contractDefJson.accessPolicyId && contractDefJson.accessPolicyId !== currentTargetPolicy.id) {
            throw new Error(`ID Mismatch: File is for policy "${contractDefJson.accessPolicyId}", not "${currentTargetPolicy.id}".`);
          }
          // --- End of Strict Validation ---

          // ENFORCE CONTEXT: Ensure the correct IDs are set before sending to the backend.
          // This handles cases where the file might not have the ID set at all.
          contractDefJson.accessPolicyId = currentTargetPolicy.id;
          contractDefJson.contractPolicyId = currentTargetPolicy.id;

          await this.policyService.createContractDefinition(contractDefJson);

          return {
            id: contractDefJson['@id'],
            assetId: contractDefJson.assetsSelector[0]?.operandRight || 'Unknown',
          };
        } catch (error: any) {
          failedUploads.push({ name: file.name, reason: error.message || 'Could not process file.' });
          // Reject the promise for this file
          return Promise.reject(error);
        }
      })();
      uploadPromises.push(promise);
    }

    // Wait for all files to be processed and collect results
    const results = await Promise.allSettled(uploadPromises);
    const successfulContractPolicies = results
      .filter((r): r is PromiseFulfilledResult<ContractPolicy> => r.status === 'fulfilled')
      .map(r => r.value);

    // Provide a summary of the results to the user
    if (successfulContractPolicies.length > 0) {
      this.messageService.add({
        severity: 'success',
        summary: 'Upload Complete',
        detail: `${successfulContractPolicies.length} contract definition(s) uploaded successfully.`,
      });

      // Update the UI in one go
      const policyIndex = this.accessPolicies.findIndex(p => p.id === currentTargetPolicy.id);
      if (policyIndex !== -1) {
        this.accessPolicies[policyIndex].contractPolicies.push(...successfulContractPolicies);
        this.accessPolicies = [...this.accessPolicies]; // Trigger change detection
      }
    }

    if (failedUploads.length > 0) {
      this.messageService.add({
        severity: 'error',
        summary: `${failedUploads.length} Upload(s) Failed`,
        detail: 'See browser console for details on failed files.',
        life: 5000,
      });
      console.error('Failed contract definition uploads:', failedUploads);
    }

    // Close the dialog if at least one upload was successful
    if (successfulContractPolicies.length > 0) {
      this.hideNewContractPolicyDialog();
    }

    // Always reset the file input
    element.value = '';
  }

  async saveNewContractPolicy() {
    // Capture the target policy at the start to ensure it's not changed during execution.
    const currentTargetPolicy = this.targetAccessPolicy;

    // Use the captured constant for all checks and operations.
    if (this.newContractPolicy.assetId && currentTargetPolicy) {

      const contractDefId = `contract-def-${this.newContractPolicy.assetId}-for-${currentTargetPolicy.id}`;

      const payload: OdrlContractDefinition = {
        '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
        '@id': contractDefId,
        // Use the non-null constant here, ensuring the correct ID is always used
        accessPolicyId: currentTargetPolicy.id,
        contractPolicyId: currentTargetPolicy.id,
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
        this.messageService.add({severity: 'success', summary: 'Success', detail: 'Contract Definition created successfully.'});

        const newUiContractPolicy: ContractPolicy = {
          id: contractDefId,
          assetId: this.newContractPolicy.assetId
        };

        const policyIndex = this.accessPolicies.findIndex(p => p.id === currentTargetPolicy.id);
        if (policyIndex !== -1) {
          this.accessPolicies[policyIndex].contractPolicies.push(newUiContractPolicy);
          this.accessPolicies = [...this.accessPolicies]; // Trigger change detection
        }

        this.hideNewContractPolicyDialog();
      } catch (error) {
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to create contract definition.'});
        console.error('Failed to create contract definition:', error);
      }
    }
  }
}
