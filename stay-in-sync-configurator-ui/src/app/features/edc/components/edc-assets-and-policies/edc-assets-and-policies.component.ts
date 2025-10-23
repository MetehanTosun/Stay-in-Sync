import {Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
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
import { forkJoin } from 'rxjs';


// App imports
import { Asset } from './models/asset.model';
import { OdrlContractDefinition, OdrlPolicyDefinition, OdrlCriterion, OdrlConstraint, OdrlPermission, UiContractDefinition } from './models/policy.model';
import { EdcInstance } from '../edc-instances/models/edc-instance.model';
import { AssetService } from './services/asset.service';
import { PolicyService } from './services/policy.service';
import { EdcInstanceService } from '../edc-instances/services/edc-instance.service';
import {lastValueFrom, Subject, Subscription} from 'rxjs';
import {debounceTime, tap} from "rxjs/operators";
import { MultiSelectModule } from 'primeng/multiselect';
import { Transformation } from '../../models/transformation.model';
import { Template } from '../templates/models/template.model';
import { TemplateService } from '../templates/services/template.service';

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
    MultiSelectModule,
  ],
  templateUrl: './edc-assets-and-policies.component.html',
  styleUrls: ['./edc-assets-and-policies.component.css'],
  providers: [ConfirmationService, MessageService],
})
export class EdcAssetsAndPoliciesComponent implements OnInit, OnDestroy {
  @ViewChild('dtAssets') dtAssets!: Table;
  @ViewChild('dtPolicies') dtPolicies!: Table;
  @ViewChild('dtContracts') dtContracts!: Table;

  @Input({ required: true }) instance!: EdcInstance;
  @Output() back = new EventEmitter<void>();

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
  contractDefId: string = ''; // Hinzugefügt: Feld für Contract Definition ID

  newContractPolicy: {
    id: string;
    assetId: string;
    accessPolicyId: string; // The ID of the parent Access Policy
  } = this.createEmptyContractPolicy();

  displayEditContractPolicyDialog: boolean = false;
contractPolicyToEdit: OdrlContractDefinition | null = null;


  displayEditAccessPolicyDialog: boolean = false;
  policyToEditODRL: OdrlPolicyDefinition | null = null;

  expertModeTemplateJsonContent: string = ''; // For the template editor
  expertModePolicyJsonContent: string = '';  // For the actual policy editor

  allAccessPolicies: OdrlPolicyDefinition[] = [];
allOdrlAccessPolicies: OdrlPolicyDefinition[] = [];
filteredAccessPolicies: OdrlPolicyDefinition[] = [];

allOdrlContractDefinitions: OdrlContractDefinition[] = [];
allContractDefinitions: UiContractDefinition[] = [];
filteredContractDefinitions: UiContractDefinition[] = [];

  // Properties for the 'New Contract Definition' dialog
  assetsForDialog: (Asset & { operator: string })[] = [];
  selectedAssetsInDialog: Asset[] = [];

  // Properties for Expert Mode in 'New Contract Definition' dialog
  isExpertMode: boolean = false;
  expertModeJsonContent: string = '';
  isComplexSelectorForEdit: boolean = false;
  contractDefinitionToEditODRL: OdrlContractDefinition | null = null;

  // Properties for Access Policy Templates
  accessPolicyTemplates: Template[] = [];
  selectedAccessPolicyTemplate: Template | null = null;
  showTemplateJson = false;

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

  // Properties for Asset Templates
  assetTemplates: { name: string; content: any }[] = [];

  trackByControlLabel(index: number, control: { label: string }): string {
    return control.label;
  }

  trackByIndex(index: number, item: any): number {
    return index;
  }

  getTargetSystemName(targetSystemId: number | undefined): string {
    if (!targetSystemId) return 'N/A';
    const system = this.allSelectableSystems.find(s => s.id === String(targetSystemId));
    return system?.alias || `ID: ${targetSystemId}`;
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
 linkedAccessPolicy: OdrlPolicyDefinition | null = null;
  linkedAssets: Asset[] = [];

  // Real-time sync from JSON editor to form
  private jsonSyncSubscription: Subscription | null = null;
  private jsonSyncSubject = new Subject<void>();
  private contractJsonSyncSubscription: Subscription | null = null;
  private contractJsonSyncSubject = new Subject<void>();
  private templateJsonSyncSubscription: Subscription | null = null;
  private templateJsonSyncSubject = new Subject<void>();
  private assetJsonSyncSubscription: Subscription | null = null;
  private assetJsonSyncSubject = new Subject<void>();
  private isSyncingFromForm = false; // Flag to prevent feedback loop

  // New Asset Dialog specific properties
  allSelectableSystems: Transformation[] = [];
  selectedSystem: Transformation | null = null;
  isManualAssetCreation = false; // Flag to control manual creation mode
  manualBaseUrl: string = ''; // For the manual creation URL field
  assetAttributes: { key: string; value: string }[] = [{ key: '', value: '' }]; // Start with one empty row
  pathParamId: string = '';
  queryParams: { key: string; value: string }[] = [{ key: '', value: '' }];
  headerParams: { key: string; value: string }[] = [{ key: '', value: '' }];

  constructor(
    private assetService: AssetService,
    private policyService: PolicyService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private edcInstanceService: EdcInstanceService,
    private templateService: TemplateService
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

    this.assetJsonSyncSubscription = this.assetJsonSyncSubject.pipe(
      debounceTime(500)
    ).subscribe(() => {
      this.syncAssetFormFromJson();
    });

  }

  ngOnInit(): void {
    // Überprüfen, ob instance.id verfügbar ist, bevor wir Assets laden
    if (this.instance && this.instance.id) {
      console.log('Initialisiere mit EDC-ID:', this.instance.id);
      this.loadAssets();
      this.loadPoliciesAndDefinitions();
    } else {
      console.error('Keine gültige EDC-Instance ID gefunden:', this.instance);
      this.messageService.add({
        severity: 'error',
        summary: 'Fehler',
        detail: 'EDC-Instance nicht korrekt initialisiert.'
      });
    }

    this.loadAccessPolicyTemplates();
    this.loadAssetTemplates();
    this.loadSelectableSystems();
  }

  ngOnDestroy(): void {
    this.jsonSyncSubscription?.unsubscribe();
    this.contractJsonSyncSubscription?.unsubscribe();
    this.templateJsonSyncSubscription?.unsubscribe();
    this.assetJsonSyncSubscription?.unsubscribe();
  }


  private loadAccessPolicyTemplates() {
    this.templateService.getTemplates().subscribe(allTemplates => {
      this.accessPolicyTemplates = allTemplates;
    });
  }



  private loadAssetTemplates() {
    // Mock data for asset templates
    this.assetTemplates = [
      {
        name: 'Standard HTTP Data Asset',
        content: {
          "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
          "@id": "",
          "properties": {
            "asset:prop:name": "",
            "asset:prop:description": "",
            "asset:prop:contenttype": "application/json",
            "asset:prop:version": "1.0.0"
          },
          "dataAddress": {
            "type": "HttpData",
            "baseUrl": "https://example.com" // Korrigiert auf baseUrl statt base_url mit einem gültigen Beispielwert
          }
        }
      }
      ,
    ];
  }

  private loadSelectableSystems() {
    this.assetService.getSelectableSystems().subscribe(systems => {
      this.allSelectableSystems = systems;
    });
  }

  addQueryParam() {
    this.queryParams.push({ key: '', value: '' });
  }

  removeQueryParam(index: number) {
    this.queryParams.splice(index, 1);
    this.syncAssetJsonFromForm();
  }

  addHeaderParam() {
    this.headerParams.push({ key: '', value: '' });
  }

  removeHeaderParam(index: number) {
    this.headerParams.splice(index, 1);
    this.syncAssetJsonFromForm();
  }

  goBack(): void {
    this.back.emit();
  }

  /**
   * Hilfsfunktion, die sicherstellt, dass die instance.id als String zurückgegeben wird
   */
  private getInstanceId(): string {
    if (this.instance && this.instance.id !== null) {
      return String(this.instance.id);
    }
    throw new Error("EDC-Instance ID ist nicht verfügbar");
  }

  private async loadAssets(): Promise<void> {
    this.assetLoading = true;
    try {
      console.log('Lade Assets für EDC-ID:', this.instance.id);
      console.log('Full EDC instance:', this.instance);
      const response = await lastValueFrom(this.assetService.getAssets(this.getInstanceId()));
      console.log('Assets geladen:', response);
      console.log('Anzahl geladener Assets:', response.length);
      this.assets = response;
      // Halte zusätzlich die rohen ODRL-Assets, falls Service sie bereitstellt
      if ((response as any).__rawList) {
        this.allOdrlAssets = (response as any).__rawList;
      }

      // Falls ein Dialog geöffnet ist, Liste aktualisieren
      if (this.displayNewContractPolicyDialog || this.displayEditContractPolicyDialog) {
        this.refreshAssetsForDialog();
      }
    } catch (error) {
      console.error('Failed to load assets:', error);
      // Detailliertere Fehlermeldung anzeigen
      let errorMessage = 'Could not load assets.';
      if (error instanceof Error) {
        errorMessage += ` Details: ${error.message}`;
      } else if (typeof error === 'object' && error !== null) {
        errorMessage += ` Details: ${JSON.stringify(error)}`;
      }

      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: errorMessage
      });
    } finally {
      this.assetLoading = false;
    }
  }


  loadPoliciesAndDefinitions() {
  this.policyLoading = true;

    forkJoin({
    accessPolicies: this.policyService.getPolicies(this.getInstanceId()),
    contractDefinitions: this.policyService.getContractDefinitions(this.getInstanceId())
    }).subscribe({
    next: ({ accessPolicies, contractDefinitions }) => {
      // Policy-Map für schnellen Lookup (ODRL-ID => Policy)
      const policyMap = new Map<string, OdrlPolicyDefinition>();
      accessPolicies.forEach(p => {
        // Verwende policyId (oder @id), wenn verfügbar
        if (p.policyId) {
          policyMap.set(p.policyId, p);
        } else if (p['@id']) {
          policyMap.set(p['@id'], p);
        }
      });

      // Contract Definitions verknüpfen mit Policies und auf UiContractDefinition mappen
      // Store the full ODRL contract definitions for later use in edit/view dialogs
      this.allOdrlContractDefinitions = contractDefinitions;

  this.allContractDefinitions = contractDefinitions.map((cd): UiContractDefinition => {
        console.log("Converting ContractDefinition to UI format:", cd); // Log to see what's coming from backend
        
        // Stelle sicher, dass wir die numerische DB-ID und contractDefinitionId haben
        const dbId = cd.id || (cd as any).dbId || null;
        const contractDefinitionId = cd['@id'] || (cd as any).contractDefinitionId || null;
        
        console.log(`ContractDefinition mapping - DB ID: ${dbId}, contractDefinitionId: ${contractDefinitionId}`);
        
        const candidates: string[] = [
          cd.accessPolicyId as any,
          (cd as any).accessPolicyIdStr,
          (cd as any).policyId,
          (cd as any).dbId,
          cd['@id'] as any
        ].filter(Boolean);

        let parentPolicy: any | undefined;
        for (const key of candidates) {
          parentPolicy = policyMap.get(key) || accessPolicies.find(p => p.dbId === key);
          if (parentPolicy) break;
        }

        const assetIds = (cd.assetsSelector && cd.assetsSelector.length)
          ? cd.assetsSelector.map((s: any) => s.operandRight).join(', ')
          : (cd as any).assetId || 'Unknown Asset';
        
        return {
          id: dbId,                   // Speichere die DB-ID für DELETE-Operationen
          contractDefinitionId,      // Speichere die Contract Definition ID für Anzeige
          assetId: assetIds,
          bpn: parentPolicy?.bpn || 'Unknown BPN',
          accessPolicyId: parentPolicy?.policyId || parentPolicy?.['@id'] || parentPolicy?.dbId || ''
        };
      });

  this.filteredContractDefinitions = [...this.allContractDefinitions];

      // Policies direkt in State übernehmen
      this.allAccessPolicies = accessPolicies;
      this.filteredAccessPolicies = [...this.allAccessPolicies];
    },
      error: (error: any) => {
      console.error('Failed to load policies:', error);
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Failed to load policies.'
      });
    },
    complete: () => {
      this.policyLoading = false;
    }
  });
}


  onGlobalFilter(event: Event, table: Table) {
    const inputElement = event.target as HTMLInputElement;
    table.filterGlobal(inputElement.value, 'contains');
  }

  onGlobalPolicySearch(event: Event): void {
  const searchTerm = (event.target as HTMLInputElement).value.toLowerCase();

  if (!searchTerm) {
    this.filteredAccessPolicies = [...this.allAccessPolicies];
    return;
  }

  this.filteredAccessPolicies = this.allAccessPolicies.filter(policy => {
    const parts: string[] = [];

    // Top-Level
    if (policy.policyId) parts.push(policy.policyId);
    if (policy['@id']) parts.push(policy['@id']);
    if (policy.dbId) parts.push(policy.dbId);
    if (policy.bpn) parts.push(policy.bpn);

    // Permissions + Constraints
    const permissions = policy.permission || policy.policy?.permission || [];
    permissions.forEach(p => {
      if (p.action) parts.push(p.action);
      p.constraint?.forEach(c => {
        if (c.leftOperand) parts.push(c.leftOperand);
        if (c.operator) parts.push(c.operator);
        if (c.rightOperand) {
          if (Array.isArray(c.rightOperand)) {
            parts.push(...c.rightOperand);
          } else {
            parts.push(c.rightOperand);
          }
        }
      });
    });

    const searchableText = parts.join(' ').toLowerCase();
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
    this.isManualAssetCreation = false; // Ensure we are in guided mode
    // Always start with a completely clean slate by resetting all form fields
    this.resetAssetFormFields();

    // Find the default template, which is the first in the list
    const defaultTemplate = this.assetTemplates.length > 0 ? this.assetTemplates[0] : null;
    if (defaultTemplate) {
      // Create a deep copy to avoid modifying the master template object
      const templateContent = JSON.parse(JSON.stringify(defaultTemplate.content));

      // Lasse die ID leer, damit der Nutzer sie selbst eingeben kann
      templateContent['@id'] = "";

      this.expertModeJsonContent = JSON.stringify(templateContent, null, 2);
      this.populateAssetFormFromDataAddress(templateContent.dataAddress || {});
    } else {
      // Fallback if no templates are loaded, create an empty form
      this.syncAssetJsonFromForm();
    }
    this.displayNewAssetDialog = true;
  }

  openManualAssetDialog() {
    this.isManualAssetCreation = true; // Set the flag for manual mode
    this.resetAssetFormFields();

    // Use the same logic as the guided creation to provide a default template
    const defaultTemplate = this.assetTemplates.length > 0 ? this.assetTemplates[0] : null;
    if (defaultTemplate) {
      const templateContent = JSON.parse(JSON.stringify(defaultTemplate.content));
      templateContent['@id'] = ""; // Start with a blank ID
      // Setze die manualBaseUrl aus dem Template
      if (templateContent.dataAddress && templateContent.dataAddress.baseUrl) {
        this.manualBaseUrl = templateContent.dataAddress.baseUrl;
      } else {
        this.manualBaseUrl = "https://example.com"; // Standard-Wert
      }
      this.expertModeJsonContent = JSON.stringify(templateContent, null, 2);
    } else {
      this.expertModeJsonContent = '{}'; // Fallback to an empty object
      this.manualBaseUrl = "https://example.com"; // Standard-Wert
    }
    this.displayNewAssetDialog = true;
  }

  // New Asset Dialog specific methods
  onSystemSelect(selectedSystem: Transformation | null) {
    if (selectedSystem && selectedSystem.id) {
      // Use forkJoin to fetch both the request config and the asset properties in parallel
      forkJoin({
        arcConfig: this.assetService.getTargetArcConfig(selectedSystem.id),
        syncedAsset: this.assetService.getSyncedAsset(selectedSystem.id)
      }).subscribe(({ arcConfig, syncedAsset }) => {
        // Pass both responses to the populating function
        this.populateFormFromSystemSelection(arcConfig, syncedAsset);
        // Sync the form to the JSON editor after populating
        this.syncAssetJsonFromForm(); 
      });
    } else {
      this.resetAssetFormFields();
      this.syncAssetJsonFromForm();
    }
  }

  hideNewAssetDialog() {
    this.displayNewAssetDialog = false;
  }

  async saveNewAsset() {
  try {
    const assetJson = JSON.parse(this.expertModeJsonContent);

    // Final validation before sending
    if (!assetJson['@id'] || assetJson['@id'].trim() === '') {
      throw new Error('Bitte eine Asset-ID eingeben. Dies ist ein Pflichtfeld.');
    }

    // The service layer requires the targetEDCId to be set.
    assetJson.targetEDCId = this.getInstanceId();

    // The createAsset service method expects assetId to be a top-level property for the mock to work correctly.
    assetJson.assetId = assetJson['@id'];
    
    // Sicherstellen, dass die Datenstruktur korrekt ist
    if (!assetJson.dataAddress) {
      assetJson.dataAddress = { type: 'HttpData' };
    }
    
    // Sicherstellen, dass baseUrl und nicht base_url verwendet wird
    if (assetJson.dataAddress.base_url && !assetJson.dataAddress.baseUrl) {
      assetJson.dataAddress.baseUrl = assetJson.dataAddress.base_url;
      delete assetJson.dataAddress.base_url;
    }
    
    // Validiere, dass baseUrl eine gültige URL ist
    if (assetJson.dataAddress.baseUrl) {
      try {
        // Versuche, die URL zu validieren
        if (!assetJson.dataAddress.baseUrl.startsWith('http://') && 
            !assetJson.dataAddress.baseUrl.startsWith('https://')) {
          // Füge http:// hinzu, wenn kein Protokoll angegeben ist
          assetJson.dataAddress.baseUrl = 'http://' + assetJson.dataAddress.baseUrl;
        }
        new URL(assetJson.dataAddress.baseUrl);
      } catch (e) {
        throw new Error('Die angegebene baseUrl ist keine gültige URL. Bitte geben Sie eine vollständige URL ein (z.B. https://example.com).');
      }
    }
    
    // Sicherstellen, dass die Pflichtfelder in properties vorhanden sind
    if (!assetJson.properties['asset:prop:name']) {
      assetJson.properties['asset:prop:name'] = assetJson['@id']; // Fallback-Name
    }
    if (!assetJson.properties['asset:prop:contenttype']) {
      assetJson.properties['asset:prop:contenttype'] = 'application/json'; // Standard-Contenttype
    }

    console.log('Sending Asset to Backend:', JSON.stringify(assetJson, null, 2));
    try {
      const response = await lastValueFrom(this.assetService.createAsset(this.getInstanceId(), assetJson));
      console.log('Asset successfully created:', response);
      this.messageService.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Asset created successfully.'
      });
      this.hideNewAssetDialog();
      await this.loadAssets();
    } catch (err: any) {
      console.error('Error creating asset:', err);
      // Detailliertere Fehlermeldung
      let detail = 'Failed to save asset.';

      if (err.error) {
        if (err.error.message) {
          detail = `Server error: ${err.error.message}`;
        } else if (err.error.detail) {
          detail = `Server error: ${err.error.detail}`;
        } else if (typeof err.error === 'string') {
          detail = `Server error: ${err.error}`;
        }
        console.log('Vollständige Fehlerdetails:', err.error);
      } else if (err.message) {
        detail = err.message;
      } else if (typeof err === 'object') {
        detail = JSON.stringify(err);
      }

      // Speziellen Hinweis geben, wenn es ein 500er Fehler ist
      if (err.status === 500) {
        detail += ' (Status 500: Interner Serverfehler. Das Asset-Format könnte nicht den Server-Anforderungen entsprechen.)';
      }

      this.messageService.add({ severity: 'error', summary: 'Error', detail });
    }
  } catch (error: any) {
    console.error('Failed to save asset:', error);

    // Verbesserte Fehlerbehandlung mit mehr Details
    let detail = 'Failed to save asset.';

    if (error.message && error.message.includes('JSON')) {
      detail = 'Could not parse JSON. Please check the format of your asset data.';
    } else if (error.message) {
      detail = error.message;
    }

    this.messageService.add({ severity: 'error', summary: 'Error', detail });
  }
}


  addAssetAttribute() {
    this.assetAttributes.push({ key: '', value: '' });
    this.syncAssetJsonFromForm();
  }

  removeAssetAttribute(index: number) {
    this.assetAttributes.splice(index, 1);
    this.syncAssetJsonFromForm();
  }

  /**
   * Syncs the asset form fields (endpoint, attributes, parameters) to the expertModeJsonContent.
   * This is the primary mechanism for the form to update the JSON editor for assets.
   */
  syncAssetJsonFromForm(): void {
    // Set a flag to prevent the JSON editor's change event from re-triggering a form sync
    this.isSyncingFromForm = true;

    let currentAssetJson: any;
    try {
      currentAssetJson = JSON.parse(this.expertModeJsonContent || '{}');
    } catch (e) {
      // If JSON is invalid, don't try to parse it, just use a basic structure
      currentAssetJson = {};
    }

    // Ensure base structure exists
    if (!currentAssetJson['@context']) {
      currentAssetJson['@context'] = { "edc": "https://w3id.org/edc/v0.0.1/ns/" };
    }
    if (!currentAssetJson.properties) {
      currentAssetJson.properties = {};
    }
    if (!currentAssetJson.dataAddress) {
      currentAssetJson.dataAddress = { type: 'HttpData' }; // Default type
    }

    // If in manual mode, use the manualBaseUrl field to set the baseUrl
    if (this.isManualAssetCreation) {
      // Sicherstellen, dass 'baseUrl' (nicht 'base_url') verwendet wird - das Backend erwartet baseUrl
      currentAssetJson.dataAddress.baseUrl = this.manualBaseUrl;
      // Lösche base_url falls vorhanden, um Konflikte zu vermeiden
      if (currentAssetJson.dataAddress.base_url) {
        delete currentAssetJson.dataAddress.base_url;
      }
    }


    // Stelle sicher, dass proxyPath und proxyQueryParams existieren
    if (currentAssetJson.dataAddress.proxyPath === undefined) {
      currentAssetJson.dataAddress.proxyPath = true;
    }

    if (currentAssetJson.dataAddress.proxyQueryParams === undefined) {
      currentAssetJson.dataAddress.proxyQueryParams = true;
    }

    // Update properties from assetAttributes
    // Clear existing dynamic properties to avoid stale data, then re-add
    // Assuming 'asset:prop:' prefix for dynamic attributes
    for (const propKey in currentAssetJson.properties) {
      if (propKey.startsWith('asset:prop:custom-')) { // Example prefix for custom attributes
        delete currentAssetJson.properties[propKey];
      }
    }
    this.assetAttributes.forEach(attr => {
      // Only add the attribute if both key and value are provided
      if (attr.key && attr.value) {
        currentAssetJson.properties[`asset:prop:custom-${attr.key.toLowerCase().replace(/\s/g, '-')}`] = attr.value;
      }
    });

    // Update Parameterization fields in dataAddress
    if (this.pathParamId) {
      currentAssetJson.dataAddress.path = this.pathParamId;
    }

    // Combine query params into a single string
    const queryParamsString = this.queryParams
      .filter(p => p.key)
      .map(p => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value)}`)
      .join('&');
    currentAssetJson.dataAddress.queryParams = queryParamsString || undefined;

    // Collect headers into a map, removing old header properties first
    Object.keys(currentAssetJson.dataAddress).forEach(key => {
      if (key.toLowerCase().startsWith('header:')) {
        delete currentAssetJson.dataAddress[key];
      }
    });
    this.headerParams.forEach(h => {
      if (h.key) {
        // The backend expects the key to be prefixed with "header:".
        currentAssetJson.dataAddress[`header:${h.key}`] = h.value;
      }
    });
    this.expertModeJsonContent = JSON.stringify(currentAssetJson, null, 2);
  }


  async onAssetTemplateFileSelect(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const file = fileList[0];
    try {
      const templateContent = await file.text();
      const parsedContent = JSON.parse(templateContent);
      this.expertModeJsonContent = JSON.stringify(parsedContent, null, 2);      this.populateAssetFormFromDataAddress(parsedContent.dataAddress || {}); // Populate form fields from template file
      this.messageService.add({ severity: 'info', summary: 'Template Loaded', detail: `Template from ${file.name} loaded into editor.` });
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Read Error', detail: 'Could not read the selected file or parse JSON.' });
      this.expertModeJsonContent = ''; // Clear on error
      this.resetAssetFormFields(); // Clear form fields on error
    } finally {
      element.value = ''; // Reset file input to allow re-selecting the same file
    }
  }

  private populateAssetFormFromDataAddress(dataAddress: any): void {
    // Korrigierter Kommentar: Das Backend erwartet baseUrl (nicht base_url)
    // Wir müssen die konsistente Verwendung von baseUrl sicherstellen
    this.pathParamId = dataAddress.path || '';

    // Parse queryParams string into key-value pairs
    this.queryParams = [];
    const queryParamsString = dataAddress.queryParams || '';
    if (queryParamsString) {
      queryParamsString.split('&').forEach((pair: string) => {
        const [key, value] = pair.split('=');
        if (key) {
          this.queryParams.push({ key: decodeURIComponent(key), value: decodeURIComponent(value || '') });
        }
      });
    }

    // Parse headers from dataAddress properties
    this.headerParams = [];
    for (const key in dataAddress) {
      if (key.toLowerCase().startsWith('header:')) {
        this.headerParams.push({ key: key.substring(7), value: dataAddress[key] });
      }
    }

    this.assetAttributes = [];
    if (dataAddress.properties) {
      for (const key in dataAddress.properties) {
        if (key.startsWith('asset:prop:custom-')) { // Assuming custom attributes have this prefix
          this.assetAttributes.push({
            key: key.replace('asset:prop:custom-', '').replace(/-/g, ' ').replace(/^\w/, c => c.toUpperCase()),
            value: dataAddress.properties[key]
          });
        }
      }
    }
    if (this.assetAttributes.length === 0) {
      this.assetAttributes.push({ key: '', value: '' }); // Ensure at least one empty row
    }
  }

  /**
   * Populates the asset form from the combined responses of the backend endpoints.
   * This "translates" the backend's structure into the UI form fields.
   * @param arcConfig The response from /target-arc/{id}
   * @param syncedAsset The response from /synced-assets/{id}
   */
  private populateFormFromSystemSelection(arcConfig: any, syncedAsset: any): void {
    const action = arcConfig?.actions?.[0];
    if (!action && !syncedAsset) {
      // If there's no action, there's nothing to populate
      return;
    }

    // "Translate" the fields
    this.pathParamId = action.path || '';

    if (Array.isArray(action?.queryParameters)) {
      this.queryParams = action.queryParameters.map((p: { key: string, value: string }) => ({ ...p }));
    }

    if (Array.isArray(action?.headers)) {
      this.headerParams = action.headers.map((h: { key: string, value: string }) => ({ ...h }));
    }

    // Also update the properties in the JSON editor
    const assetJson = JSON.parse(this.expertModeJsonContent || '{}');
    assetJson.properties['asset:prop:name'] = syncedAsset?.['asset:prop:name'] || action?.name || arcConfig?.alias || '';
    assetJson.properties['asset:prop:description'] = syncedAsset?.['asset:prop:description'] || action?.description || '';
    assetJson.properties['asset:prop:version'] = syncedAsset?.['asset:prop:version'] || '1.0.0';
    assetJson.properties['asset:prop:contenttype'] = syncedAsset?.['asset:prop:contenttype'] || 'application/json';
    this.expertModeJsonContent = JSON.stringify(assetJson, null, 2);
  }

  private resetAssetFormFields(): void {
    this.selectedSystem = null;
    this.assetAttributes = [{ key: '', value: '' }];
    this.manualBaseUrl = '';
    this.pathParamId = '';
    this.queryParams = []; // Reset to an empty array
    this.headerParams = []; // Reset to an empty array for consistency
  }

  /**
   * Triggers the debounced synchronization from the asset JSON editor to the form fields.
   */
  syncAssetFormFromJsonWithDebounce(): void {
    this.assetJsonSyncSubject.next();
  }

  syncAssetFormFromJson(): void {
    // If the sync was triggered by the form itself, do nothing to prevent a loop.
    if (this.isSyncingFromForm) {
      this.isSyncingFromForm = false; // Reset the flag and exit
      return;
    }

    try {
      const assetJson = JSON.parse(this.expertModeJsonContent || '{}');
      // The populating function expects the dataAddress object, not the whole asset
      this.populateAssetFormFromDataAddress(assetJson.dataAddress || {});
    } catch (e) {
      // If JSON is invalid, do nothing to avoid clearing the form fields while user is typing.
      console.warn('syncAssetFormFromJson: Could not parse JSON, aborting sync.');
    }
  }
  // End Asset Template methods

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
   * Aktualisiert die Contract Definition ID im JSON basierend auf dem Eingabefeld
   */
  updateContractIdInJson(newId: string): void {
    try {
      const currentJson = JSON.parse(this.expertModeJsonContent || '{}');
      currentJson['@id'] = newId; // Setze die ID im JSON
      this.expertModeJsonContent = JSON.stringify(currentJson, null, 2);
      console.log(`Contract Definition ID wurde auf "${newId}" aktualisiert`);
    } catch (e) {
      console.error('Fehler beim Aktualisieren der Contract ID im JSON:', e);
    }
  }

  /**
   * Updates the contract definition JSON based on changes in the simple form.
   */
  syncJsonFromContractForm(): void {
    try {
      const currentJson: OdrlContractDefinition = JSON.parse(this.expertModeJsonContent || '{}');
  const sel: any = this.newContractPolicy.accessPolicyId as any;
  const accessPolicyId = (sel && (sel.policyId || sel['@id'])) || this.newContractPolicy.accessPolicyId;

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
      // Aktualisiere auch das Contract Definition ID Feld
      this.contractDefId = odrlPayload['@id'] || '';
      const assetSelectors = odrlPayload.assetsSelector || [];

      // A contract is "complex" if it contains criteria that are not simple 'eq' or 'in' selectors on asset ID
      const isComplex = assetSelectors.some(s =>
        s.operandLeft !== 'https://w3id.org/edc/v0.0.1/ns/id' ||
        !['eq', 'in'].includes(s.operator)
      );

      if (isComplex) {
        this.isContractJsonComplex = true;
        // When complex, clear the simple form to avoid confusion
        this.newContractPolicy.accessPolicyId = '';
        this.selectedAssetsInDialog = [];
        return;
      }

      // Sync Access Policy
      const accessPolicyObject = this.allAccessPolicies.find(p =>
        p.policyId === odrlPayload.accessPolicyId ||
        p['@id'] === odrlPayload.accessPolicyId
      );
      this.newContractPolicy.accessPolicyId = accessPolicyObject as any || '';

      // Sync Asset Selection by flattening the operandRight values
      const selectedAssetIds = new Set<string>(assetSelectors.map(s => s.operandRight).flat());
      this.selectedAssetsInDialog = this.assets.filter(asset => selectedAssetIds.has(asset.assetId));

    } catch (e) {
      this.isContractJsonComplex = true; // Invalid JSON is considered complex
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
          let assetJson = JSON.parse(fileContent);


          if (!assetJson['@context'] || !assetJson['@context'].edc) {
            assetJson['@context'] = { "edc": "https://w3id.org/edc/v0.0.1/ns/" };
          }

          if (!assetJson.properties) {
            assetJson.properties = {};
          }

          if (!assetJson.properties['asset:prop:name']) {
            assetJson.properties['asset:prop:name'] = file.name.replace(/\.[^/.]+$/, ""); // Dateiname ohne Erweiterung
          }

          if (!assetJson.properties['asset:prop:description']) {
            assetJson.properties['asset:prop:description'] = '';
          }

          if (!assetJson.properties['asset:prop:contenttype']) {
            assetJson.properties['asset:prop:contenttype'] = 'application/json';
          }

          if (!assetJson.dataAddress) {
            assetJson.dataAddress = { type: 'HttpData' };
          }

          if (!assetJson.dataAddress.type) {
            assetJson.dataAddress.type = 'HttpData';
          }

          // Normalisiere für Backend auf baseUrl (camelCase)
          if (assetJson.dataAddress.base_url || assetJson.dataAddress.baseURL) {
            assetJson.dataAddress.baseUrl = assetJson.dataAddress.base_url || assetJson.dataAddress.baseURL;
            delete assetJson.dataAddress.base_url;
            delete assetJson.dataAddress.baseURL;
          }

          if (!assetJson.dataAddress.baseUrl) {
            assetJson.dataAddress.baseUrl = 'https://example.com/api';
          }

          // Properties ggf. von Objekt zu Array wandeln
          if (assetJson.properties && !Array.isArray(assetJson.properties)) {
            assetJson.properties = [assetJson.properties];
          }

          // Proxy-Einstellungen
          if (assetJson.dataAddress.proxyPath === undefined) {
            assetJson.dataAddress.proxyPath = true;
          }

          if (assetJson.dataAddress.proxyQueryParams === undefined) {
            assetJson.dataAddress.proxyQueryParams = true;
          }

          console.log('Uploading asset:', JSON.stringify(assetJson, null, 2));
          await this.assetService.createAsset(this.getInstanceId(), assetJson);
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
      this.populateAssetFormFromDataAddress(this.assetToEditODRL.dataAddress || {});
      this.displayEditAssetDialog = true;
      return;
    }

    // Fallback: Einzelnes Asset vom Backend laden (nutzt UUID id)
    if (asset.id) {
      this.assetService.getAssetRaw(this.getInstanceId(), asset.id).subscribe({
        next: (raw: any) => {
          this.assetToEditODRL = raw as any;
          this.expertModeJsonContent = JSON.stringify(this.assetToEditODRL, null, 2);
          this.populateAssetFormFromDataAddress(this.assetToEditODRL.dataAddress || {});
          this.displayEditAssetDialog = true;
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Could not find the full ODRL asset to edit.' });
        }
      });
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

      // The JSON from the editor is the source of truth.
      // Add the assetId for the service call.
      assetJson.assetId = assetJson['@id'];

      // Use the update service method instead of create
      console.log('Sending updated asset to backend:', JSON.stringify(assetJson, null, 2));
      await lastValueFrom(this.assetService.updateAsset(this.getInstanceId(), this.assetToEditODRL['@id'], assetJson));
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
      message: `Are you sure you want to delete the asset "${asset.assetId}"?`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      // Make the accept callback async to handle the service call
      accept: async () => {
        try {
          // Call the service to delete the asset
          // Backend erwartet die interne UUID (id), nicht die @id
          const deleteId = asset.id || asset.assetId;
          await this.assetService.deleteAsset(this.getInstanceId(), deleteId);

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

    // Set the first available template as the default
    const defaultTemplate = this.accessPolicyTemplates.length > 0 ? this.accessPolicyTemplates[0] : null;
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
  let raw: any;
  try {
    raw = JSON.parse(this.expertModePolicyJsonContent);
  } catch (e: any) {
    this.messageService.add({ severity: 'error', summary: 'Invalid JSON', detail: e?.message ?? 'Parse error' });
    return;
  }

  // Minimalvalidierung:
  if (!raw?.['@id'] || String(raw['@id']).trim().length === 0) {
    this.messageService.add({ severity: 'warn', summary: 'Missing @id', detail: 'Please provide a non-empty @id.' });
    return;
  }

  // Call:
  try {
    const resp = await this.policyService.uploadPolicyDefinition(this.getInstanceId(), raw).toPromise();

    // Erfolg nur, wenn Server 2xx und eine ID zurückkommt (oder wenigstens 201)
    const body: any = resp?.body ?? {};
    if ((resp?.status ?? 0) >= 200 && (resp?.status ?? 0) < 300 && (body?.id || body?.policyId)) {
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Access Policy created successfully.' });
      this.loadPoliciesAndDefinitions();
      this.hideNewAccessPolicyDialog();
    } else {
      throw new Error(`Server responded ${resp?.status} without ID.`);
    }
  } catch (err: any) {
    // Versuche serverseitige Fehlermeldung anzuzeigen
    const serverMsg =
      err?.error?.message ||
      err?.error?.detail ||
      err?.error?.error ||
      (typeof err?.error === 'string' ? err.error : '') ||
      err?.message ||
      'Unknown server error';

    console.error('[Policy Create] HTTP error:', err);
    this.messageService.add({
      severity: 'error',
      summary: `Backend error (${err?.status ?? 'n/a'})`,
      detail: serverMsg
    });
  }
}



  editAccessPolicy(policy: OdrlPolicyDefinition) {
  // Suche nach dbId (primär), policyId oder @id
  this.policyToEditODRL = this.allAccessPolicies.find(p =>
    (policy.dbId && p.dbId === policy.dbId) ||
    (policy.policyId && p.policyId === policy.policyId) ||
    (policy['@id'] && p['@id'] === policy['@id'])
  ) ?? null;
  this.selectedAccessPolicyTemplate = null; // Explicitly clear template selection

  if (this.policyToEditODRL) {
    this.expertModePolicyJsonContent = JSON.stringify(this.policyToEditODRL, null, 2);
    this.expertModeTemplateJsonContent = '';
    this.syncFormFromJson();
    this.displayEditAccessPolicyDialog = true;
  } else {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Could not find the full ODRL policy to edit.'
    });
  }
}

  resetEditedPolicy(): void {
    if (this.policyToEditODRL) {
      this.expertModePolicyJsonContent = JSON.stringify(this.policyToEditODRL, null, 2);
      this.syncFormFromJson();
      this.messageService.add({ severity: 'info', summary: 'Resetted', detail: 'Changes have been reverted to the original state.' });
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

      // Wichtig: Wir müssen sicherstellen, dass die dbId erhalten bleibt
      if (!policyJson.dbId && this.policyToEditODRL.dbId) {
        console.log('Adding dbId to policy JSON:', this.policyToEditODRL.dbId);
        policyJson.dbId = this.policyToEditODRL.dbId;
      }

      console.log('Saving edited policy with dbId:', policyJson.dbId);
      await lastValueFrom(this.policyService.uploadPolicyDefinition(this.getInstanceId(), policyJson));

      // Nach dem Speichern die Daten neu laden
      await lastValueFrom(this.policyService.getPolicies(this.getInstanceId()).pipe(
        tap((policies: OdrlPolicyDefinition[]) => {
          console.log('Reloaded policies after edit:', policies);
          this.allAccessPolicies = policies;
          this.filteredAccessPolicies = [...this.allAccessPolicies];
        })
      ));

      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Access Policy updated successfully.' });
      this.hideEditAccessPolicyDialog();
    } catch (error: any) {
      const detail = error.message.includes('JSON') ? 'Could not parse JSON.' : error.message || 'Failed to save access policy.';
      this.messageService.add({ severity: 'error', summary: 'Error', detail: detail });
      console.error('Failed to update access policy:', error);
    }
  }

  deleteAccessPolicy(policy: OdrlPolicyDefinition) {
  console.log('Attempting to delete policy:', policy);
  console.log('Policy dbId:', policy.dbId);
  console.log('Policy @id:', policy['@id']);

  this.confirmationService.confirm({
    message: `Are you sure you want to delete the policy for BPN "${policy.bpn || ''}"? This will also delete all associated contract definitions.`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        // Backend löschen mit DB-UUID
        if (!policy.dbId) {
          throw new Error('Policy has no dbId, cannot delete');
        }
        console.log('Deleting policy with dbId:', policy.dbId, 'for EDC:', this.instance.id);
        await lastValueFrom(this.policyService.deletePolicy(this.getInstanceId(), policy.dbId));

        console.log('Policy deleted successfully, reloading data from server...');
        // Nach dem Löschen neu laden, anstatt nur die lokalen Arrays zu filtern
        this.loadPoliciesAndDefinitions();

        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Access Policy and associated definitions deleted successfully.'
        });
      } catch (error) {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to delete access policy.'
        });
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


          await this.policyService.uploadPolicyDefinition(this.getInstanceId(), policyJson);
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
    this.contractDefId = ''; // Zurücksetzen der Contract Definition ID

    // Create a default empty JSON structure ohne automatisch generierte ID
    this.expertModeJsonContent = JSON.stringify({
      '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
      '@id': '', // Benutzer muss die ID selbst eingeben
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
      const raw = JSON.parse(this.expertModeJsonContent || '{}');

      // Ensure required fields for backend DTO
      let contractDefinitionId = String(raw['@id'] || '').trim();
      if (!contractDefinitionId) {
        // Keine automatische ID-Generierung mehr
        throw new Error("Contract Definition ID is required. Please enter a valid ID in the form or JSON.");
      }

      // Determine accessPolicyId string from simple form selection or raw JSON
      const accessPolicyObj = (this.newContractPolicy.accessPolicyId as any) || {};
      const accessPolicyIdStr = accessPolicyObj.policyId || accessPolicyObj['@id'] || raw.accessPolicyId || '';
      if (!accessPolicyIdStr) {
        throw new Error("Invalid contract definition. 'accessPolicyId' is required.");
      }

      // Determine assetId: prefer selected assets, otherwise from JSON assetsSelector
      let assetId = '';
      if (this.selectedAssetsInDialog.length > 0) {
        assetId = this.selectedAssetsInDialog[0]?.assetId || '';
      } else if (Array.isArray(raw.assetsSelector) && raw.assetsSelector.length > 0) {
        // Look for operandRight (eq) or array
        const firstSelector = raw.assetsSelector[0];
        if (Array.isArray(firstSelector.operandRight)) {
          assetId = firstSelector.operandRight[0] || '';
        } else {
          assetId = firstSelector.operandRight || '';
        }
      }
      if (!assetId) {
        // As a safety, take the first available asset from list if any
        assetId = this.assets[0]?.assetId || '';
      }
      if (!assetId) {
        throw new Error("Invalid contract definition. 'assetId' is required (select at least one asset).");
      }

      // Build backend DTO shape
      const dto = {
        contractDefinitionId,
        assetId,
        accessPolicyIdStr: accessPolicyIdStr,
        contractPolicyIdStr: accessPolicyIdStr,
        rawJson: JSON.stringify(raw)
      };

  await lastValueFrom(this.policyService.createContractDefinition(this.getInstanceId(), dto as any));
      this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Contract Definition created successfully.' });
      this.loadPoliciesAndDefinitions();
      this.hideNewContractPolicyDialog();
    } catch (error: any) {
      const detail = error?.message?.includes('JSON') ? 'Could not parse JSON.' : error?.message || 'Failed to save contract definition.';
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
          const parentPolicyExists = this.allAccessPolicies.some(p =>
            p.policyId === contractDefJson.accessPolicyId ||
            p['@id'] === contractDefJson.accessPolicyId
          );
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


          await lastValueFrom(this.policyService.createContractDefinition(this.getInstanceId(), contractDefJson));

          const parentPolicy = this.allAccessPolicies.find(p =>
            p.policyId === contractDefJson.accessPolicyId ||
            p['@id'] === contractDefJson.accessPolicyId
          );
          // Temporäre ID verwenden bis wir die tatsächliche DB-ID kennen
          this.allContractDefinitions.unshift({
            id: 0, // Temporäre ID, wird später durch echte DB-ID ersetzt
            contractDefinitionId: contractDefJson['@id'],
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
          const accessPolicyObject = this.allAccessPolicies.find(p =>
            p.policyId === odrlPayload.accessPolicyId ||
            p['@id'] === odrlPayload.accessPolicyId
          );
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
  editContractPolicy(contractPolicy: UiContractDefinition) {
     this.isExpertMode = false; // Default to normal mode
     this.isComplexSelectorForEdit = false; // Reset

     // Find the full ODRL object for expert mode - Vergleiche mit contractDefinitionId statt id
     this.contractDefinitionToEditODRL = this.allOdrlContractDefinitions.find(cd => cd['@id'] === contractPolicy.contractDefinitionId) ?? null;

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

     const accessPolicyObject = this.allAccessPolicies.find(p =>
        p.policyId === contractPolicy.accessPolicyId ||
        p['@id'] === contractPolicy.accessPolicyId
     );

     this.contractPolicyToEdit = {
       ...contractPolicy,
       accessPolicyId: accessPolicyObject as any,
       assetId: '', // No longer used for a single asset, but keep property for model consistency
       '@context': this.contractDefinitionToEditODRL['@context'],
       '@id': this.contractDefinitionToEditODRL['@id'],
       contractPolicyId: this.contractDefinitionToEditODRL.contractPolicyId,
       assetsSelector: this.contractDefinitionToEditODRL.assetsSelector
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

      await this.policyService.updateContractDefinition(this.getInstanceId(), odrlPayload);

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

  deleteContractPolicy(contractPolicy: UiContractDefinition) {
  this.confirmationService.confirm({
    message: `Are you sure you want to delete the contract definition for asset "${contractPolicy.assetId}"?`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        // Log die ID, die zum Löschen verwendet wird
        console.log(`Deleting contract definition with ID: ${contractPolicy.id}, contractDefinitionId: ${contractPolicy.contractDefinitionId}`);
        await this.policyService.deleteContractDefinition(this.getInstanceId(), contractPolicy.id);

        // UI-Listen aktualisieren
        this.allContractDefinitions = this.allContractDefinitions.filter(cd => cd.id !== contractPolicy.id);
        this.filteredContractDefinitions = [...this.allContractDefinitions];

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


  onAccessPolicyTemplateChange(event: { value: Template | null }) {
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

  async onPolicyJsonUpload(event: Event) {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;

    if (!fileList || fileList.length === 0) {
      return;
    }

    const file = fileList[0]; // Only one file
    try {
      const fileContent = await file.text();
      // We need to pretty-print it for the editor
      const parsedJson = JSON.parse(fileContent);
      this.expertModePolicyJsonContent = JSON.stringify(parsedJson, null, 2);
      this.syncFormFromJson(); // After loading, sync the form from the new JSON
      this.messageService.add({ severity: 'info', summary: 'Content Loaded', detail: `JSON from ${file.name} loaded into editor.` });
    } catch (error) {
      this.messageService.add({ severity: 'error', summary: 'Read Error', detail: 'Could not read or parse the selected file.' });
    } finally {
      element.value = ''; // Reset file input
    }
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
      } else if (Array.isArray(obj[key])) {
        for (let i = 0; i < obj[key].length; i++) {
          if (typeof obj[key][i] === 'object' && obj[key][i] !== null) {
            this.replaceTemplatePlaceholders(obj[key][i]);
          }
        }
      } else if (typeof obj[key] === 'object' && obj[key] !== null) {
        this.replaceTemplatePlaceholders(obj[key]);
      }
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
  const selectedAsset = event.data as Asset;

  // Find the original, raw JSON object from the list we stored when loading assets.
  // This ensures we show the true structure, not the transformed UI model.
  const rawAsset = this.allOdrlAssets.find(a => a['@id'] === selectedAsset.assetId);

  if (rawAsset) {
    this.jsonToView = JSON.stringify(rawAsset, null, 2);
    this.viewDialogHeader = `Details for Asset: ${selectedAsset.name || selectedAsset.assetId}`;
    this.displayViewDialog = true;
  } else {
    this.messageService.add({ severity: 'warn', summary: 'Not Found', detail: 'Could not find the original raw JSON for this asset.' });
  }
}


  viewAccessPolicyDetails(event: TableRowSelectEvent): void {
  this.viewingEntityType = 'policy';
  const policy = event.data as OdrlPolicyDefinition;

  // Da du deine allAccessPolicies schon entpackst, reicht direkt die Suche dort
  const odrlPolicy = this.allAccessPolicies.find(p =>
    (policy.dbId && p.dbId === policy.dbId) ||
    (policy.policyId && p.policyId === policy.policyId) ||
    (policy['@id'] && p['@id'] === policy['@id'])
  );

  if (odrlPolicy) {
    this.jsonToView = JSON.stringify(odrlPolicy, null, 2);
    this.viewDialogHeader = `Details for Access Policy: ${policy.policyId || policy['@id'] || policy.dbId}`;
    this.displayViewDialog = true;
  } else {
    this.messageService.add({
      severity: 'warn',
      summary: 'Not Found',
      detail: 'Could not find the full ODRL details for this policy.'
    });
  }
}

viewContractDefinitionDetails(event: TableRowSelectEvent): void {
  this.viewingEntityType = 'contract';
  const contractPolicy = event.data as UiContractDefinition;

  const odrlContractDef = this.allOdrlContractDefinitions.find(
    cd => cd['@id'] === contractPolicy.contractDefinitionId
  );

  if (odrlContractDef) {
    // Raw JSON
    this.jsonToView = JSON.stringify(odrlContractDef, null, 2);
    this.viewDialogHeader = `Details for Contract Definition: ${contractPolicy.contractDefinitionId || contractPolicy.id}`;

    // Verknüpfte Access Policy suchen
    this.linkedAccessPolicy = this.allAccessPolicies.find(p =>
      p.policyId === odrlContractDef.accessPolicyId ||
      p['@id'] === odrlContractDef.accessPolicyId
    ) || null;

    // Verknüpfte Assets suchen
    const assetIds = new Set(
      (odrlContractDef.assetsSelector || []).map(s => s.operandRight)
    );
    this.linkedAssets = this.assets.filter(a => assetIds.has(a.assetId));

    this.displayViewDialog = true;
  } else {
    this.messageService.add({
      severity: 'warn',
      summary: 'Not Found',
      detail: 'Could not find the full ODRL details for this contract definition.'
    });
  }
}
}
