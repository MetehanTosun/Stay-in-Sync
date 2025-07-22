import {Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

// PrimeNG Modules
import {Table, TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {DialogModule} from 'primeng/dialog';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextarea} from 'primeng/inputtextarea';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {IconFieldModule} from 'primeng/iconfield';
import {InputIconModule} from 'primeng/inputicon';
import {RippleModule} from 'primeng/ripple';
import {ConfirmationService, MessageService} from 'primeng/api';
import {DividerModule} from 'primeng/divider';
//import { DropdownModule } from 'primeng/dropdown';
import {SelectModule} from 'primeng/select';
import {ToastModule} from 'primeng/toast';
import {AutoCompleteModule} from 'primeng/autocomplete';


// App imports
import {Asset} from './models/asset.model';
import {AccessPolicy, ContractPolicy, OdrlContractDefinition, OdrlPolicyDefinition} from './models/policy.model';
import {AssetService} from './services/asset.service';
import {PolicyService} from './services/policy.service';
import {HttpErrorService} from '../../../../core/services/http-error.service';

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
    //DropdownModule,
    SelectModule,
    ToastModule,
    AutoCompleteModule,
  ],
  templateUrl: './edc-assets-and-policies.component.html',
  styleUrls: ['./edc-assets-and-policies.component.css'],
  providers: [ConfirmationService, MessageService],
})
export class EdcAssetsAndPoliciesComponent implements OnInit {
  @ViewChild('dtAssets') dtAssets!: Table;
  @ViewChild('dtPolicies') dtPolicies!: Table;

  // Asset properties
  assets: Asset[] = [];
  assetLoading: boolean = true;
  displayNewAssetDialog: boolean = false;
  newAsset: Asset = this.createEmptyAsset();
  displayEditAssetDialog: boolean = false;
  assetToEdit: Asset | null = null;

  // Policy properties
  policyLoading: boolean = true;
  manuallyExpandedRows: Set<string> = new Set<string>();
  displayNewAccessPolicyDialog: boolean = false;
  newAccessPolicy: AccessPolicy = this.createEmptyAccessPolicy();
  displayNewContractPolicyDialog: boolean = false;

  newContractPolicy: {
    id: string;
    assetId: string;
    operandLeft: string;
    operator: string;
  } = this.createEmptyContractPolicy();

  displayEditContractPolicyDialog: boolean = false;
  contractPolicyToEdit: {
    id: string;
    assetId: string;
    operandLeft: string;
    operator: string;
  } | null = null;

  targetAccessPolicy: AccessPolicy | null = null;
  displayEditAccessPolicyDialog: boolean = false;
  policyToEdit: AccessPolicy | null = null;
  operatorOptions: { label: string; value: string; }[];
  actionOptions: { label: string; value: string; }[];


  allAccessPolicies: AccessPolicy[] = [];
  filteredAccessPolicies: AccessPolicy[] = [];

  assetIdSuggestions: Asset[] = [];

  constructor(
    private assetService: AssetService,
    private policyService: PolicyService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private httpErrorService: HttpErrorService,
  ) {
    // initialize dropdown options
    this.operatorOptions = [
      {label: 'Equals', value: 'eq'},
      {label: 'Not Equals', value: 'neq'},
      {label: 'Is In', value: 'in'},
    ];
    this.actionOptions = [
      {label: 'Use', value: 'use'},
      {label: 'Read', value: 'read'},
      {label: 'Write', value: 'write'},
    ];

  }

  ngOnInit(): void {
    this.loadAssets();
    this.loadPolicies();
  }

  /**
   * Filters assets based on user input for the autocomplete component.
   * It searches by both Asset ID and asset's name
   * @param event The autocomplete event containing the user's query.
   */
  searchAssets(event: { query: string }) {
    const query = event.query.toLowerCase();
    this.assetIdSuggestions = this.assets.filter(asset =>
      asset.assetId.toLowerCase().includes(query) ||
      asset.name.toLowerCase().includes(query)
    );
  }

  private loadAssets(): void {
    this.assetLoading = true;
    this.assetService
      .getAssets()
      .subscribe({
        next: data => {
          this.assets = data
        },
        error: err => {
          console.error('Failed to load assets:', err);
          this.httpErrorService.handleError(err);
        },
        complete: () => {
          this.assetLoading = false
        }
      });
  }

  async loadPolicies() {
    this.policyLoading = true;
    try {
      const [accessPolicies, contractDefinitions] = await Promise.all([
        this.policyService.getAccessPolicies(),
        this.policyService.getContractDefinitions(),
      ]);

      const contractDefsByPolicyId = new Map<string, ContractPolicy[]>();
      for (const cd of contractDefinitions) {
        const parentId = cd.accessPolicyId;
        if (!parentId) continue;

        const uiContractPolicy: ContractPolicy = {
          id: cd['@id'],
          assetId: cd.assetsSelector?.[0]?.operandRight || 'Unknown Asset',
        };

        if (!contractDefsByPolicyId.has(parentId)) {
          contractDefsByPolicyId.set(parentId, []);
        }
        contractDefsByPolicyId.get(parentId)!.push(uiContractPolicy);
      }

      for (const policy of accessPolicies) {
        policy.contractPolicies = contractDefsByPolicyId.get(policy.id) || [];
      }

      this.allAccessPolicies = accessPolicies;
      this.filteredAccessPolicies = [...this.allAccessPolicies];

    } catch (error) {
      console.error('Failed to load policies:', error);
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to load policies.'});
    } finally {
      this.policyLoading = false;
    }
  }

  onGlobalFilter(event: Event, table: Table) {
    const inputElement = event.target as HTMLInputElement;
    table.filterGlobal(inputElement.value, 'contains');
  }

  onGlobalPolicySearch(event: Event): void {
    const searchTerm = (event.target as HTMLInputElement).value.toLowerCase();

    // If the search bar is empty, show all policies
    if (!searchTerm) {
      this.filteredAccessPolicies = [...this.allAccessPolicies];
      return;
    }

    this.filteredAccessPolicies = this.allAccessPolicies.filter(policy => {
      // Create a single string of all searchable text for this policy
      const searchableText = [
        policy.id,
        policy.bpn,
        policy.action,
        policy.operator,

        ...policy.contractPolicies.flatMap(cp => [cp.id, cp.assetId])
      ].join(' ').toLowerCase();

      // Check if the combined text includes the search term
      return searchableText.includes(searchTerm);
    });
  }

  // Asset methods
  private createEmptyAsset(): Asset {
    // Initialize with empty strings to allow for placeholder text
    return {assetId: '', name: '', url: '', type: '', description: '', contentType: ''};
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
          failedUploads.push({name: file.name, reason: error.message || 'Could not process file.'});
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
    this.assetToEdit = {...asset};
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
      const index = this.assets.findIndex((a) => a.assetId === this.assetToEdit!.assetId);
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
          await this.assetService.deleteAsset(asset.assetId);

          // if success, update the UI
          this.assets = this.assets.filter((a) => a.assetId !== asset.assetId);
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
      action: 'use',
      operator: 'eq',
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
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'BPN is required for manual creation.'
      });
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
      this.messageService.add({severity: 'warn', summary: 'Validation Error', detail: 'BPN is required.'});
      return;
    }

    try {
      await this.policyService.updateAccessPolicy(this.policyToEdit);

      // Update the 'allAccessPolicies' master list
      const index = this.allAccessPolicies.findIndex((p: AccessPolicy) => p.id === this.policyToEdit!.id);
      if (index !== -1) {
        this.policyToEdit.contractPolicies = this.allAccessPolicies[index].contractPolicies;
        this.allAccessPolicies[index] = this.policyToEdit;
        //refresh the filtered list to update the UI
        this.filteredAccessPolicies = [...this.allAccessPolicies];
      }

      this.messageService.add({severity: 'success', summary: 'Success', detail: 'Access Policy updated successfully.'});
      this.hideEditAccessPolicyDialog();
    } catch (error) {
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to update access policy.'});
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
          // update both the master and filtered lists
          this.allAccessPolicies = this.allAccessPolicies.filter((p: AccessPolicy) => p.id !== policy.id);
          this.filteredAccessPolicies = [...this.allAccessPolicies];
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'Access Policy deleted successfully.'
          });
        } catch (error) {
          this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to delete access policy.'});
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

          //update the nested array in the 'allAccessPolicies' master list
          const parentIndex = this.allAccessPolicies.findIndex((p: AccessPolicy) => p.id === parentAccessPolicy.id);
          if (parentIndex !== -1) {
            this.allAccessPolicies[parentIndex].contractPolicies = this.allAccessPolicies[parentIndex].contractPolicies.filter((cp: ContractPolicy) => cp.id !== contractPolicy.id);
            // refresh the filtered list to update the UI
            this.filteredAccessPolicies = [...this.allAccessPolicies];
          }

          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'Contract Definition deleted successfully.'
          });
        } catch (error) {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to delete contract definition.'
          });
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
          failedUploads.push({name: file.name, reason: error.message || 'Could not process file.'});
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
  private createEmptyContractPolicy() {
    return {
      id: '',
      assetId: '',
      operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id', // Default value
      operator: '=',
    };
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

  /**
   * Validates and saves a new Contract Definition created manually.
   */
  async saveNewContractPolicy() {

    if (
      !this.newContractPolicy.assetId?.trim() ||
      !this.newContractPolicy.operandLeft?.trim() ||
      !this.newContractPolicy.operator
    ) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'Asset ID, EDC ID (Operand Left), and Operator are required fields.',
      });
      return;
    }

    if (!this.targetAccessPolicy) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Cannot save: No parent Access Policy context.',
      });
      return;
    }

    try {

      const contractDefId = `contract-def-${this.newContractPolicy.assetId}`;
      const odrlPayload: OdrlContractDefinition = {
        '@context': {edc: 'https://w3id.org/edc/v0.0.1/ns/'},
        '@id': contractDefId,
        accessPolicyId: this.targetAccessPolicy.id,
        contractPolicyId: this.targetAccessPolicy.id,
        assetsSelector: [
          {
            operandLeft: this.newContractPolicy.operandLeft,
            operator: this.newContractPolicy.operator,
            operandRight: this.newContractPolicy.assetId,
          },
        ],
      };


      await this.policyService.createContractDefinition(odrlPayload);


      const newUiPolicy: ContractPolicy = {
        id: contractDefId,
        assetId: this.newContractPolicy.assetId,
      };

      const parentIndex = this.allAccessPolicies.findIndex((p: AccessPolicy) => p.id === this.targetAccessPolicy!.id);
      if (parentIndex !== -1) {
        this.allAccessPolicies[parentIndex].contractPolicies.push(newUiPolicy);

        this.filteredAccessPolicies = [...this.allAccessPolicies];
      }

      this.messageService.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Contract Definition created successfully.'
      });
      this.hideNewContractPolicyDialog();

    } catch (error: any) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to create contract definition.'
      });
      console.error('Failed to create contract definition:', error);
    }
  }


  /**
   * Handles contract definition file uploads with detailed validation.
   */
  async onContractDefFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    if (!this.targetAccessPolicy) {
      this.messageService.add({
        severity: 'error',
        summary: 'Upload Error',
        detail: 'Cannot upload file: No parent Access Policy is selected.'
      });
      return;
    }

    const uploadPromises: Promise<void>[] = [];
    const successfulUploads: string[] = [];
    const failedUploads: { name: string; reason: string }[] = [];

    for (const file of Array.from(fileList)) {
      const promise = (async () => {
        try {
          const fileContent = await file.text();
          const contractDefJson: OdrlContractDefinition = JSON.parse(fileContent);

          if (!contractDefJson['@id']?.trim()) {
            throw new Error("Validation failed: The '@id' field is missing or empty.");
          }
          if (!contractDefJson['@context']?.edc) {
            throw new Error("Validation failed: The '@context' with a non-empty 'edc' property is required.");
          }
          if (!contractDefJson.accessPolicyId?.trim()) {
            throw new Error("Validation failed: The 'accessPolicyId' field is missing or empty.");
          }
          if (contractDefJson.accessPolicyId !== this.targetAccessPolicy!.id) {
            throw new Error(`Validation failed: 'accessPolicyId' in file does not match the selected policy ('${this.targetAccessPolicy!.id}').`);
          }
          if (!contractDefJson.assetsSelector || !Array.isArray(contractDefJson.assetsSelector) || contractDefJson.assetsSelector.length === 0) {
            throw new Error("Validation failed: The 'assetsSelector' array is missing or empty.");
          }

          const selector = contractDefJson.assetsSelector[0];

          if (!selector?.operandLeft?.trim()) {
            throw new Error("Validation failed: 'operandLeft' in assetsSelector is missing or empty.");
          }
          if (selector.operandLeft !== 'https://w3id.org/edc/v0.0.1/ns/id') {
            throw new Error(`Validation failed: Invalid 'operandLeft' value. Expected 'https://w3id.org/edc/v0.0.1/ns/id'.`);
          }
          if (!selector?.operator?.trim()) {
            throw new Error("Validation failed: 'operator' in assetsSelector is missing or empty.");
          }
          if (selector.operator !== '=') {
            throw new Error("Validation failed: Invalid 'operator' in contract definition. Must be =");
          }
          if (!selector?.operandRight?.trim()) {
            throw new Error("Validation failed: 'operandRight' (the Asset ID) in assetsSelector is missing or empty.");
          }


          // Check if the asset ID from the file exists in our current assets list
          const assetIdFromFile = selector.operandRight.trim();
          const assetExists = this.assets.some(asset => asset.assetId === assetIdFromFile);

          if (!assetExists) {
            throw new Error(`Validation failed: Asset with ID '${assetIdFromFile}' does not exist.`);
          }


          await this.policyService.createContractDefinition(contractDefJson);

          // Update the UI
          const newContractPolicy: ContractPolicy = {
            id: contractDefJson['@id'],
            assetId: selector.operandRight
          };

          this.targetAccessPolicy!.contractPolicies.push(newContractPolicy);
          successfulUploads.push(file.name);

        } catch (error: any) {
          failedUploads.push({name: file.name, reason: error.message || 'Could not process file.'});
        }
      })();
      uploadPromises.push(promise);
    }

    await Promise.all(uploadPromises);

    if (successfulUploads.length > 0) {
      this.messageService.add({
        severity: 'success',
        summary: 'Upload Complete',
        detail: `${successfulUploads.length} contract definition(s) uploaded successfully.`,
      });
      await this.loadPolicies();
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

    if (successfulUploads.length > 0) {
      this.hideNewContractPolicyDialog();
    }

    element.value = '';
  }


  /**
   * Opens the dialog to edit an existing Contract Definition.
   */
  editContractPolicy(contractPolicy: ContractPolicy, parentAccessPolicy: AccessPolicy) {

    this.contractPolicyToEdit = {
      id: contractPolicy.id,
      assetId: contractPolicy.assetId,
      operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id', // Default value
      operator: '=', // Default value
    };
    this.targetAccessPolicy = parentAccessPolicy; // Set context for saving
    this.displayEditContractPolicyDialog = true;
  }

  /**
   * Hides the "Edit Contract Definition" dialog.
   */
  hideEditContractPolicyDialog() {
    this.displayEditContractPolicyDialog = false;
    this.contractPolicyToEdit = null;
    this.targetAccessPolicy = null; // Clear context
  }


  /**
   * Saves the changes to an existing Contract Definition.
   */
  async saveEditedContractPolicy() {
    if (!this.contractPolicyToEdit || !this.targetAccessPolicy) {
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'Cannot save: Editing context is lost.'});
      return;
    }

    // Validation
    if (
      !this.contractPolicyToEdit.assetId?.trim() ||
      !this.contractPolicyToEdit.operandLeft?.trim() ||
      !this.contractPolicyToEdit.operator
    ) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'All fields are required.',
      });
      return;
    }

    try {
      // Build the ODRL payload for the update
      const odrlPayload: OdrlContractDefinition = {
        '@context': {edc: 'https://w3id.org/edc/v0.0.1/ns/'},
        '@id': this.contractPolicyToEdit.id,
        accessPolicyId: this.targetAccessPolicy.id,
        contractPolicyId: this.targetAccessPolicy.id,
        assetsSelector: [
          {
            operandLeft: this.contractPolicyToEdit.operandLeft,
            operator: this.contractPolicyToEdit.operator,
            operandRight: this.contractPolicyToEdit.assetId,
          },
        ],
      };

      // relies on the 'updateContractDefinition' method in PolicyService
      await this.policyService.updateContractDefinition(odrlPayload);

      // Update the UI model
      const parentIndex = this.allAccessPolicies.findIndex((p: AccessPolicy) => p.id === this.targetAccessPolicy!.id);
      if (parentIndex !== -1) {
        const contractIndex = this.allAccessPolicies[parentIndex].contractPolicies.findIndex((cp: ContractPolicy) => cp.id === this.contractPolicyToEdit!.id);
        if (contractIndex !== -1) {
          this.allAccessPolicies[parentIndex].contractPolicies[contractIndex].assetId = this.contractPolicyToEdit!.assetId;

          this.filteredAccessPolicies = [...this.allAccessPolicies];
        }
      }

      this.messageService.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Contract Definition updated successfully.'
      });
      this.hideEditContractPolicyDialog();

    } catch (error: any) {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to update contract definition.'
      });
      console.error('Failed to update contract definition:', error);
    }
  }


}
