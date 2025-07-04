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

  displayEditAccessPolicyDialog: boolean = false;
  policyToEdit: AccessPolicy | null = null;

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

  async saveEditedAsset() {
    if (!this.assetToEdit) {
      return;
    }

    // Add validation to ensure required fields are not empty
    if (
      !this.assetToEdit.name ||
      !this.assetToEdit.url ||
      !this.assetToEdit.type ||
      !this.assetToEdit.contentType
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
      // Call the service to change
      await this.assetService.updateAsset(this.assetToEdit);

      // On success, update the UI
      const index = this.assets.findIndex((a) => a.id === this.assetToEdit!.id);
      if (index !== -1) {
        this.assets[index] = this.assetToEdit;
        this.assets = [...this.assets]; // Trigger change detection
      }

      this.messageService.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Asset updated successfully.',
      });

      this.hideEditAssetDialog();
    } catch (error) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Failed to update asset.',
      });
      console.error('Failed to update asset:', error);
    }
  }

  deleteAsset(asset: Asset) {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the asset "${asset.name}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      // Make the accept callback async to handle the service call
      accept: async () => {
        try {
          // Call the service to delete the asset
          await this.assetService.deleteAsset(asset.id);

          // if success, update the UI
          this.assets = this.assets.filter((a) => a.id !== asset.id);
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'Asset deleted successfully.',
          });
        } catch (error) {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to delete asset.',
          });
          console.error('Failed to delete asset:', error);
        }
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

  editAccessPolicy(policy: AccessPolicy) {
    // using a deep copy to avoid modifying the original object while editing
    this.policyToEdit = JSON.parse(JSON.stringify(policy));
    this.displayEditAccessPolicyDialog = true;
  }

  hideEditAccessPolicyDialog() {
    this.displayEditAccessPolicyDialog = false;
    this.policyToEdit = null;
  }

  async saveEditedAccessPolicy() {
    if (!this.policyToEdit || !this.policyToEdit.bpn) {
      this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'BPN is required.' });
      return;
    }

    try {
      // call the service to persist the change
      await this.policyService.updateAccessPolicy(this.policyToEdit);

      const index = this.accessPolicies.findIndex(p => p.id === this.policyToEdit!.id);
      if (index !== -1) {
        // IMPORTANT: Preserve the nested contract policies from the original object, as the edit dialog doesn't manage them.
        this.policyToEdit.contractPolicies = this.accessPolicies[index].contractPolicies;
        this.accessPolicies[index] = this.policyToEdit;
        this.accessPolicies = [...this.accessPolicies]; // Trigger change detection
      }

      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Access Policy updated successfully.' });
      this.hideEditAccessPolicyDialog();
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update access policy.' });
      console.error('Failed to update access policy:', error);
    }
  }

  deleteAccessPolicy(policy: AccessPolicy) {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the policy for BPN "${policy.bpn}"? This will also delete all associated contract definitions.`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await this.policyService.deleteAccessPolicy(policy.id);
          this.accessPolicies = this.accessPolicies.filter(p => p.id !== policy.id);
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Access Policy deleted successfully.' });
        } catch (error) {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete access policy.' });
          console.error('Failed to delete access policy:', error);
        }
      },
    });
  }

  deleteContractPolicy(contractPolicy: ContractPolicy, parentAccessPolicy: AccessPolicy) {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the contract definition for asset "${contractPolicy.assetId}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await this.policyService.deleteContractDefinition(contractPolicy.id);

          // Update the UI by removing the item from the nested array
          const parentIndex = this.accessPolicies.findIndex(p => p.id === parentAccessPolicy.id);
          if (parentIndex !== -1) {
            this.accessPolicies[parentIndex].contractPolicies = this.accessPolicies[parentIndex].contractPolicies.filter(cp => cp.id !== contractPolicy.id);
            this.accessPolicies = [...this.accessPolicies]; // Trigger change detection
          }

          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Contract Definition deleted successfully.' });
        } catch (error) {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete contract definition.' });
          console.error('Failed to delete contract definition:', error);
        }
      },
    });
  }

  /**
   * Handles policy file uploads and with validation.
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

    // Get the list of valid values from the component's options
    const validActions = this.actionOptions.map(opt => opt.value);
    const validOperators = this.operatorOptions.map(opt => opt.value);


    for (const file of Array.from(fileList)) {
      const promise = (async () => {
        try {
          const fileContent = await file.text();
          const policyJson: OdrlPolicyDefinition = JSON.parse(fileContent);


          if (!policyJson['@context'] || !policyJson['@context'].edc) {
            throw new Error("Validation failed: The '@context' with a non-empty 'edc' property is required.");
          }
          if (!policyJson['@id']) {
            throw new Error("Validation failed: The '@id' field is missing or empty.");
          }
          if (!policyJson.policy) {
            throw new Error("Validation failed: The 'policy' object is missing.");
          }


          const policy = policyJson.policy;

          if (!policy.permission || !Array.isArray(policy.permission) || policy.permission.length === 0) {
            throw new Error("Validation failed: The 'policy.permission' array is missing or empty.");
          }



          const permission = policy.permission[0];
          if (!permission) {
            throw new Error("Validation failed: The first permission rule is invalid or missing.");
          }


          if (!permission.action) {
            throw new Error("Validation failed: 'permission.action' is missing or empty.");
          }
          if (!validActions.includes(permission.action)) {
            throw new Error(`Validation failed: Invalid action '${permission.action}'. Must be one of: ${validActions.join(', ')}.`);
          }



          if (!permission.constraint || !Array.isArray(permission.constraint) || permission.constraint.length === 0) {
            throw new Error("Validation failed: 'permission.constraint' array is missing or empty.");
          }

          const constraint = permission.constraint[0];



          if (!constraint.leftOperand) {
            throw new Error("Validation failed: 'constraint.leftOperand' is missing or empty.");
          }
          if (!constraint.rightOperand) {
            throw new Error("Validation failed: 'constraint.rightOperand' (the BPN) is missing or empty.");
          }


          if (!constraint.operator) {
            throw new Error("Validation failed: 'constraint.operator' is missing or empty.");
          }
          if (!validOperators.includes(constraint.operator)) {
            throw new Error(`Validation failed: Invalid operator '${constraint.operator}'. Must be one of: ${validOperators.join(', ')}.`);
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

          // Check if the file specifies an access policy ID and if it mismatches the current context.
          if (contractDefJson.accessPolicyId && contractDefJson.accessPolicyId !== currentTargetPolicy.id) {
            throw new Error(`ID Mismatch: File is for policy "${contractDefJson.accessPolicyId}", not "${currentTargetPolicy.id}".`);
          }


          // check the correct IDs are set before sending to the backend.

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
