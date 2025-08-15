import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

// PrimeNG Modules
import { Table, TableModule, TableRowSelectEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { RippleModule } from 'primeng/ripple';
import {ConfirmationService, MessageService} from 'primeng/api';
import { DividerModule } from 'primeng/divider';
import { TabViewModule } from 'primeng/tabview';
import { DropdownModule } from 'primeng/dropdown';
import { ToastModule } from 'primeng/toast';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { MessageModule } from 'primeng/message';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';



// App imports
import { Asset } from './models/asset.model';
import { AccessPolicy, OdrlContractDefinition, ContractPolicy, OdrlPolicyDefinition, OdrlCriterion, OdrlConstraint } from './models/policy.model';
import { AssetService } from './services/asset.service';
import { PolicyService } from './services/policy.service';
import { EdcInstanceService } from '../edc-instances/services/edc-instance.service';
import {lastValueFrom, Subject, Subscription} from 'rxjs';
import {debounceTime} from "rxjs/operators";

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
    TagModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    RippleModule,
    DividerModule,
    DropdownModule,
    ToastModule,
    AutoCompleteModule,
    MessageModule,
    MonacoEditorModule,
    TabViewModule,
  ],
  templateUrl: './edc-assets-and-policies.component.html',
  styleUrls: ['./edc-assets-and-policies.component.css'],
  providers: [ConfirmationService, MessageService],
})
export class EdcAssetsAndPoliciesComponent implements OnInit, OnDestroy {
  @ViewChild('dtAssets') dtAssets!: Table;
  @ViewChild('dtPolicies') dtPolicies!: Table;
  @ViewChild('dtContracts') dtContracts!: Table;

  // Asset properties
  assets: Asset[] = [];
  allOdrlAssets: any[] = []; // For holding the raw ODRL
  assetLoading: boolean = true;
  displayNewAssetDialog: boolean = false;
  displayEditAssetDialog: boolean = false;
  assetToEditODRL: any | null = null; // For editing raw JSON

  // Policy properties
  policyLoading: boolean = true;
  displayNewAccessPolicyDialog: boolean = false;
  displayNewContractPolicyDialog: boolean = false;

  newContractPolicy: {
    id: string;
    assetId: string;
    accessPolicyId: string; // The ID of the parent Access Policy
  } = this.createEmptyContractPolicy();

  displayEditContractPolicyDialog: boolean = false;
  contractPolicyToEdit: ContractPolicy | null = null;

  displayEditAccessPolicyDialog: boolean = false;
  policyToEditODRL: OdrlPolicyDefinition | null = null;
  operatorOptions: { label: string; value: string; }[];
  actionOptions: { label: string; value: string; }[];

  allAccessPolicies: AccessPolicy[] = [];
  allOdrlAccessPolicies: OdrlPolicyDefinition[] = [];
  filteredAccessPolicies: AccessPolicy[] = [];

  // A flat list for all contract definitions
  allOdrlContractDefinitions: OdrlContractDefinition[] = [];
  allContractDefinitions: ContractPolicy[] = [];
  filteredContractDefinitions: ContractPolicy[] = [];

  assetIdSuggestions: Asset[] = [];
  accessPolicySuggestions: AccessPolicy[] = [];

  // Properties for the 'New Contract Definition' dialog
  assetsForDialog: (Asset & { operator: string })[] = [];
  selectedAssetsInDialog: (Asset & { operator: string })[] = [];

  // Properties for Expert Mode in 'New Contract Definition' dialog
  isExpertMode: boolean = false;
  expertModeJsonContent: string = '';
  contractDefinitionTemplates: { name: string, content: any }[] = [];
  selectedTemplate: any | null = null;
  isComplexSelectorForEdit: boolean = false;
  contractDefinitionToEditODRL: OdrlContractDefinition | null = null;

  // Properties for Asset Templates
  assetTemplates: { name: string, content: any }[] = [];
  selectedAssetTemplate: any | null = null;

  // Properties for Access Policy Templates
  accessPolicyTemplates: { name: string, content: any }[] = [];
  selectedAccessPolicyTemplate: any | null = null;

  // Dynamic form properties for Access Policy dialogs
  formAction: string = 'use';
  formConstraints: {
    label: string;
    leftOperand: string;
    operator: string;
    rightOperand: any;
    valueLabel: string;
  }[] = [];
  isPolicyJsonComplex: boolean = false;
  isContractJsonComplex: boolean = false;


  // BPN suggestions
  bpnSuggestions: string[] = [];
  allBpns: string[] = [];

  editorOptions = {
    theme: 'vs-dark',
    language: 'json',
    automaticLayout: true,
    minimap: { enabled: false },
  };

  // Properties for the read-only view dialog
  displayViewDialog: boolean = false;
  viewDialogHeader: string = '';
  jsonToView: string = '';
  readOnlyEditorOptions = {
    ...this.editorOptions,
    readOnly: true,
  };

  // Properties for the structured detail view
  viewingEntityType: 'asset' | 'policy' | 'contract' | null = null;
  linkedAccessPolicy: AccessPolicy | null = null;
  linkedAssets: Asset[] = [];

  // Real-time sync from JSON editor to form
  private jsonSyncSubscription: Subscription | null = null;
  private jsonSyncSubject = new Subject<void>();
  private contractJsonSyncSubscription: Subscription | null = null;
  private contractJsonSyncSubject = new Subject<void>();

  constructor(
    private assetService: AssetService,
    private policyService: PolicyService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private edcInstanceService: EdcInstanceService
  ) {
    // initialize dropdown options
    this.operatorOptions = [
      { label: 'Equals', value: 'eq' },
      { label: 'Not Equals', value: 'neq' },
    ];
    this.actionOptions = [
      {label: 'Use', value: 'use'},
      {label: 'Read', value: 'read'},
      {label: 'Write', value: 'write'},
    ];

    this.jsonSyncSubscription = this.jsonSyncSubject.pipe(
      debounceTime(500) // Wait for 500ms of silence before processing
    ).subscribe(() => {
      this.syncFormFromJson();
    });

    this.contractJsonSyncSubscription = this.contractJsonSyncSubject.pipe(
      debounceTime(500) // Wait for 500ms of silence before processing
    ).subscribe(() => {
      this.syncContractFormFromJson(); // Corrected from syncFormFromJson
    });

  }

  ngOnInit(): void {
    this.loadAssets();
    this.loadPoliciesAndDefinitions();
    this.loadContractDefinitionTemplates();
    this.loadAccessPolicyTemplates();
    this.loadAssetTemplates();
    this.loadAllBpns();
  }

  ngOnDestroy(): void {
    this.jsonSyncSubscription?.unsubscribe();
    this.contractJsonSyncSubscription?.unsubscribe();
  }

  private loadContractDefinitionTemplates() {
    // In backend this would come from a service or whatever
    this.contractDefinitionTemplates = [
      {
        name: 'Simple Asset Selector',
        content: {
          '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
          '@id': 'CONTRACT_DEFINITION_ID_1',
          accessPolicyId: 'ACCESS_POLICY_ID',
          contractPolicyId: 'CONTRACT_POLICY_ID',
          assetsSelector: [
            {
              operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
              operator: 'eq',
              operandRight: 'ASSET_ID',
            },
          ],
        }
      },
      {
        name: 'Multi-Asset Selector (using IN)',
        content: {
          '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
          '@id': 'CONTRACT_DEFINITION_ID_2',
          accessPolicyId: 'ACCESS_POLICY_ID',
          contractPolicyId: 'CONTRACT_POLICY_ID',
          assetsSelector: [
            {
              "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
              "operator": "in",
              "operandRight": ["ASSET_ID_1", "ASSET_ID_2", "ASSET_ID_3"]
            }
          ]
        }
      },
      {
        name: 'Template with Variables (Advanced)',
        content: {
          '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
          '@id': 'CONTRACT_DEF_ID_WITH_VARIABLE_${uuid}',
          accessPolicyId: 'ACCESS_POLICY_ID_VARIABLE_${access_policy_id}',
          contractPolicyId: 'CONTRACT_POLICY_ID_VARIABLE_${contract_policy_id}',
          assetsSelector: [
            {
              operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
              operator: 'eq',
              operandRight: '${asset_id}',
            },
          ],
        }
      }
    ];
  }

  private loadAccessPolicyTemplates() {

    this.accessPolicyTemplates = [
      {
        name: 'BPN Access Policy',
        content: {
          "@context": { "odrl": "http://www.w3.org/ns/odrl/2/" },
          "@id": "ACCESS_POLICY_ID_1",
          "policy": {
            "permission": [{
              "action": "use",
              "constraint": [{
                "leftOperand": "BusinessPartnerNumber",
                "operator": "eq",
                "rightOperand": "BPN_OF_ALLOWED_PARTNER"
              }]
            }]
          }
        }
      },
      {
        name: 'Membership Policy',
        content: {
          "@context": { "odrl": "http://www.w3.org/ns/odrl/2/" },
          "@id": "MEMBERSHIP_POLICY_ID",
          "policy": {
            "permission": [{
              "action": "use",
              "constraint": [{
                "leftOperand": "Membership",
                "operator": "eq",
                "rightOperand": "active"
              }]
            }]
          }
        }
      },
      {
        name: 'Framework Agreement Policy (Traceability)',
        content: {
          "@context": { "odrl": "http://www.w3.org/ns/odrl/2/" },
          "@id": "TRACEABILITY_FRAMEWORK_POLICY_ID",
          "policy": {
            "permission": [{
              "action": "use",
              "constraint": [{
                "leftOperand": "FrameworkAgreement.traceability",
                "operator": "eq",
                "rightOperand": "active"
              }]
            }]
          }
        }
      },
      {
        name: 'Multi-Constraint BPN Policy',
        content: {
          "@context": { "odrl": "http://www.w3.org/ns/odrl/2/" },
          "@id": "MULTI_CONSTRAINT_POLICY_ID",
          "policy": {
            "permission": [{
              "action": "use",
              "constraint": [
                { "leftOperand": "BusinessPartnerNumber", "operator": "eq", "rightOperand": "BPN_OF_ALLOWED_PARTNER" },
                { "leftOperand": "Membership", "operator": "eq", "rightOperand": "active" }
              ]
            }]
          }
        }
      }
    ];
  }

  private loadAssetTemplates() {
    // Mock data for asset templates
    this.assetTemplates = [
      {
        name: 'Standard HTTP Data Asset',
        content: {
          "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
          "@id": "UNIQUE_ASSET_ID",
          "properties": {
            "asset:prop:name": "HUMAN_READABLE_ASSET_NAME",
            "asset:prop:description": "A_BRIEF_DESCRIPTION_OF_THE_ASSET",
            "asset:prop:contenttype": "application/json",
            "asset:prop:version": "1.0.0"
          },
          "dataAddress": {
            "type": "HttpData",
            "baseUrl": "https://YOUR_BACKEND_ENDPOINT/api/data"
          }
        }
      }
      ,
      {
        name: 'Traceability Data Asset (BOM)',
        content: {
          "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
          "@id": "urn:uuid:A_UNIQUE_UUID_FOR_THE_ASSET",
          "properties": {
            "asset:prop:name": "BOM_AS_BUILT_V1.2.3",
            "asset:prop:description": "BILL_OF_MATERIALS_FOR_A_SPECIFIC_COMPONENT",
            "asset:prop:contenttype": "application/json",
            "asset:prop:version": "1.2.3",
            "asset:prop:standard": "urn:bamm:io.catenax.serial_part:1.1.0#SerialPart"
          },
          "dataAddress": {
            "type": "HttpData",
          }
        }
      }
    ];
  }

  private loadAllBpns() {
    this.edcInstanceService.getEdcInstancesLarge().then(instances => {
      // Use a Set to get unique BPNs from existing instances
      const bpnSet = new Set(instances.map(i => i.bpn));
      this.allBpns = [...bpnSet];
    });
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

  /**
   * Filters access policies for the autocomplete component.
   * Searches by both Policy ID and BPN.
   * @param event The autocomplete event containing the user's query.
   */
  searchAccessPolicies(event: { query: string }) {
    const query = event.query.toLowerCase();
    this.accessPolicySuggestions = this.allAccessPolicies.filter(policy =>
      policy.id.toLowerCase().includes(query) ||
      policy.bpn.toLowerCase().includes(query)
    );
  }

  private async loadAssets(): Promise<void> {
    this.assetLoading = true;
    try {
      const [assets, odrlAssets] = await Promise.all<any>([
        lastValueFrom(this.assetService.getAssets()),
        this.assetService.getOdrlAssets()
      ]);
      this.assets = assets;
      this.allOdrlAssets = odrlAssets;
      // If a dialog that uses the asset list is open, refresh its content
      if (this.displayNewContractPolicyDialog || this.displayEditContractPolicyDialog) {
        this.refreshAssetsForDialog();
      }
    } catch (error) {
      console.error('Failed to load assets:', error);
      this.messageService.add({severity: 'error', summary: 'Error', detail: 'Could not load assets.'});
    } finally {
      this.assetLoading = false;
    }
  }

  async loadPoliciesAndDefinitions() {
    this.policyLoading = true;
    try {
      const [accessPolicies, contractDefinitions, odrlAccessPolicies] = await Promise.all([
        this.policyService.getAccessPolicies(),
        this.policyService.getContractDefinitions(),
        this.policyService.getOdrlPolicyDefinitions(),
      ]);

      this.allOdrlContractDefinitions = contractDefinitions;
      this.allOdrlAccessPolicies = odrlAccessPolicies;

      // Create a map from policy ID to the policy object for efficient lookup
      const policyMap = new Map<string, AccessPolicy>();
      accessPolicies.forEach(p => policyMap.set(p.id, p));

      // Create list of all contract definitions
      this.allContractDefinitions = contractDefinitions.map(cd => {
        const parentId = cd.accessPolicyId;
        const parentPolicy = parentId ? policyMap.get(parentId) : undefined;
        const assetIds = cd.assetsSelector?.map(s => s.operandRight).join(', ') ?? 'Unknown Asset';
        return {
          id: cd['@id'],
          assetId: assetIds,
          bpn: parentPolicy?.bpn || 'Unknown BPN',
          accessPolicyId: parentId || ''
        };
      });
      this.filteredContractDefinitions = [...this.allContractDefinitions];

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
        policy.operator
      ].join(' ').toLowerCase();

      // Check if the combined text includes the search term
      return searchableText.includes(searchTerm);
    });
  }

  onGlobalContractDefSearch(event: Event): void {
    const searchTerm = (event.target as HTMLInputElement).value.toLowerCase();
    if (!searchTerm) {
      this.filteredContractDefinitions = [...this.allContractDefinitions];
      return;
    }
    this.filteredContractDefinitions = this.allContractDefinitions.filter(cd => {
      const searchableText = [
        cd.id,
        cd.assetId,
        cd.bpn
      ].join(' ').toLowerCase();
      return searchableText.includes(searchTerm);
    });
  }

  // Asset methods
  openNewAssetDialog() {
    this.expertModeJsonContent = JSON.stringify({
      "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
      "@id": "asset-id-goes-here",
      "properties": {
        "asset:prop:name": "Asset Name",
        "asset:prop:description": "A description of the asset.",
        "asset:prop:contenttype": "application/json"
      },
      "dataAddress": {
        "type": "HttpData",
        "baseUrl": "https://my-backend/api/data"
      }
    }, null, 2);
    this.selectedAssetTemplate = null;
    this.displayNewAssetDialog = true;
  }

  hideNewAssetDialog() {
    this.displayNewAssetDialog = false;
  }

  async saveNewAsset() {
    try {
      const assetJson = JSON.parse(this.expertModeJsonContent);
      // Basic validation
      if (!assetJson['@id'] || !assetJson.properties || !assetJson.dataAddress) {
        throw new Error("Invalid asset structure. '@id', 'properties', and 'dataAddress' are required.");
      }
      await this.assetService.uploadAsset(assetJson);
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Asset created successfully.' });
      this.loadAssets();
      this.hideNewAssetDialog();
    } catch (error: any) {
      const detail = error.message.includes('JSON') ? 'Could not parse JSON.' : 'Failed to save asset.';
      this.messageService.add({ severity: 'error', summary: 'Error', detail });
      console.error('Failed to save asset:', error);
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
    this.assetToEditODRL = this.allOdrlAssets.find(a => a['@id'] === asset.assetId) ?? null;
    if (this.assetToEditODRL) {
      this.expertModeJsonContent = JSON.stringify(this.assetToEditODRL, null, 2);
      this.displayEditAssetDialog = true;
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Could not find the full ODRL asset to edit.' });
    }
  }

  hideEditAssetDialog() {
    this.displayEditAssetDialog = false;
    this.assetToEditODRL = null;
  }

  async saveEditedAsset() {
    if (!this.assetToEditODRL) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Editing context is lost.' });
      return;
    }
    try {
      const assetJson = JSON.parse(this.expertModeJsonContent);
      if (assetJson['@id'] !== this.assetToEditODRL['@id']) {
        throw new Error("The '@id' of the asset cannot be changed during an edit.");
      }
      // The service's uploadAsset handles both create and update
      await this.assetService.uploadAsset(assetJson);
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Asset updated successfully.' });
      this.loadAssets();
      this.hideEditAssetDialog();
    } catch (error: any) {
      const detail = error.message.includes('JSON') ? 'Could not parse JSON.' : error.message || 'Failed to save asset.';
      this.messageService.add({ severity: 'error', summary: 'Error', detail });
      console.error('Failed to save asset:', error);
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

  // Access Policy Methods

  openNewAccessPolicyDialog() {
    this.isPolicyJsonComplex = false;
    this.formAction = 'use';
    this.formConstraints = [];
    // Create a default empty JSON structure
    const odrlPayload: OdrlPolicyDefinition = {
      '@context': { odrl: 'http://www.w3.org/ns/odrl/2/' },
      '@id': 'POLICY_ID',
      policy: {
        permission: [{
          action: 'use',
          constraint: []
        }]
      }
    };
    this.expertModeJsonContent = JSON.stringify(odrlPayload, null, 2);
    this.syncFormFromJson();
    this.selectedAccessPolicyTemplate = null;
    this.policyToEditODRL = null; // Ensure we are in "create" mode
    this.displayNewAccessPolicyDialog = true;
  }

  hideNewAccessPolicyDialog() {
    this.displayNewAccessPolicyDialog = false;
  }

  async saveNewAccessPolicy() {
    let policyJson: OdrlPolicyDefinition;
    try {
      policyJson = JSON.parse(this.expertModeJsonContent);
      if (!policyJson['@id'] || !policyJson.policy) {
        throw new Error("Invalid policy structure. '@id' and 'policy' are required.");
      }
    } catch (e) {
      this.messageService.add({ severity: 'error', summary: 'JSON Parse Error', detail: 'Could not parse the JSON content.' });
      return;
    }

    try {
      await this.policyService.uploadPolicyDefinition(policyJson);
      this.messageService.add({severity: 'success', summary: 'Success', detail: 'Access Policy created successfully.'});
      this.loadPoliciesAndDefinitions();
      this.hideNewAccessPolicyDialog();
    } catch (error: any) {
      const detail = error.message.includes('JSON') ? 'Could not parse JSON.' : 'Failed to save access policy.';
      this.messageService.add({ severity: 'error', summary: 'Error', detail: detail });
      console.error('Failed to save access policy:', error);
    }
  }

  editAccessPolicy(policy: AccessPolicy) {
    this.policyToEditODRL = this.allOdrlAccessPolicies.find(p => p['@id'] === policy.id) ?? null;
    if (this.policyToEditODRL) {
      this.expertModeJsonContent = JSON.stringify(this.policyToEditODRL, null, 2);
      this.syncFormFromJson();
      this.displayEditAccessPolicyDialog = true;
    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Could not find the full ODRL policy to edit.' });
    }
  }

  hideEditAccessPolicyDialog() {
    this.displayEditAccessPolicyDialog = false;
    this.policyToEditODRL = null;
  }

  async saveEditedAccessPolicy() {
    if (!this.policyToEditODRL) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Editing context is lost.' });
      return;
    }
    try {
      const policyJson = JSON.parse(this.expertModeJsonContent);
      if (policyJson['@id'] !== this.policyToEditODRL['@id']) {
        throw new Error("The '@id' of the policy cannot be changed during an edit.");
      }
      await this.policyService.uploadPolicyDefinition(policyJson);
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Access Policy updated successfully.' });
      this.loadPoliciesAndDefinitions();
      this.hideEditAccessPolicyDialog();
    } catch (error: any) {
      const detail = error.message.includes('JSON') ? 'Could not parse JSON.' : error.message || 'Failed to save access policy.';
      this.messageService.add({ severity: 'error', summary: 'Error', detail: detail });
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

          // Also remove associated contract definitions from the UI
          this.allContractDefinitions = this.allContractDefinitions.filter(cd => cd.accessPolicyId !== policy.id);
          this.filteredContractDefinitions = [...this.allContractDefinitions];

          // update both the master and filtered lists for access policies
          this.allAccessPolicies = this.allAccessPolicies.filter((p: AccessPolicy) => p.id !== policy.id);
          this.filteredAccessPolicies = [...this.allAccessPolicies];

          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Access Policy and associated definitions deleted successfully.' });
        } catch (error) {
          this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to delete access policy.'});
          console.error('Failed to delete access policy:', error);
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
      this.loadPoliciesAndDefinitions();
      this.hideNewAccessPolicyDialog();
    }

    // Always reset the file input to allow selecting the same files again
    element.value = '';
  }


  //Contract policy methods
  private createEmptyContractPolicy() {
    return {
      id: '',
      assetId: '', // This will be an Asset object from autocomplete
      accessPolicyId: ''
    };
  }

  openNewContractPolicyDialog() {
    this.isContractJsonComplex = false;
    this.selectedTemplate = null;

    // Prepare assets for the dialog table by adding a default operator to each
    this.assetsForDialog = this.assets.map(asset => ({
      ...asset,
      operator: 'eq' // Default operator
    }));
    this.selectedAssetsInDialog = []; // Clear previous selections
    this.newContractPolicy = this.createEmptyContractPolicy();
    // Create a default empty JSON structure
    this.expertModeJsonContent = JSON.stringify({
      '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
      '@id': `contract-def-new-${Date.now()}`,
      accessPolicyId: '', contractPolicyId: '', assetsSelector: [],
    }, null, 2);
    this.syncContractFormFromJson(); // Initialize form state from the empty JSON
    this.displayNewContractPolicyDialog = true;
  }

  hideNewContractPolicyDialog() {
    this.displayNewContractPolicyDialog = false;
  }

  onTemplateChange(event: { value: any }) {
    if (event.value) {
      this.expertModeJsonContent = JSON.stringify(event.value.content, null, 2);
    } else {
      // When clearing the template, reset to a default empty structure
      this.expertModeJsonContent = JSON.stringify({
        '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
        '@id': `contract-def-new-${Date.now()}`,
        accessPolicyId: '', contractPolicyId: '', assetsSelector: [],
      }, null, 2);
    }
    this.syncContractFormFromJson();
  }

  async onTemplateFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const file = fileList[0]; // Only one file
    try {
      this.expertModeJsonContent = await file.text();
      this.messageService.add({ severity: 'info', summary: 'Template Loaded', detail: `Template from ${file.name} loaded into editor.` });
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Read Error', detail: 'Could not read the selected file.' });
    } finally {
      element.value = ''; // Reset file input
      this.syncContractFormFromJson(); // Sync form after loading from file
    }
  }

  /**
   * Validates and saves a new Contract Definition from the dialog.
   */
  async saveNewContractDefinition() {
    try {
      const contractDefJson = JSON.parse(this.expertModeJsonContent);
      // Basic validation
      if (!contractDefJson['@id'] || !contractDefJson.accessPolicyId) {
        throw new Error("Invalid contract definition. '@id' and 'accessPolicyId' are required.");
      }
      await this.policyService.createContractDefinition(contractDefJson);
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Contract Definition created successfully.' });
      this.loadPoliciesAndDefinitions();
      this.hideNewContractPolicyDialog();
    } catch (error: any) {
      const detail = error.message.includes('JSON') ? 'Could not parse JSON.' : error.message || 'Failed to save contract definition.';
      this.messageService.add({ severity: 'error', summary: 'Error', detail });
      console.error('Failed to save contract definition:', error);
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
          const parentPolicyExists = this.allAccessPolicies.some(p => p.id === contractDefJson.accessPolicyId);
          if (!parentPolicyExists) {
            throw new Error(`Validation failed: 'accessPolicyId' (${contractDefJson.accessPolicyId}) in file does not correspond to any existing Access Policy.`);
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
          // For this simplified file upload, we only support 'eq'
          if (selector.operator !== 'eq') {
            throw new Error("Validation failed: Invalid 'operator' in contract definition. For this simple upload, it must be 'eq'.");
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

          const parentPolicy = this.allAccessPolicies.find(p => p.id === contractDefJson.accessPolicyId);
          this.allContractDefinitions.unshift({
            id: contractDefJson['@id'],
            assetId: selector.operandRight,
            bpn: parentPolicy?.bpn || 'Unknown BPN',
            accessPolicyId: contractDefJson.accessPolicyId
          });

          this.filteredContractDefinitions = [...this.allContractDefinitions];

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
      this.loadPoliciesAndDefinitions();
    }

    element.value = '';
  }

  /**
   * Toggles expert mode for the 'Edit Contract Definition' dialog, synchronizing state between modes.
   */
  toggleEditExpertMode() {
    if (this.isExpertMode) {
      // Switching from Expert to Normal
      try {
        const odrlPayload: OdrlContractDefinition = JSON.parse(this.expertModeJsonContent);
        const assetSelectors = odrlPayload.assetsSelector || [];

        const isComplex = assetSelectors.some(s => s.operandLeft !== 'https://w3id.org/edc/v0.0.1/ns/id' || !['eq', 'neq'].includes(s.operator));

        if (isComplex) {
          this.messageService.add({ severity: 'warn', summary: 'Cannot Switch to Normal Mode', detail: 'The JSON contains complex rules not supported by the Normal Mode UI.', life: 5000 });
          return;
        }

        if (this.contractPolicyToEdit) {
          const accessPolicyObject = this.allAccessPolicies.find(p => p.id === odrlPayload.accessPolicyId);
          this.contractPolicyToEdit.accessPolicyId = accessPolicyObject as any;
        }

        this.selectedAssetsInDialog = this.assetsForDialog.filter(dialogAsset => {
          const selector = assetSelectors.find(s => s.operandRight === dialogAsset.assetId);
          if (selector) {
            dialogAsset.operator = selector.operator === '=' ? 'eq' : selector.operator;
            return true;
          }
          return false;
        });
        this.isExpertMode = false;
      } catch (error) {
        this.messageService.add({ severity: 'error', summary: 'JSON Parse Error', detail: 'Could not parse JSON. Please fix errors before switching.', life: 5000 });
        return;
      }
    } else {
      // Switching from Normal to Expert
      this.generateJsonForExpertMode();
      this.isExpertMode = true;
    }
  }

  /**
   * Opens the dialog to edit an existing Contract Definition.
   */
  editContractPolicy(contractPolicy: ContractPolicy) {
     this.isExpertMode = false; // Default to normal mode
     this.isComplexSelectorForEdit = false; // Reset

     // Find the full ODRL object for expert mode
     this.contractDefinitionToEditODRL = this.allOdrlContractDefinitions.find(cd => cd['@id'] === contractPolicy.id) ?? null;

     if (!this.contractDefinitionToEditODRL) {
       this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Could not find the full contract definition to edit.' });
       return;
     }

     this.expertModeJsonContent = JSON.stringify(this.contractDefinitionToEditODRL, null, 2);

     // Check if the asset selectors are too complex for the Normal Mode UI.
     // The Normal Mode UI can only handle a list of simple asset ID selectors.
     this.isComplexSelectorForEdit = this.contractDefinitionToEditODRL.assetsSelector.some(
       selector => selector.operandLeft !== 'https://w3id.org/edc/v0.0.1/ns/id'
     );

     if (this.isComplexSelectorForEdit) {
         // If the selector logic is complex, force expert mode to avoid data loss.
         this.isExpertMode = true;
         this.messageService.add({
             severity: 'info',
             summary: 'Expert Mode Required',
             detail: 'This definition uses complex asset selection rules and can only be edited in Expert Mode.',
             life: 6000
         });
     }

     // Prepare assets for the dialog table, same as in the 'create' dialog
     this.assetsForDialog = this.assets.map(asset => ({
       ...asset,
       operator: 'eq' // Default operator
     }));

     // Pre-select the assets that are part of this contract definition
     const assetSelectors = this.contractDefinitionToEditODRL.assetsSelector || [];
     this.selectedAssetsInDialog = this.assetsForDialog.filter(dialogAsset => {
       const selector = assetSelectors.find(s => s.operandRight === dialogAsset.assetId);
       if (selector) {
         dialogAsset.operator = selector.operator === '=' ? 'eq' : selector.operator;
         return true;
       }
       return false;
     });

     const accessPolicyObject = this.allAccessPolicies.find(p => p.id === contractPolicy.accessPolicyId);

     this.contractPolicyToEdit = {
       ...contractPolicy,
       accessPolicyId: accessPolicyObject as any,
       assetId: '' // No longer used for a single asset, but keep property for model consistency
     };

     this.displayEditContractPolicyDialog = true;
  }

  /**
   * Hides the "Edit Contract Definition" dialog.
   */
  hideEditContractPolicyDialog() {
    this.displayEditContractPolicyDialog = false;
    this.contractPolicyToEdit = null;
  }

  /**
   * Saves the changes to an existing Contract Definition.
   */
  async saveEditedContractPolicy(generateJsonOnly: boolean = false) {
    if (!this.contractPolicyToEdit || !this.contractDefinitionToEditODRL) {
      if (!generateJsonOnly) {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Cannot save: Editing context is lost.' });
      }
      return;
    }

    let odrlPayload: OdrlContractDefinition;

    try {
      if (this.isExpertMode) {
        // Save from Expert Mode
        try {
          odrlPayload = JSON.parse(this.expertModeJsonContent);
          // Basic validation on the parsed JSON
          if (!odrlPayload['@id'] || odrlPayload['@id'] !== this.contractDefinitionToEditODRL['@id']) {
            if (!generateJsonOnly) {
              this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: "The '@id' in the JSON must not be changed." });
            }
            return;
          }
        } catch (error) {
          if (!generateJsonOnly) {
            this.messageService.add({ severity: 'error', summary: 'JSON Parse Error', detail: 'Could not parse the JSON content. Please check for syntax errors.' });
          }
          return;
        }
      } else {
        // Save from Normal Mode, reading from the asset table
        const accessPolicyId = (this.contractPolicyToEdit.accessPolicyId as any)?.id || this.contractPolicyToEdit.accessPolicyId;

        if (this.selectedAssetsInDialog.length === 0) {
          if (!generateJsonOnly) {
            this.messageService.add({
              severity: 'warn',
              summary: 'Validation Error',
              detail: 'You must select at least one asset.',
            });
          }
          return;
        }
        if (!accessPolicyId) {
          if (!generateJsonOnly) {
            this.messageService.add({
              severity: 'warn',
              summary: 'Validation Error',
              detail: 'An Access Policy must be selected.',
            });
          }
          return;
        }

        const assetsSelector = this.buildAssetSelectors();

        odrlPayload = {
          ...this.contractDefinitionToEditODRL, // Keep other properties
          '@id': this.contractPolicyToEdit.id,
          accessPolicyId: accessPolicyId,
          contractPolicyId: accessPolicyId, // Usually the same
          assetsSelector: assetsSelector,
        };
      }

      if (generateJsonOnly) {
        this.expertModeJsonContent = JSON.stringify(odrlPayload, null, 2);
        return;
      }

      await this.policyService.updateContractDefinition(odrlPayload);

      // Update the UI model by reloading everything to ensure data consistency
      await this.loadPoliciesAndDefinitions();

      this.messageService.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Contract Definition updated successfully.'
      });
      this.hideEditContractPolicyDialog();
    } catch (error: any) {
      if (!generateJsonOnly) {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: error.message || 'Failed to update contract definition.' });
      }
      console.error('Failed to update contract definition:', error);
    }
  }

  deleteContractPolicy(contractPolicy: ContractPolicy) {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the contract definition for asset "${contractPolicy.assetId}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await this.policyService.deleteContractDefinition(contractPolicy.id);
          this.allContractDefinitions = this.allContractDefinitions.filter(cd => cd.id !== contractPolicy.id);
          this.filteredContractDefinitions = [...this.allContractDefinitions];
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Contract Definition deleted successfully.' });
        } catch (error) {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete contract definition.' });
          console.error('Failed to delete contract definition:', error);
        }
      },
    });
  }

  /**
   * Builds the asset selector array for an ODRL payload from the dialog's selection.
   * It correctly translates the UI operator 'eq' to the ODRL-compliant '='.
   */
  private buildAssetSelectors(): OdrlCriterion[] {
    return this.selectedAssetsInDialog.map(selectedAsset => ({
      operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
      operator: 'eq', // The new UI uses a multi-select, so we default to 'eq'
      operandRight: selectedAsset.assetId,
    }));
  }

  /**
   * Refreshes the list of assets displayed within the new/edit contract definition dialogs.
   * This is called after a new asset is created to make it immediately available for selection.
   */
  private refreshAssetsForDialog(): void {
    this.assetsForDialog = this.assets.map(asset => ({
      ...asset,
      operator: this.selectedAssetsInDialog.find(s => s.assetId === asset.assetId)?.operator || 'eq'
    }));
  }

  /**
   * Generates the JSON for the expert mode editor based on the current state of the 'Edit' dialog's normal mode form.
   */
  private generateJsonForExpertMode() {
    if (this.contractPolicyToEdit && this.contractDefinitionToEditODRL) {
      const accessPolicyId = (this.contractPolicyToEdit.accessPolicyId as any)?.id || this.contractPolicyToEdit.accessPolicyId;
      const updatedOdrlPayload: OdrlContractDefinition = {
        ...this.contractDefinitionToEditODRL,
        '@id': this.contractPolicyToEdit.id,
        accessPolicyId: accessPolicyId,        contractPolicyId: accessPolicyId,        assetsSelector: this.buildAssetSelectors(),
      };
      this.expertModeJsonContent = JSON.stringify(updatedOdrlPayload, null, 2);
    }
  }

  /**
   * Updates the JSON editor content based on changes in the simple form fields.
   */
   syncJsonFromForm(): void {
     try {
       const currentJson = JSON.parse(this.expertModeJsonContent || '{}');
       const permission = currentJson.policy?.permission?.[0];

       if (permission) {
         permission.action = this.formAction;
         permission.constraint = this.formConstraints.map(c => ({
           leftOperand: c.leftOperand,
           operator: c.operator,
           rightOperand: c.rightOperand
         }));
       }

       this.expertModeJsonContent = JSON.stringify(currentJson, null, 2);
     } catch (e) {
       const policyId = this.policyToEditODRL?.['@id'] || `policy-new-${Date.now()}`;
       const odrlPayload: OdrlPolicyDefinition = {
         '@context': { odrl: 'http://www.w3.org/ns/odrl/2/' },
         '@id': policyId,
         policy: {
           permission: [{
             action: this.formAction,
             constraint: this.formConstraints.map(c => ({
               leftOperand: c.leftOperand,
               operator: c.operator,
               rightOperand: c.rightOperand
             }))
           }]
         }
       };
       this.expertModeJsonContent = JSON.stringify(odrlPayload, null, 2);
     }
   }

  /**
   * Updates the simple form fields based on the content of the JSON editor.
   */
  syncFormFromJson(): void {
    this.isPolicyJsonComplex = false; // Assume the JSON is simple until proven otherwise
    try {
      const odrlPayload: OdrlPolicyDefinition = JSON.parse(this.expertModeJsonContent || '{}');
      const permissions = odrlPayload.policy?.permission;

      // If there are no permissions or more than one, the form is considered complex or empty.
      if (!permissions || permissions.length !== 1) {
        if (permissions && permissions.length > 1) {
          this.isPolicyJsonComplex = true;
        }
        // For no permissions or multiple permissions, the simple form should be empty and disabled (if complex).
        this.formAction = 'use';
        this.formConstraints = [];
        return;
      }

      const permission = permissions[0];
      const newConstraints = permission?.constraint || [];

      // Check if the structure of the constraints has changed (number of constraints or their left operands).
      const structureChanged = newConstraints.length !== this.formConstraints.length ||
        newConstraints.some((nc, i) => nc.leftOperand !== this.formConstraints[i].leftOperand);

      this.formAction = permission.action || 'use';

      if (structureChanged) {
        // If structure is different (e.g., new template loaded), rebuild the entire form model.
        this.formConstraints = newConstraints.map((c: OdrlConstraint) => {
          const isTemplateVar = typeof c.rightOperand === 'string' && c.rightOperand === c.rightOperand.toUpperCase() && c.rightOperand.includes('_');
          return {
            label: this.formatConstraintLabel(c.leftOperand),
            leftOperand: c.leftOperand,
            operator: c.operator || 'eq',
            rightOperand: isTemplateVar ? '' : (c.rightOperand || ''),
            valueLabel: isTemplateVar ? this.formatValueLabel(c.rightOperand) : 'Value'
          };
        });
      } else {
        // If structure is the same, only update the values to prevent losing the custom label.
        newConstraints.forEach((nc, i) => {
          const isTemplateVar = typeof nc.rightOperand === 'string' && nc.rightOperand === nc.rightOperand.toUpperCase() && nc.rightOperand.includes('_');
          this.formConstraints[i].operator = nc.operator || 'eq';
          // If the value from JSON is a template variable, reset the input and update the label.
          // Otherwise, just update the input's value and leave the label as it was.
          if (isTemplateVar) {
            this.formConstraints[i].rightOperand = '';
            this.formConstraints[i].valueLabel = this.formatValueLabel(nc.rightOperand);
          } else {
            this.formConstraints[i].rightOperand = nc.rightOperand || '';
          }
        });
      }
    } catch (e) {
      // If JSON is invalid, it's "complex" and the form should be disabled.
      this.isPolicyJsonComplex = true;
      this.formAction = 'use';
      this.formConstraints = [];
    }
  }

  private formatConstraintLabel(operand: string): string {
    if (!operand) return 'Unknown Constraint';
    return operand.replace(/([A-Z])/g, ' $1').replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, (str) => str.toUpperCase()).trim();
  }

  private formatValueLabel(templateVar: string): string {
    if (!templateVar || typeof templateVar !== 'string') {
      return 'Value';
    }
    // A simple check if it's a template variable (example ALL_CAPS_WITH_UNDERSCORES)
    return templateVar
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/^\w/, c => c.toUpperCase());
  }

  onAccessPolicyTemplateChange(event: { value: any }) {
    if (event.value) {
      this.expertModeJsonContent = JSON.stringify(event.value.content, null, 2);
    } else {
      this.expertModeJsonContent = '';
    }
    this.syncFormFromJson();
  }

  async onAccessPolicyTemplateFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const file = fileList[0]; // Only one file
    try {
      this.expertModeJsonContent = await file.text();
      this.messageService.add({ severity: 'info', summary: 'Template Loaded', detail: `Template from ${file.name} loaded into editor.` });
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Read Error', detail: 'Could not read the selected file.' });
    } finally {
      element.value = ''; // Reset file input
      this.syncFormFromJson();
    }
  }

  onAssetTemplateChange(event: { value: any }) {
    if (event.value) {
      this.expertModeJsonContent = JSON.stringify(event.value.content, null, 2);
    } else {
      this.expertModeJsonContent = '';
    }
  }

  async onAssetTemplateFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const file = fileList[0];
    try {
      this.expertModeJsonContent = await file.text();
      this.messageService.add({ severity: 'info', summary: 'Template Loaded', detail: `Template from ${file.name} loaded into editor.` });
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Read Error', detail: 'Could not read the selected file.' });
    } finally {
      element.value = '';
    }
  }

  /**
   * Triggers the debounced synchronization from the JSON editor to the form fields.
   */
  syncFormFromJsonWithDebounce(): void {
    this.jsonSyncSubject.next();
  }

  /**
   * Triggers the debounced synchronization from the contract JSON editor to the form fields.
   */
  syncContractFormFromJsonWithDebounce(): void {
    this.contractJsonSyncSubject.next();
  }

  /**
   * Updates the contract definition JSON based on changes in the simple form.
   */
  syncJsonFromContractForm(): void {
    try {
      const currentJson: OdrlContractDefinition = JSON.parse(this.expertModeJsonContent || '{}');
      const accessPolicyId = (this.newContractPolicy.accessPolicyId as any)?.id || this.newContractPolicy.accessPolicyId;

      currentJson.accessPolicyId = accessPolicyId;
      currentJson.contractPolicyId = accessPolicyId; // Keep them in sync for simplicity
      currentJson.assetsSelector = this.buildAssetSelectors();

      this.expertModeJsonContent = JSON.stringify(currentJson, null, 2);
    } catch (e) {
      // Fallback for invalid JSON: create a new structure from the form state
      const accessPolicyId = (this.newContractPolicy.accessPolicyId as any)?.id || this.newContractPolicy.accessPolicyId;
      const contractDefId = `contract-def-${accessPolicyId || 'new'}-${Date.now()}`;
      const odrlPayload: OdrlContractDefinition = {
        '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
        '@id': contractDefId,
        accessPolicyId: accessPolicyId,
        contractPolicyId: accessPolicyId,
        assetsSelector: this.buildAssetSelectors(),
      };
      this.expertModeJsonContent = JSON.stringify(odrlPayload, null, 2);
    }
  }

  /**
   * Updates the simple contract definition form based on the JSON editor content.
   */
  syncContractFormFromJson(): void {
    this.isContractJsonComplex = false; // Reset
    try {
      const odrlPayload: OdrlContractDefinition = JSON.parse(this.expertModeJsonContent || '{}');
      const assetSelectors = odrlPayload.assetsSelector || [];

      // A contract is "complex" if it has more than one selector rule, or uses operators other than 'eq'.
      const isComplex = assetSelectors.length > 1 || assetSelectors.some(s => s.operator !== 'eq');

      if (isComplex) {
        this.isContractJsonComplex = true;
        return; // Don't sync to the simple form if complex
      }

      // Sync Access Policy
      const accessPolicyObject = this.allAccessPolicies.find(p => p.id === odrlPayload.accessPolicyId);
      this.newContractPolicy.accessPolicyId = accessPolicyObject as any || '';

      // Sync Asset Selection
      const selectedAssetIds = new Set(assetSelectors.map(s => s.operandRight));
      this.selectedAssetsInDialog = this.assetsForDialog.filter(asset => selectedAssetIds.has(asset.assetId));
    } catch (e) {
      this.isContractJsonComplex = true; // Invalid JSON is considered complex
    }
  }
  // View Details Methods

  hideViewDialog() {
    this.displayViewDialog = false;
    this.viewingEntityType = null;
    this.linkedAccessPolicy = null;
    this.linkedAssets = [];
    this.jsonToView = '';
  }

  viewAssetDetails(event: TableRowSelectEvent): void {
    this.viewingEntityType = 'asset';
    const asset = event.data as Asset;
    const odrlAsset = this.allOdrlAssets.find(a => a['@id'] === asset.assetId);
    if (odrlAsset) {
      this.jsonToView = JSON.stringify(odrlAsset, null, 2);
      this.viewDialogHeader = `Details for Asset: ${asset.name}`;
      this.displayViewDialog = true;
    } else {
      this.messageService.add({ severity: 'warn', summary: 'Not Found', detail: 'Could not find the full ODRL details for this asset.' });
    }
  }

  viewAccessPolicyDetails(event: TableRowSelectEvent): void {
    this.viewingEntityType = 'policy';
    const policy = event.data as AccessPolicy;
    const odrlPolicy = this.allOdrlAccessPolicies.find(p => p['@id'] === policy.id);
    if (odrlPolicy) {
      this.jsonToView = JSON.stringify(odrlPolicy, null, 2);
      this.viewDialogHeader = `Details for Access Policy: ${policy.id}`;
      this.displayViewDialog = true;
    } else {
      this.messageService.add({ severity: 'warn', summary: 'Not Found', detail: 'Could not find the full ODRL details for this policy.' });
    }
  }

  viewContractDefinitionDetails(event: TableRowSelectEvent): void {
    this.viewingEntityType = 'contract';
    const contractPolicy = event.data as ContractPolicy;
    const odrlContractDef = this.allOdrlContractDefinitions.find(cd => cd['@id'] === contractPolicy.id);

    if (odrlContractDef) {
      // For Raw JSON tab
      this.jsonToView = JSON.stringify(odrlContractDef, null, 2);
      this.viewDialogHeader = `Details for Contract Definition: ${contractPolicy.id}`;
      // For Details tab
      // First find linked Access Policy
      this.linkedAccessPolicy = this.allAccessPolicies.find(p => p.id === odrlContractDef.accessPolicyId) || null;

      // Then find linked Assets
      const assetIds = new Set(odrlContractDef.assetsSelector.map(s => s.operandRight));
      this.linkedAssets = this.assets.filter(a => assetIds.has(a.assetId));

      this.displayViewDialog = true;
    } else {
      this.messageService.add({ severity: 'warn', summary: 'Not Found', detail: 'Could not find the full ODRL details for this contract definition.' });
    }
  }
}
