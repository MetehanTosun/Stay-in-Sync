import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';

import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {CardModule} from 'primeng/card';
import {CheckboxModule} from 'primeng/checkbox';
import {DialogModule} from 'primeng/dialog';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';

import {SourceSystemEndpointResourceService} from '../../service/sourceSystemEndpointResource.service';
import {HttpClient} from '@angular/common/http';
import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';

import {ApiEndpointQueryParamResourceService} from '../../service/apiEndpointQueryParamResource.service';
import {SourceSystemEndpointDTO} from '../../models/sourceSystemEndpointDTO';
import {ApiEndpointQueryParamDTO} from '../../models/apiEndpointQueryParamDTO';
import {ApiEndpointQueryParamType} from '../../models/apiEndpointQueryParamType';
import {CreateSourceSystemEndpointDTO} from '../../models/createSourceSystemEndpointDTO';

import { load as parseYAML } from 'js-yaml';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { ManageEndpointParamsComponent } from '../manage-endpoint-params/manage-endpoint-params.component';


/**
 * Component for managing endpoints of a source system: list, create, edit, delete, and import.
 */
@Component({
  standalone: true,
  selector: 'app-manage-endpoints',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    CardModule,
    CheckboxModule,
    DialogModule,
    ProgressSpinnerModule,
    ManageEndpointParamsComponent,
    MonacoEditorModule,
  ],
  templateUrl: './manage-endpoints.component.html',
  styleUrls: ['./manage-endpoints.component.css']
})
export class ManageEndpointsComponent implements OnInit {
  public ApiEndpointQueryParamType = ApiEndpointQueryParamType;
  @Input() sourceSystemId!: number;
  @Output() backStep = new EventEmitter<void>();
  @Output() finish = new EventEmitter<void>();
  endpoints: SourceSystemEndpointDTO[] = [];
  endpointForm!: FormGroup;
  loading = false;
  selectedEndpoint: SourceSystemEndpointDTO | null = null;
  httpRequestTypes = [
    {label: 'GET', value: 'GET'},
    {label: 'POST', value: 'POST'},
    {label: 'PUT', value: 'PUT'},
    {label: 'DELETE', value: 'DELETE'}
  ];
  apiUrl: string | null = null;
  importing = false;
  editDialog: boolean = false;
  editingEndpoint: SourceSystemEndpointDTO | null = null;
  editForm!: FormGroup;
  jsonEditorOptions = { theme: 'vs-dark', language: 'json', automaticLayout: true };
  jsonError: string | null = null;
  editJsonError: string | null = null;
  requestBodyEditorEndpoint: SourceSystemEndpointDTO | null = null;
  requestBodyEditorModel: NgxEditorModel = { value: '', language: 'json' };
  requestBodyEditorError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private endpointSvc: SourceSystemEndpointResourceService,
    private sourceSystemService: SourceSystemResourceService,
    private http: HttpClient,
    private queryParamSvc: ApiEndpointQueryParamResourceService,
  ) {}

  /**
   * Initialize forms and load endpoints and source system API URL.
   */
  ngOnInit(): void {
    this.endpointForm = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: ['']
    });
    this.editForm = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: ['']
    });
    this.loadSourceSystemAndSetApiUrl();
  }

  /**
   * Loads the source system and sets the API URL.
   */
  private loadSourceSystemAndSetApiUrl(): void {
    this.sourceSystemService.apiConfigSourceSystemIdGet(this.sourceSystemId)
      .subscribe({
        next: (sourceSystem: SourceSystemDTO) => {
          if (sourceSystem.openApiSpec && typeof sourceSystem.openApiSpec === 'string') {
            if (sourceSystem.openApiSpec.startsWith('http')) {
              this.apiUrl = sourceSystem.openApiSpec.trim();
            } else {
              this.apiUrl = sourceSystem.apiUrl!;
            }
          } else {
            this.apiUrl = sourceSystem.apiUrl!;
          }
        },
        error: () => {
          this.apiUrl = null;
        }
      });
  }

  /**
   * Loads endpoints for the current source system from the backend.
   */
  loadEndpoints() {
    if (!this.sourceSystemId) return;
    this.loading = true;
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointGet(this.sourceSystemId)
      .subscribe({
        next: (eps: SourceSystemEndpointDTO[]) => {
          this.endpoints = eps;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  /**
   * Create a new endpoint using form data and refresh list upon success.
   */
  addEndpoint() {
    let requestBodySchema = this.endpointForm.get('requestBodySchema')?.value || '';
    requestBodySchema = this.resolveSchemaReference(requestBodySchema);
    const dto: any = {
      endpointPath: this.endpointForm.get('endpointPath')?.value,
      httpRequestType: this.endpointForm.get('httpRequestType')?.value,
      requestBodySchema
    };
    this.endpointSvc.apiConfigSourceSystemSourceSystemIdEndpointPost(
      this.sourceSystemId,
      [dto]
    ).subscribe();
  }

  /**
   * Delete an endpoint by its ID and remove it from the list.
   */
  deleteEndpoint(id: number) {
    this.endpointSvc
      .apiConfigSourceSystemEndpointIdDelete(id)
      .subscribe({
        next: () => this.endpoints = this.endpoints.filter(e => e.id !== id),
        error: () => {}
      });
  }

  /**
   * Open the edit dialog pre-filled with endpoint data.
   */
  openEditDialog(endpoint: SourceSystemEndpointDTO) {
    this.editingEndpoint = endpoint;
    this.editForm.patchValue({
      endpointPath: endpoint.endpointPath,
      httpRequestType: endpoint.httpRequestType,
      requestBodySchema: endpoint.requestBodySchema || ''
    });
    this.editDialog = true;
    this.editJsonError = null;
  }

  /**
   * Save changes made to the editing endpoint and refresh list.
   */
  saveEdit() {
    if (!this.editingEndpoint || this.editForm.invalid) {
      return;
    }
    this.editJsonError = null;
    const dto: SourceSystemEndpointDTO = {
      id: this.editingEndpoint.id!,
      sourceSystemId: this.sourceSystemId,
      endpointPath: this.editForm.value.endpointPath,
      httpRequestType: this.editForm.value.httpRequestType,
      requestBodySchema: this.editForm.value.requestBodySchema
    };
    if (dto.requestBodySchema) {
      try {
        JSON.parse(dto.requestBodySchema);
      } catch (e) {
        this.editJsonError = 'Request-Body-Schema ist kein valides JSON.';
        return;
      }
    }
    this.endpointSvc
      .apiConfigSourceSystemEndpointIdPut(this.editingEndpoint.id!, dto, 'body')
      .subscribe({
        next: () => {
          this.editDialog = false;
          this.loadEndpoints();
        },
        error: () => {}
      });
  }

  /**
   * Show the request body editor for a given endpoint.
   */
  showRequestBodyEditor(endpoint: SourceSystemEndpointDTO) {
    this.requestBodyEditorEndpoint = endpoint;
    if (!endpoint.requestBodySchema) {
      this.requestBodyEditorModel = {
        value: JSON.stringify({
          error: 'No request body schema defined for this endpoint',
          endpoint: endpoint.endpointPath,
          method: endpoint.httpRequestType,
          note: 'This endpoint does not require a request body or no schema is defined in the OpenAPI specification.'
        }, null, 2),
        language: 'json'
      };
      return;
    }
    const resolved = this.resolveSchemaReference(endpoint.requestBodySchema);
    this.requestBodyEditorModel = {
      value: resolved,
      language: 'json'
    };
  }

  /**
   * Cleans a JSON schema by removing null values and unnecessary properties and resolving schema references.
   */
  private cleanJsonSchema(schema: any): any {
    if (!schema || typeof schema !== 'object') {
      return schema;
    }
    if (schema.$ref) {
      const resolvedSchemaStr = this.resolveSchemaReference(schema);
      try {
        const resolvedSchema = JSON.parse(resolvedSchemaStr);
        return this.cleanJsonSchema(resolvedSchema);
      } catch {
        return schema;
      }
    }
    const cleaned: any = {};
    const relevantProps = [
      'type', 'properties', 'required', 'items', 'allOf', 'anyOf', 'oneOf', 
      'not', 'additionalProperties', 'description', 'format', '$ref', 
      'nullable', 'readOnly', 'writeOnly', 'example', 'deprecated', 
      'xml', 'discriminator', 'enum', 'const', 'default', 'minItems', 
      'maxItems', 'uniqueItems', 'minProperties', 'maxProperties',
      'minLength', 'maxLength', 'pattern', 'minimum', 'maximum',
      'exclusiveMinimum', 'exclusiveMaximum', 'multipleOf'
    ];
    for (const prop of relevantProps) {
      if (schema[prop] !== null && schema[prop] !== undefined) {
        if (typeof schema[prop] === 'object' && !Array.isArray(schema[prop])) {
          cleaned[prop] = this.cleanJsonSchema(schema[prop]);
        } else if (Array.isArray(schema[prop])) {
          cleaned[prop] = schema[prop].map((item: any) => 
            typeof item === 'object' ? this.cleanJsonSchema(item) : item
          );
        } else {
          cleaned[prop] = schema[prop];
        }
      }
    }
    return cleaned;
  }

  /**
   * Resolves a schema reference ($ref) in the OpenAPI spec and returns the resolved schema as a string.
   */
  private resolveSchemaReference(schemaInput: any): string {
    try {
      let schemaObj = typeof schemaInput === 'string' ? JSON.parse(schemaInput) : schemaInput;
      if (schemaObj && schemaObj.$ref && this.currentOpenApiSpec) {
        const openApi = typeof this.currentOpenApiSpec === 'string'
          ? JSON.parse(this.currentOpenApiSpec)
          : this.currentOpenApiSpec;
        if (schemaObj.$ref.startsWith('#/components/schemas/')) {
          const schemaName = schemaObj.$ref.replace('#/components/schemas/', '');
          const resolved = openApi.components?.schemas?.[schemaName];
          if (resolved) {
            if (resolved.$ref) {
              return this.resolveSchemaReference(resolved);
            }
            return JSON.stringify(resolved, null, 2);
          }
        }
      }
    } catch (e) {}
    return typeof schemaInput === 'string' ? schemaInput : JSON.stringify(schemaInput, null, 2);
  }

  /**
   * Parses the OpenAPI spec (JSON or YAML) and resolves the schema reference.
   */
  private parseAndResolveSchema(specText: string | undefined, ref: string): void {
    if (!specText) {
      this.requestBodyEditorModel = {
        value: JSON.stringify({ $ref: ref, error: 'No OpenAPI spec available' }, null, 2),
        language: 'json'
      };
      return;
    }
    try {
      let spec: any;
      try {
        spec = JSON.parse(specText);
      } catch (jsonError) {
        try {
          spec = this.parseYamlToJson(specText);
        } catch (yamlError) {
          throw new Error('Failed to parse OpenAPI spec as JSON or YAML');
        }
      }
      const schemas = spec.components?.schemas || {};
      const schemaName = ref.replace('#/components/schemas/', '');
      const schema = schemas[schemaName];
      if (schema) {
        const resolvedSchema = this.resolveRefs(schema, schemas);
        this.requestBodyEditorModel = {
          value: JSON.stringify(resolvedSchema, null, 2),
          language: 'json'
        };
      } else {
        this.requestBodyEditorModel = {
          value: JSON.stringify({ $ref: ref, error: 'Schema not found in OpenAPI spec' }, null, 2),
          language: 'json'
        };
      }
    } catch (e) {
      this.requestBodyEditorModel = {
        value: JSON.stringify({ $ref: ref, error: 'Failed to parse OpenAPI spec' }, null, 2),
        language: 'json'
      };
    }
  }

  /**
   * Converts YAML to JSON for basic OpenAPI specs.
   */
  private parseYamlToJson(yamlText: string): any {
    try {
      const cleanedYaml = this.cleanYaml(yamlText);
      const result = this.simpleYamlParse(cleanedYaml);
      return result;
    } catch (e) {
      throw new Error('YAML parsing failed');
    }
  }

  /**
   * Removes comments and unnecessary characters from YAML text.
   */
  private cleanYaml(yamlText: string): string {
    const lines = yamlText.split('\n');
    const cleanedLines = lines.map(line => {
      const commentIndex = line.indexOf('#');
      if (commentIndex >= 0) {
        line = line.substring(0, commentIndex);
      }
      return line;
    });
    return cleanedLines.join('\n');
  }

  /**
   * Simple YAML parsing implementation for OpenAPI specs.
   */
  private simpleYamlParse(yamlText: string): any {
    const lines = yamlText.split('\n').filter(line => line.trim() !== '');
    const result: any = {};
    const path: string[] = [];
    const stack: any[] = [result];
    for (const line of lines) {
      const indent = this.getIndentLevel(line);
      const content = line.trim();
      if (content === '') continue;
      while (path.length > indent / 2) {
        path.pop();
        stack.pop();
      }
      if (content.includes(':')) {
        const colonIndex = content.indexOf(':');
        const key = content.substring(0, colonIndex).trim();
        const value = content.substring(colonIndex + 1).trim();
        if (value === '') {
          const newObj: any = {};
          this.setNestedValue(result, [...path, key], newObj);
          path.push(key);
          stack.push(newObj);
        } else {
          this.setNestedValue(result, [...path, key], this.parseValue(value));
        }
      } else if (content.startsWith('- ')) {
        const value = content.substring(2);
        const currentPath = [...path];
        const parentKey = currentPath.pop();
        if (parentKey) {
          const parent = this.getNestedValue(result, currentPath);
          if (!Array.isArray(parent[parentKey])) {
            parent[parentKey] = [];
          }
          parent[parentKey].push(this.parseValue(value));
        }
      }
    }
    return result;
  }

  /**
   * Sets a value in a nested object.
   */
  private setNestedValue(obj: any, path: string[], value: any): void {
    let current = obj;
    for (let i = 0; i < path.length - 1; i++) {
      if (!current[path[i]]) {
        current[path[i]] = {};
      }
      current = current[path[i]];
    }
    current[path[path.length - 1]] = value;
  }

  /**
   * Gets a value from a nested object.
   */
  private getNestedValue(obj: any, path: string[]): any {
    let current = obj;
    for (const key of path) {
      if (current && typeof current === 'object' && key in current) {
        current = current[key];
      } else {
        return undefined;
      }
    }
    return current;
  }

  /**
   * Determines the indentation level of a line.
   */
  private getIndentLevel(line: string): number {
    let level = 0;
    for (let i = 0; i < line.length; i++) {
      if (line[i] === ' ' || line[i] === '\t') {
        level++;
      } else {
        break;
      }
    }
    return level;
  }

  /**
   * Parses a single YAML value.
   */
  private parseValue(value: string): any {
    value = value.trim();
    if (value === 'true') return true;
    if (value === 'false') return false;
    if (value === 'null' || value === '') return null;
    if (!isNaN(Number(value))) {
      return Number(value);
    }
    if ((value.startsWith('"') && value.endsWith('"')) || 
        (value.startsWith("'") && value.endsWith("'"))) {
      return value.slice(1, -1);
    }
    return value;
  }

  /**
   * Closes the request body editor.
   */
  closeRequestBodyEditor() {
    this.requestBodyEditorEndpoint = null;
    this.requestBodyEditorModel = { value: '', language: 'json' };
    this.requestBodyEditorError = null;
  }

  /**
   * Saves the request body schema from the editor.
   */
  saveRequestBodySchema() {
    if (!this.requestBodyEditorEndpoint) return;
    let schemaValue = this.requestBodyEditorModel.value;
    schemaValue = this.resolveSchemaReference(schemaValue);
    this.requestBodyEditorEndpoint.requestBodySchema = schemaValue;
    this.closeRequestBodyEditor();
  }

  /**
   * Handles changes in the request body editor model.
   */
  onRequestBodyModelChange(event: any) {
    let value: string = '';
    if (typeof event === 'string') {
      value = event;
    } else if (event && typeof event.detail === 'string') {
      value = event.detail;
    } else if (event && event.target && typeof event.target.value === 'string') {
      value = event.target.value;
    } else {
      value = String(event);
    }
    this.requestBodyEditorModel = {
      ...this.requestBodyEditorModel,
      value
    };
  }

  /**
   * Navigates back to the previous wizard step.
   */
  onBack() {
    this.backStep.emit();
  }

  /**
   * Finishes the wizard and emits the completion event.
   */
  onFinish() {
    this.finish.emit();
  }

  /**
   * Imports endpoints from the OpenAPI spec.
   */
  async importEndpoints(): Promise<void> {
    if (!this.apiUrl) {
      return;
    }
    this.importing = true;
    try {
      await this.tryImportFromUrl();
    } catch (err) {
      this.importing = false;
    }
  }

  /**
   * Attempts to import the OpenAPI spec from the apiUrl, using direct and fallback URLs.
   */
  private async tryImportFromUrl(): Promise<void> {
    if (!this.apiUrl) {
      throw new Error('No API URL available for import');
    }
    if (
      this.apiUrl.includes('swagger.json') ||
      this.apiUrl.includes('openapi.json') ||
      this.apiUrl.endsWith('.yaml') ||
      this.apiUrl.endsWith('.yml')
    ) {
      try {
        const openApiSpec = await this.http.get(this.apiUrl).toPromise();
        await this.processOpenApiSpec(openApiSpec);
        this.importing = false;
      } catch (err) {
        this.importing = false;
        throw err;
      }
    } else {
      try {
        const openApiSpec = await this.http.get(this.apiUrl + '/swagger.json').toPromise();
        await this.processOpenApiSpec(openApiSpec);
      } catch (err) {
        this.tryAlternativeOpenApiUrls();
      }
    }
  }

  /**
   * Tries alternative OpenAPI URLs if the main one fails.
   */
  private tryAlternativeOpenApiUrls() {
    const alternativeUrls = [
      `${this.apiUrl}/v2/swagger.json`,
      `${this.apiUrl}/api/v3/openapi.json`,
      `${this.apiUrl}/swagger/v1/swagger.json`,
      `${this.apiUrl}/api-docs`,
      `${this.apiUrl}/openapi.json`,
      `${this.apiUrl}/docs/swagger.json`,
      `${this.apiUrl}/openapi.yaml`,   
      `${this.apiUrl}/openapi.yml`,     
    ];
    this.loadOpenApiFromUrls(alternativeUrls, 0);
  }

  /**
   * Loads the OpenAPI spec from a list of URLs.
   */
  private loadOpenApiFromUrls(urls: string[], index: number) {
    if (index >= urls.length) {
      this.importing = false;
      return;
    }
    const url = urls[index];
    const isJson = url.endsWith('.json');
    this.http.get(url, {
      responseType: isJson ? 'json' : 'text' as 'json'
    }).subscribe({
      next: (raw: any) => {
        let spec: any;
        if (isJson) {
          spec = raw;
        } else {
          try {
            spec = parseYAML(raw as string);
          } catch (e) {
            this.loadOpenApiFromUrls(urls, index + 1);
            return;
          }
        }
        this.processOpenApiSpec(spec);
      },
      error: () => {
        this.loadOpenApiFromUrls(urls, index + 1);
      }
    });
  }

  /**
   * Processes the OpenAPI spec and creates endpoints.
   */
  private async processOpenApiSpec(spec: any): Promise<void> {
    try {
      const endpointsToCreate: CreateSourceSystemEndpointDTO[] = [];
      const schemas = spec.components?.schemas || {};
      if (spec.paths) {
        for (const [path, pathItem] of Object.entries(spec.paths)) {
          for (const [method, operation] of Object.entries(pathItem as any)) {
            if ([
              'get', 'post', 'put', 'delete', 'patch', 'head', 'options'
            ].includes(method.toLowerCase())) {
              const operationDetails = operation as any;
              const pathLevelParams = (pathItem as any).parameters || [];
              const operationLevelParams = operationDetails.parameters || [];
              const allParameters = [...pathLevelParams, ...operationLevelParams];
              const formattedPath = this.formatPathWithParameters(path, allParameters);
              let requestBodySchema: string | undefined = undefined;
              if ([
                'post', 'put'
              ].includes(method.toLowerCase())) {
                const requestBody = operationDetails.requestBody;
                if (requestBody?.content?.['application/json']?.schema) {
                  let schema = requestBody.content['application/json'].schema;
                  schema = this.resolveRefs(schema, schemas);
                  if (schema) {
                    requestBodySchema = JSON.stringify(schema, null, 2);
                  }
                }
              }
              const endpoint: CreateSourceSystemEndpointDTO = {
                endpointPath: formattedPath,
                httpRequestType: method.toUpperCase(),
                requestBodySchema
              };
              endpointsToCreate.push(endpoint);
            }
          }
        }
      }
      if (endpointsToCreate.length > 0) {
        this.endpoints = endpointsToCreate;
        this.endpointSvc.apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, endpointsToCreate)
          .subscribe({
            next: () => {
              this.loadEndpoints();
            },
            error: () => {}
          });
      }
    } finally {
      this.importing = false;
    }
  }

  /**
   * Ensures a parameter name is wrapped in curly braces {paramName}.
   */
  private ensureBraces(paramName: string): string {
    const cleanName = paramName.replace(/[{}]/g, '');
    return `{${cleanName}}`;
  }

  /**
   * Extracts path parameter names from an endpoint path.
   */
  private extractPathParameters(endpointPath: string): string[] {
    const pathParamRegex = /\{([^}]+)\}/g;
    const matches: string[] = [];
    let match: RegExpExecArray | null;
    while ((match = pathParamRegex.exec(endpointPath)) !== null) {
      matches.push(this.ensureBraces(match[1]));
    }
    const altPatterns = [
      /:(\w+)/g,
      /<(\w+)>/g,
      /\[(\w+)\]/g
    ];
    altPatterns.forEach(pattern => {
      let m: RegExpExecArray | null;
      while ((m = pattern.exec(endpointPath)) !== null) {
        const withBraces = this.ensureBraces(m[1]);
        if (!matches.includes(withBraces)) {
          matches.push(withBraces);
        }
      }
    });
    return matches;
  }

  /**
   * Saves discovered endpoints to the backend.
   */
  private saveDiscoveredEndpoints(discoveredEndpoints: Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[]}>) {
    if (discoveredEndpoints.length === 0) {
      this.importing = false;
      return;
    }
    const endpointDTOs = discoveredEndpoints.map(item => item.endpoint);
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, endpointDTOs)
      .subscribe({
        next: () => {
          this.loadEndpoints();
          setTimeout(() => {}, 1000);
        },
        error: () => {
          this.importing = false;
        }
      });
  }

  /**
   * Validator to ensure path parameter format: starts with { and ends with } with at least one character inside.
   */
  private pathParamFormatValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const val = control.value as string;
      if (typeof val === 'string' && val.match(/^\{[A-Za-z0-9_]+\}$/)) {
        return null;
      }
      return { invalidPathParamFormat: true };
    };
  }

  /**
   * Formats a path with parameters.
   */
  private formatPathWithParameters(path: string, parameters: any[]): string {
    let formattedPath = path;
    const pathParams = parameters?.filter(param => param.in === 'path') || [];
    pathParams.forEach(param => {
      const paramName = param.name;
      const patterns = [
        { regex: new RegExp(`:${paramName}\\b`, 'g'), replacement: `{${paramName}}` },
        { regex: new RegExp(`<${paramName}>`, 'g'), replacement: `{${paramName}}` },
        { regex: new RegExp(`\\[${paramName}\\]`, 'g'), replacement: `{${paramName}}` },
        { 
          regex: new RegExp(`(?<!\\{|:|<|\\[)\\b${paramName}\\b(?!\\}|>|\\])(?=/|$)`, 'g'), 
          replacement: `{${paramName}}` 
        }
      ];
      patterns.forEach(pattern => {
        formattedPath = formattedPath.replace(pattern.regex, pattern.replacement);
      });
    });
    return formattedPath;
  }

  /**
   * Recursively resolves $ref in OpenAPI schemas.
   */
  private resolveRefs(schema: any, schemas: any, seen = new Set()): any {
    if (!schema) return schema;
    if (schema.$ref) {
      if (seen.has(schema.$ref)) return {};
      seen.add(schema.$ref);
      const refPath = schema.$ref.replace(/^#\//, '').split('/');
      let resolved = schemas;
      for (const part of refPath.slice(2)) {
        resolved = resolved?.[part];
      }
      if (!resolved) return {};
      return this.resolveRefs(resolved, schemas, seen);
    }
    if (schema.properties) {
      const newProps: any = {};
      for (const [key, value] of Object.entries(schema.properties)) {
        newProps[key] = this.resolveRefs(value, schemas, seen);
      }
      return { ...schema, properties: newProps };
    }
    if (schema.items) {
      return { ...schema, items: this.resolveRefs(schema.items, schemas, seen) };
    }
    for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
      if (schema[keyword]) {
        return {
          ...schema,
          [keyword]: schema[keyword].map((s: any) => this.resolveRefs(s, schemas, seen))
        };
      }
    }
    if (typeof schema.additionalProperties === 'object' && schema.additionalProperties !== null) {
      return {
        ...schema,
        additionalProperties: this.resolveRefs(schema.additionalProperties, schemas, seen)
      };
    }
    return schema;
  }

  public currentOpenApiSpec: string | any = '';

  /**
   * Resolves a $ref schema from the current OpenAPI spec (recursively).
   */
  private resolveSchemaFromOpenApi(schemaRef: any): any {
    if (!this.currentOpenApiSpec || !schemaRef.includes('$ref')) {
      try {
        return typeof schemaRef === 'string' ? JSON.parse(schemaRef) : schemaRef;
      } catch {
        return schemaRef;
      }
    }
    try {
      const openApiJson = typeof this.currentOpenApiSpec === 'string'
        ? JSON.parse(this.currentOpenApiSpec)
        : this.currentOpenApiSpec;
      const ref = typeof schemaRef === 'string'
        ? JSON.parse(schemaRef).$ref
        : schemaRef.$ref;
      if (ref && ref.startsWith('#/components/schemas/')) {
        const schemaName = ref.replace('#/components/schemas/', '');
        const schema = openApiJson.components?.schemas?.[schemaName];
        if (schema) {
          if (schema.$ref) {
            return this.resolveSchemaFromOpenApi(JSON.stringify(schema));
          }
          return schema;
        }
      }
    } catch (e) {}
    return schemaRef;
    
  }
}
