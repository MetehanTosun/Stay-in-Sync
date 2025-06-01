// src/app/features/source-system/components/create-source-system/create-source-system.component.ts

// TypeScript-Deklaration, damit der Compiler nicht meckert.
// Zur Laufzeit findet er dann window.SwaggerParser (durch das <script> in index.html).
declare const SwaggerParser: any;

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

// PrimeNG-Module
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextarea } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';

// Services und Bibliotheken
import { AasService } from '../../services/aas.service';
import { Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

// Erlaubte Quellsystem-Typen (angepasst an HTML)
type SourceType = 'AAS' | 'REST_OPENAPI';

@Component({
  selector: 'app-create-source-system',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    DialogModule,
    DropdownModule,
    InputTextarea,
    InputTextModule
  ],
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css']
})
export class CreateSourceSystemComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() sourceSaved = new EventEmitter<any>();

  sourceType: SourceType = 'AAS';
  sourceTypeOptions = [
    { label: 'AAS Registry', value: 'AAS' as SourceType },
    { label: 'REST (from OpenAPI)', value: 'REST_OPENAPI' as SourceType }
  ];

  source: { name: string; aasId?: string } = { name: '' };
  aasList$: Observable<{ id: string; name: string }[]> = of([]);
  isLoadingAas = false;

  // === Eigenschaften für REST_OPENAPI ===
  openApiSpecUrl: string = '';
  isLoadingOpenApiSpec = false;
  openApiSpecError: string | null = null;
  parsedOpenApiSpec: any = null;
  availableApiEndpoints: { path: string; method: string; operation: any; id: string }[] = [];
  selectedApiEndpoint: { path: string; method: string; operation: any; id: string } | null = null;
  dynamicEndpointParameters: {
    name: string;
    in: 'query' | 'path' | 'header' | 'cookie';
    required: boolean;
    schema: any;
    value: any;
    description?: string;
  }[] = [];
  requestBodyValue: string = '';
  // === Ende Eigenschaften für REST_OPENAPI ===

  constructor(private aasService: AasService, private http: HttpClient) {}

  ngOnInit(): void {
    if (this.sourceType === 'AAS') {
      this.loadAasList();
    }
  }

  onTypeChange(newType: SourceType): void {
    this.sourceType = newType;
    this.source.aasId = undefined;
    if (newType !== 'REST_OPENAPI') {
      this.resetOpenApiFields();
    }
    if (newType === 'AAS') {
      this.loadAasList();
    } else {
      this.aasList$ = of([]);
      this.isLoadingAas = false;
    }
  }

  private resetOpenApiFields(): void {
    this.openApiSpecUrl = '';
    this.isLoadingOpenApiSpec = false;
    this.openApiSpecError = null;
    this.parsedOpenApiSpec = null;
    this.availableApiEndpoints = [];
    this.selectedApiEndpoint = null;
    this.dynamicEndpointParameters = [];
    this.requestBodyValue = '';
  }

  private loadAasList(): void {
    if (this.sourceType === 'AAS') {
      this.isLoadingAas = true;
      this.aasList$ = this.aasService.getAll().pipe(
        tap(() => {
          this.isLoadingAas = false;
        }),
        catchError((err) => {
          console.error('Fehler beim Laden der AAS-Liste:', err);
          this.isLoadingAas = false;
          return of([]);
        })
      );
    }
  }

  async loadAndParseOpenApiSpec(): Promise<void> {
    if (!this.openApiSpecUrl) {
      this.openApiSpecError = 'Please enter an OpenAPI Spec URL.';
      return;
    }
    this.isLoadingOpenApiSpec = true;
    this.openApiSpecError = null;
    this.parsedOpenApiSpec = null;
    this.availableApiEndpoints = [];
    this.selectedApiEndpoint = null;
    this.dynamicEndpointParameters = [];
    this.requestBodyValue = '';

    try {
      // Nutze hier das globale SwaggerParser (aus index.html <script>)
      const spec = await SwaggerParser.dereference(this.openApiSpecUrl);
      this.parsedOpenApiSpec = spec;
      this.extractEndpoints();
    } catch (err: any) {
      console.error('Error loading or parsing OpenAPI spec:', err);
      this.openApiSpecError = `Failed to load/parse spec: ${err.message || 'Unknown error'}`;
    } finally {
      this.isLoadingOpenApiSpec = false;
    }
  }

  extractEndpoints(): void {
    if (!this.parsedOpenApiSpec || !this.parsedOpenApiSpec.paths) {
      return;
    }
    const endpoints: { path: string; method: string; operation: any; id: string }[] = [];
    for (const path in this.parsedOpenApiSpec.paths) {
      for (const method in this.parsedOpenApiSpec.paths[path]) {
        if (['get', 'post', 'put', 'delete', 'patch', 'options', 'head', 'trace'].includes(method.toLowerCase())) {
          const operation = this.parsedOpenApiSpec.paths[path][method];
          endpoints.push({
            path: path,
            method: method.toUpperCase(),
            operation: operation,
            id: operation.operationId || `${method.toUpperCase()} ${path}`
          });
        }
      }
    }
    this.availableApiEndpoints = endpoints;
  }

  selectApiEndpoint(endpoint: { path: string; method: string; operation: any; id: string }): void {
    this.selectedApiEndpoint = endpoint;
    this.dynamicEndpointParameters = [];
    this.requestBodyValue = '';

    if (endpoint.operation.parameters) {
      this.dynamicEndpointParameters = endpoint.operation.parameters.map((param: any) => ({
        name: param.name,
        in: param.in,
        required: param.required || false,
        schema: param.schema || { type: 'string' },
        value: param.schema?.default !== undefined ? param.schema.default : '',
        description: param.description || ''
      }));
    }
    if (endpoint.operation.requestBody?.content?.['application/json']?.schema) {
      this.requestBodyValue = '';
    }
  }

  save(): void {
    if (!this.source.name || this.source.name.trim() === '') {
      console.error('Source System Name is required.');
      return;
    }

    let sourceToSave: any = {
      name: this.source.name.trim(),
      type: this.sourceType
    };

    this.openApiSpecError = null;

    switch (this.sourceType) {
      case 'AAS':
        if (!this.source.aasId) {
          console.error('AAS Instance is required for AAS type.');
          return;
        }
        sourceToSave.aasId = this.source.aasId;
        break;
      case 'REST_OPENAPI':
        if (!this.openApiSpecUrl || this.openApiSpecUrl.trim() === '') {
          this.openApiSpecError = 'OpenAPI Spec URL is required.';
          return;
        }
        if (!this.selectedApiEndpoint) {
          this.openApiSpecError = 'An API endpoint must be selected.';
          return;
        }
        sourceToSave.specUrl = this.openApiSpecUrl.trim();
        sourceToSave.operationDetails = {
          operationId: this.selectedApiEndpoint.operation.operationId,
          path: this.selectedApiEndpoint.path,
          method: this.selectedApiEndpoint.method
        };
        sourceToSave.parameters = this.dynamicEndpointParameters.reduce((acc, param) => {
          if (param.value !== '' || param.required) {
            acc[param.name] = param.value;
          }
          return acc;
        }, {} as { [key: string]: any });

        if (this.requestBodyValue && this.requestBodyValue.trim() !== '') {
          try {
            sourceToSave.requestBody = JSON.parse(this.requestBodyValue);
          } catch (e) {
            console.error('Invalid JSON in request body:', e);
            this.openApiSpecError = 'Request body contains invalid JSON.';
            return;
          }
        }
        break;
      default:
        console.error('Unknown source type:', this.sourceType);
        return;
    }

    console.log('Saving Source System:', sourceToSave);
    this.sourceSaved.emit(sourceToSave);
    this.closeDialogAndReset();
  }

  cancel(): void {
    this.closeDialogAndReset();
  }

  private closeDialogAndReset(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
    this.resetForm();
  }

  private resetForm(): void {
    this.source = { name: '' };
    this.sourceType = 'AAS';
    this.resetOpenApiFields();

    if (this.sourceType === 'AAS') {
      this.loadAasList();
    } else {
      this.aasList$ = of([]);
      this.isLoadingAas = false;
    }
    this.openApiSpecError = null;
  }
}