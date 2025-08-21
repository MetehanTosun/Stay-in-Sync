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
import { AccessPolicy, OdrlContractDefinition, ContractPolicy, OdrlPolicyDefinition, OdrlCriterion, OdrlConstraint, OdrlPermission } from './models/policy.model';
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

  expertModeTemplateJsonContent: string = ''; // For the template editor
  expertModePolicyJsonContent: string = '';  // For the actual policy editor

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
  selectedAssetsInDialog: Asset[] = [];

  // Properties for Expert Mode in 'New Contract Definition' dialog
  isExpertMode: boolean = false;
  expertModeJsonContent: string = '';
  isComplexSelectorForEdit: boolean = false;
  contractDefinitionToEditODRL: OdrlContractDefinition | null = null;

  // Properties for Asset Templates
  assetTemplates: { name: string, content: any }[] = [];
  selectedAssetTemplate: any | null = null;

  // Properties for Access Policy Templates
  accessPolicyTemplates: { name: string, content: any }[] = [];
  selectedAccessPolicyTemplate: any | null = null;

  // Properties for the fully dynamic Access Policy form
  dynamicFormControls: {
    label: string;
    value: any;
    type: 'text' | 'dropdown';
    options?: { label: string; value: string }[];
    targetObject: any; // Direct reference to the object in the JSON structure
    targetKey: string;  // The key for the property to update
  }[] = [];
  private currentlyLoadedPolicy: OdrlPolicyDefinition | null = null;

  trackByControlLabel(index: number, control: { label: string }): string {
    return control.label;
  }

  isContractJsonComplex: boolean = false; // This is for contract definitions


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
  private templateJsonSyncSubscription: Subscription | null = null;
  private templateJsonSyncSubject = new Subject<void>();

  constructor(
    private assetService: AssetService,
    private policyService: PolicyService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private edcInstanceService: EdcInstanceService
  ) {


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

    this.templateJsonSyncSubscription = this.templateJsonSyncSubject.pipe(
      debounceTime(500)
    ).subscribe(() => {
      this.syncPolicyFromTemplate();
    });

  }

  ngOnInit(): void {
    this.loadAssets();
    this.loadPoliciesAndDefinitions();
    this.loadAccessPolicyTemplates();
    this.loadAssetTemplates();
    this.loadAllBpns();
  }

  ngOnDestroy(): void {
    this.jsonSyncSubscription?.unsubscribe();
    this.contractJsonSyncSubscription?.unsubscribe();
    this.templateJsonSyncSubscription?.unsubscribe();
  }


  private loadAccessPolicyTemplates() {

    this.accessPolicyTemplates = [


      {
        name: 'BPN Access Policy',
        content: {
          "@context": [
            "http://www.w3.org/ns/odrl.jsonld",
            "https://w3id.org/catenax/2025/9/policy/context.jsonld"
          ],
          "@type": "Set",
          "@id": "POLICY_ID_BPN",
          "permission": [
            {
              "action": "${Action|access,read,write,use}",
              "constraint": [
                {
                  "and": [
                    {
                      "leftOperand": "Membership",
                      "operator": "${Operator|eq,neq}",
                      "rightOperand": "${Status|active,inactive}"
                    },
                    {
                      "leftOperand": "BusinessPartnerNumber",
                      "operator": "${Operator|isAnyOf,eq,neq}",
                      "rightOperand": [
                        "${BPN-Value}"
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      },

      {
        name: 'Access Policy',
        content: {
          "@context": {"odrl": "http://www.w3.org/ns/odrl/2/"},
          "@id": "POLICY_ID_BPN",
          "policy": {
            "permission": [{
              "action": "${Action|use,read,write}",
              "constraint": [{
                "leftOperand": "BusinessPartnerNumber",
                "operator": "${Operator|eq,neq}",
                "rightOperand": "${BPN-Value}"
              }]
            }]
          }
        }
      },

/*
      {
        name: 'Framework Agreement Policy (Traceability)',
        content: {
          "@context": { "odrl": "http://www.w3.org/ns/odrl/2/" },
          "@id": "POLICY_ID_TRACEABILITY",
          "policy": {
            "permission": [{
              "action": "${Action|use}",
              "constraint": [{
                "leftOperand": "FrameworkAgreement.traceability",
                "operator": "${Operator|eq}",
                "rightOperand": "${Status|active,inactive}"
              }]
            }]
          }
        }
      },

*/
      /*
      {
        name: 'Multi-Constraint BPN Policy',
        content: {
          "@context": { "odrl": "http://www.w3.org/ns/odrl/2/" },
          "@id": "POLICY_ID_MULTI_CONSTRAINT",
          "policy": {
            "permission": [{
              "action": "${Action|use,read}",
              "constraint": [
                {"leftOperand": "BusinessPartnerNumber",
                  "operator": "eq",
                  "rightOperand": "${BPN-Value}"},
                {"leftOperand": "Membership",
                  "operator": "eq",
                  "rightOperand": "${Status|active}"}
              ]
            }]
          }
        }
      }
*/

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
    this.policyToEditODRL = null; // Ensure we are in "create" mode for saving
    this.expertModeTemplateJsonContent = ''; // Clear template editor initially
    this.expertModePolicyJsonContent = ''; // Clear policy editor initially

    // Set "BPN Access Policy" as the default template
    const defaultTemplate = this.accessPolicyTemplates.find(t => t.name === 'BPN Access Policy');
    if (defaultTemplate) {
      this.selectedAccessPolicyTemplate = defaultTemplate;
      // This will set the JSON content and sync the form
      this.onAccessPolicyTemplateChange({ value: this.selectedAccessPolicyTemplate });
    } else {
      // Fallback if template is not found
      this.expertModeTemplateJsonContent = JSON.stringify({}, null, 2);
      this.expertModePolicyJsonContent = JSON.stringify({}, null, 2);
      this.syncFormFromJson();
    }

    this.displayNewAccessPolicyDialog = true;
  }

  hideNewAccessPolicyDialog() {
    this.displayNewAccessPolicyDialog = false;
  }

  async saveNewAccessPolicy() {
    let policyJson: OdrlPolicyDefinition;
    try {
      policyJson = JSON.parse(this.expertModePolicyJsonContent); // Save from the policy editor
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
      this.expertModePolicyJsonContent = JSON.stringify(this.policyToEditODRL, null, 2); // Load actual policy
      this.expertModeTemplateJsonContent = ''; // Clear template editor, user can select one if needed
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
    try { // Save from the policy editor
      const policyJson = JSON.parse(this.expertModePolicyJsonContent);
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

    const validActions = ['use', 'read', 'write'];
    const validOperators = ['eq', 'neq'];


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
      accessPolicyId: '',
      contractPolicyId: '',
      assetsSelector: [],
    }, null, 2);
    this.syncContractFormFromJson(); // Initialize form state from the empty JSON

    this.displayNewContractPolicyDialog = true;
  }

  hideNewContractPolicyDialog() {
    this.displayNewContractPolicyDialog = false;
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
      this.messageService.add({ severity: 'info', summary: 'Content Loaded', detail: `JSON from ${file.name} loaded into editor.` });
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
      operator: 'eq' // Default to 'eq' as the selection list no longer holds operator info
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
    if (!this.currentlyLoadedPolicy) return; // Ensure we have a policy object to modify

    // Update the in-memory JSON object directly through the references stored in the controls
    this.dynamicFormControls.forEach(control => {
      control.targetObject[control.targetKey] = control.value;
    });

    // Re-stringify the updated JSON object into the editor
    this.expertModePolicyJsonContent = JSON.stringify(this.currentlyLoadedPolicy, null, 2);
  }

  /**
   * Updates the simple form fields based on the content of the JSON editor.
   * This now takes into account both the template JSON and the actual policy JSON.
   */
  syncFormFromJson(): void {
     this.dynamicFormControls = []; // Reset the form
     this.currentlyLoadedPolicy = null; // Reset before parsing

     try {
       const templatePolicy: OdrlPolicyDefinition = JSON.parse(this.expertModeTemplateJsonContent || '{}');
       const actualPolicy: OdrlPolicyDefinition = JSON.parse(this.expertModePolicyJsonContent || '{}');
       this.currentlyLoadedPolicy = actualPolicy; // Set this early for two-way binding

       // Handle different root structures for permissions (some templates have `policy.permission`, others just `permission`)
       const templatePermissions = templatePolicy.policy?.permission || templatePolicy.permission;
       const actualPermissions = actualPolicy.policy?.permission || actualPolicy.permission;

       // If no template is provided, or if the template structure is invalid, try to parse the actual policy as a static form
       if (!templatePermissions || !Array.isArray(templatePermissions) || templatePermissions.length !== 1) {
         if (actualPermissions && Array.isArray(actualPermissions) && actualPermissions.length === 1) {
           const permission = actualPermissions[0];
           this.processJsonNodeForForm(permission, permission, 'action', 'Action');
           const constraints = this.extractConstraints(permission);
           constraints.forEach(constraint => {
             const groupLabel = this.formatConstraintLabel(constraint.leftOperand);
             this.processJsonNodeForForm(constraint, constraint, 'operator', `${groupLabel} Operator`);
             this.processJsonNodeForForm(constraint, constraint, 'rightOperand', groupLabel);
           });
         }
         return; // Exit if no valid template to work from
       }

       // Check for basic structural compatibility between template and actual policy
       if (!actualPermissions || !Array.isArray(actualPermissions) || actualPermissions.length !== 1) {
         return;
       }

       const templatePermission = templatePermissions[0];
       const actualPermission = actualPermissions[0];

       this.processJsonNodeForForm(templatePermission, actualPermission, 'action', 'Action');

       const templateConstraints = this.extractConstraints(templatePermission);
       const actualConstraints = this.extractConstraints(actualPermission);

       // Check for constraint array compatibility
       if (!Array.isArray(templateConstraints) || !Array.isArray(actualConstraints) || templateConstraints.length !== actualConstraints.length) {
         return;
       }

       // Process each constraint based on the template structure
       templateConstraints.forEach((templateConstraint, index) => {
         const actualConstraint = actualConstraints[index]; // Assuming constraints are in same order
         if (!actualConstraint || templateConstraint.leftOperand !== actualConstraint.leftOperand) {
           return; // Skip incompatible constraint
         }
         const groupLabel = this.formatConstraintLabel(templateConstraint.leftOperand);
         this.processJsonNodeForForm(templateConstraint, actualConstraint, 'operator', `${groupLabel} Operator`);
         this.processJsonNodeForForm(templateConstraint, actualConstraint, 'rightOperand', groupLabel);
       });

     } catch (e) {
       // On JSON parse error, the form will be empty, which is correct.
       this.currentlyLoadedPolicy = null; // Ensure it's null on error
       this.dynamicFormControls = [];
     }
  }

  // Function to extract constraints, handling nested 'and'/'or'
  private extractConstraints(permission: OdrlPermission): any[] {
    if (!permission || !permission.constraint || !Array.isArray(permission.constraint)) {
      return [];
    }
    const constraints = permission.constraint;
    // Check for nested 'and' or 'or' in the first element of the constraint array
    if (constraints.length === 1 && (constraints[0].and || constraints[0].or)) {
      return constraints[0].and || constraints[0].or || [];
    }
    return constraints; // Return the flat array if not nested
  }

  // Process nodes for form generation
  private processJsonNodeForForm(templateObject: any, actualObject: any, key: string, defaultLabel: string) {
    if (!(key in templateObject) || !(key in actualObject)) return;

    const templateValue = templateObject[key];
    const actualValue = actualObject[key];

    // Handle case where the template value is an array containing a single placeholder string
    if (Array.isArray(templateValue) && templateValue.length === 1 && typeof templateValue[0] === 'string') {
      const parsed = this.parseTemplateVariable(templateValue[0]);
      if (parsed) {
        this.dynamicFormControls.push({
          ...parsed,
          value: Array.isArray(actualValue) ? actualValue[0] : '',
          targetObject: actualValue, // The target is the array itself
          targetKey: '0',            // The key is the index '0'
        });
      }
    } else if (typeof templateValue === 'string') {
      // Original logic for string-based template values
      const parsed = this.parseTemplateVariable(templateValue);
      if (parsed) {
        this.dynamicFormControls.push({
          ...parsed,
          value: actualValue,
          targetObject: actualObject,
          targetKey: key,
        });
      }
    }
  }

  /**
   * Regenerates the policy JSON from the template JSON.
   * This is triggered when the template editor's content changes.
   */
  syncPolicyFromTemplate(): void {
    try {
      const templateContent = JSON.parse(this.expertModeTemplateJsonContent);
      const newPolicyContent = JSON.parse(JSON.stringify(templateContent)); // Deep copy
      this.replaceTemplatePlaceholders(newPolicyContent);
      this.expertModePolicyJsonContent = JSON.stringify(newPolicyContent, null, 2);
    } catch (e) {
      // If template JSON is invalid, clear the generated policy.
      this.expertModePolicyJsonContent = '{}';
    } finally {
      // Always attempt to sync the form, which will either show the new state or an empty form on error.
      this.syncFormFromJson();
    }
  }


  private parseTemplateVariable(variable: string): { label: string, type: 'text' | 'dropdown', options?: {label: string, value: string}[] } | null {
    if (typeof variable !== 'string' || !variable.startsWith('${') || !variable.endsWith('}')) {
      return null;
    }

    const content = variable.substring(2, variable.length - 1);
    const parts = content.split('|');
    const label = parts[0].replace(/-/g, ' ').replace(/^\w/, c => c.toUpperCase());

    if (parts.length > 1) {
      // Dropdown: e.g., ${Action|use,read}
      const options = parts[1].split(',').map(opt => ({ label: opt.charAt(0).toUpperCase() + opt.slice(1), value: opt }));
      return { label, type: 'dropdown', options };
    } else {
      // Text input: e.g., ${BPN-Number}
      return { label, type: 'text' };
    }
  }

  private formatConstraintLabel(operand: string): string {
    if (!operand) return 'Unknown Constraint';
    return operand.replace(/([A-Z])/g, ' $1').replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, (str) => str.toUpperCase()).trim();
  }


  onAccessPolicyTemplateChange(event: { value: any }) {
    if (event.value) {
      const templateContent = event.value.content;
      this.expertModeTemplateJsonContent = JSON.stringify(templateContent, null, 2);

      // Generate initial policy JSON from template, replacing placeholders with empty strings or defaults
      const initialPolicyContent = JSON.parse(JSON.stringify(templateContent)); // Deep copy
      this.replaceTemplatePlaceholders(initialPolicyContent); // Helper to replace ${...} with ""
      this.expertModePolicyJsonContent = JSON.stringify(initialPolicyContent, null, 2);

    } else {
      this.expertModeTemplateJsonContent = '';
      this.expertModePolicyJsonContent = ''; // Clear policy JSON too
    }
    this.syncFormFromJson();
  }

  // Replace placeholders in a JSON object
  private replaceTemplatePlaceholders(obj: any) {
    for (const key in obj) {
      if (typeof obj[key] === 'string') {
        if (obj[key].startsWith('${') && obj[key].endsWith('}')) {
          const parsed = this.parseTemplateVariable(obj[key]);
          if (parsed?.type === 'dropdown' && parsed.options && parsed.options.length > 0) {
            obj[key] = parsed.options[0].value; // Default to first option for dropdowns
          } else {
            obj[key] = ''; // Default to empty string for text fields
          }
        }
      } else if (typeof obj[key] === 'object' && obj[key] !== null) {
        this.replaceTemplatePlaceholders(obj[key]);
      }
    }
  }

  async onAccessPolicyTemplateFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const file = fileList[0]; // Only one file
    try {
      const templateContent = await file.text();
      this.expertModeTemplateJsonContent = templateContent; // Load template JSON

      // Generate initial policy JSON from template, replacing placeholders
      const initialPolicyContent = JSON.parse(templateContent); // Parse template
      this.replaceTemplatePlaceholders(initialPolicyContent); // Replace placeholders
      this.expertModePolicyJsonContent = JSON.stringify(initialPolicyContent, null, 2); // Set policy JSON

      this.messageService.add({ severity: 'info', summary: 'Template Loaded', detail: `Template from ${file.name} loaded into editor.` });
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Read Error', detail: 'Could not read the selected file.' });
      this.expertModeTemplateJsonContent = ''; // Clear on error
      this.expertModePolicyJsonContent = ''; // Clear on error
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
   * Triggers the debounced synchronization from the template JSON editor to the generated policy editor.
   */
  syncPolicyFromTemplateWithDebounce(): void {
    this.templateJsonSyncSubject.next();
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
      // If JSON is invalid (e.g., during manual editing), do nothing to avoid overwriting user's work.
      console.warn('syncJsonFromContractForm: Could not parse JSON, aborting sync to prevent data loss.');
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

      // A contract is "complex" if it contains criteria that are not simple 'eq' or 'in' selectors on asset ID
      const isComplex = assetSelectors.some(s =>
        s.operandLeft !== 'https://w3id.org/edc/v0.0.1/ns/id' || !['eq', 'in'].includes(s.operator)
      );

      if (isComplex) {
        this.isContractJsonComplex = true;
        // When complex, clear the simple form to avoid confusion
        this.newContractPolicy.accessPolicyId = '';
        this.selectedAssetsInDialog = [];
        return;
      }

      // Sync Access Policy
      const accessPolicyObject = this.allAccessPolicies.find(p => p.id === odrlPayload.accessPolicyId);
      this.newContractPolicy.accessPolicyId = accessPolicyObject as any || '';

      // Sync Asset Selection
      const selectedAssetIds = new Set<string>();
      assetSelectors.forEach(selector => {
        if (selector.operator === 'eq' && typeof selector.operandRight === 'string') {
          selectedAssetIds.add(selector.operandRight);
        } else if (selector.operator === 'in' && Array.isArray(selector.operandRight)) {
          selector.operandRight.forEach(id => selectedAssetIds.add(id as string));
        }
      });
      this.selectedAssetsInDialog = this.assets.filter(asset => selectedAssetIds.has(asset.assetId));
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
