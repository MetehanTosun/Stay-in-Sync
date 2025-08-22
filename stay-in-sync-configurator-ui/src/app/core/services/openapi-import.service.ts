import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { load as parseYAML } from 'js-yaml';
import { ApiEndpointQueryParamResourceService } from '../../features/source-system/service/apiEndpointQueryParamResource.service';
import { ApiEndpointQueryParamDTO } from '../../features/source-system/models/apiEndpointQueryParamDTO';
import { ApiEndpointQueryParamType } from '../../features/source-system/models/apiEndpointQueryParamType';

export interface DiscoveredEndpoint {
  endpointPath: string;
  httpRequestType: string;
  requestBodySchema?: string;
  responseBodySchema?: string;
}

export interface DiscoveredParam {
  name: string;
  in: 'path' | 'query';
  schemaType?: 'STRING'|'INTEGER'|'NUMBER'|'BOOLEAN'|'ARRAY';
  required?: boolean;
  example?: string;
}

@Injectable({ providedIn: 'root' })
export class OpenApiImportService {
  constructor(private http: HttpClient, private paramApi: ApiEndpointQueryParamResourceService) {}

  async discoverEndpointsFromSpecUrl(baseUrl: string): Promise<DiscoveredEndpoint[]> {
    const candidateUrls = [
      baseUrl,
      `${baseUrl}/swagger.json`,
      `${baseUrl}/openapi.json`,
      `${baseUrl}/v2/swagger.json`,
      `${baseUrl}/api/v3/openapi.json`,
      `${baseUrl}/swagger/v1/swagger.json`,
      `${baseUrl}/api-docs`,
      `${baseUrl}/openapi.yaml`,
      `${baseUrl}/openapi.yml`
    ];
    for (const url of candidateUrls) {
      try {
        const isJson = url.endsWith('.json') || (!url.endsWith('.yaml') && !url.endsWith('.yml'));
        const raw = await this.http.get(url, { responseType: isJson ? 'json' : 'text' as 'json' }).toPromise();
        const spec: any = isJson ? raw : parseYAML(raw as string);
        const endpoints = this.discoverEndpointsFromSpec(spec);
        if (endpoints.length) return endpoints;
      } catch { /* try next */ }
    }
    return [];
  }

  discoverEndpointsFromSpec(spec: any): DiscoveredEndpoint[] {
    if (!spec || !spec.paths) return [];
    const endpoints: DiscoveredEndpoint[] = [];
    const schemas = spec.components?.schemas || {};
    for (const [path, pathItem] of Object.entries(spec.paths)) {
      for (const [method, operation] of Object.entries(pathItem as any)) {
        const m = (method as string).toLowerCase();
        if (!['get','post','put','delete','patch','head','options'].includes(m)) continue;
        const op: any = operation;
        let requestBodySchema: string | undefined;
        let responseBodySchema: string | undefined;
        if (['post','put','patch'].includes(m)) {
          const req = op.requestBody?.content?.['application/json']?.schema;
          if (req) requestBodySchema = JSON.stringify(this.resolveRefs(req, schemas), null, 2);
        }
        const success = ['200','201','202','204'];
        for (const sc of success) {
          const resp = op.responses?.[sc]?.content?.['application/json']?.schema;
          if (resp) { responseBodySchema = JSON.stringify(this.resolveRefs(resp, schemas), null, 2); break; }
        }
        endpoints.push({ endpointPath: path as string, httpRequestType: (method as string).toUpperCase(), ...(requestBodySchema?{requestBodySchema}:{}) , ...(responseBodySchema?{responseBodySchema}:{}) });
      }
    }
    return endpoints;
  }

  discoverParamsFromSpec(spec: any): Record<string, DiscoveredParam[]> {
    const map: Record<string, DiscoveredParam[]> = {};
    if (!spec || !spec.paths) return map;
    for (const [path, pathItem] of Object.entries(spec.paths)) {
      const pathParams = (pathItem as any).parameters || [];
      for (const [method, operation] of Object.entries(pathItem as any)) {
        const m = (method as string).toLowerCase();
        if (!['get','post','put','delete','patch','head','options'].includes(m)) continue;
        const op: any = operation;
        const all = [...pathParams, ...(op.parameters || [])];
        const normalizedPath = this.normalizePath(path as string, all);
        const key = `${(method as string).toUpperCase()} ${normalizedPath}`;
        const params: DiscoveredParam[] = [];
        for (const p of all) {
          if (!p?.in || !['path','query'].includes(p.in)) continue;
          const t = this.mapSchemaType(p.schema?.type);
          params.push({ name: p.name, in: p.in, schemaType: t, required: !!p.required, example: p.example });
        }
        if (params.length) map[key] = params;
      }
    }
    return map;
  }

  discoverFromSpec(spec: any): { endpoints: DiscoveredEndpoint[]; paramsByKey: Record<string, DiscoveredParam[]> } {
    return { endpoints: this.discoverEndpointsFromSpec(spec), paramsByKey: this.discoverParamsFromSpec(spec) };
  }

  async persistParamsForEndpoint(endpointId: number, params: DiscoveredParam[]): Promise<void> {
    if (!endpointId || !params?.length) return;
    for (const p of params) {
      const dto: ApiEndpointQueryParamDTO = {
        paramName: p.name,
        queryParamType: p.in === 'path' ? ApiEndpointQueryParamType.Path : ApiEndpointQueryParamType.Query,
        // include schemaType if discovered; backend expects SchemaType enum
        // @ts-ignore
        schemaType: p.schemaType as any,
        // Backend expects a JSON array; Set serializes poorly. Send empty array when none.
        // @ts-ignore
        values: [],
      };
      await this.paramApi.apiConfigEndpointEndpointIdQueryParamPost(endpointId, dto).toPromise();
    }
  }

  private mapSchemaType(openapiType?: string): DiscoveredParam['schemaType'] {
    switch ((openapiType || '').toLowerCase()) {
      case 'string': return 'STRING';
      case 'integer': return 'INTEGER';
      case 'number': return 'NUMBER';
      case 'boolean': return 'BOOLEAN';
      case 'array': return 'ARRAY';
      default: return undefined;
    }
  }

  private normalizePath(path: string, parameters: any[]): string {
    let formattedPath = path;
    const pathParams = parameters?.filter((p: any) => p.in === 'path') || [];
    pathParams.forEach((param: any) => {
      const name = param.name;
      const patterns = [
        { regex: new RegExp(`:${name}\\b`, 'g'), replacement: `{${name}}` },
        { regex: new RegExp(`<${name}>`, 'g'), replacement: `{${name}}` },
        { regex: new RegExp(`\\[${name}\\]`, 'g'), replacement: `{${name}}` },
        { regex: new RegExp(`(?<!\\{|:|<|\\])\\b${name}\\b(?!\\}|>|\\])(?=/|$)`, 'g'), replacement: `{${name}}` }
      ];
      patterns.forEach(p => { formattedPath = formattedPath.replace(p.regex, p.replacement); });
    });
    return formattedPath;
  }

  private resolveRefs(schema: any, schemas: any, seen = new Set()): any {
    if (!schema) return schema;
    if (schema.$ref) {
      if (seen.has(schema.$ref)) return {};
      seen.add(schema.$ref);
      const refPath = schema.$ref.replace(/^#\//, '').split('/');
      let resolved = schemas;
      for (const part of refPath.slice(2)) resolved = resolved?.[part];
      if (!resolved) return {};
      return this.resolveRefs(resolved, schemas, seen);
    }
    if (schema.properties) {
      const newProps: any = {};
      for (const [k, v] of Object.entries(schema.properties)) newProps[k] = this.resolveRefs(v, schemas, seen);
      return { ...schema, properties: newProps };
    }
    if (schema.items) return { ...schema, items: this.resolveRefs(schema.items, schemas, seen) };
    for (const kw of ['allOf','anyOf','oneOf']) if ((schema as any)[kw]) return { ...schema, [kw]: (schema as any)[kw].map((s: any) => this.resolveRefs(s, schemas, seen)) };
    if (typeof schema.additionalProperties === 'object' && schema.additionalProperties !== null) return { ...schema, additionalProperties: this.resolveRefs(schema.additionalProperties, schemas, seen) };
    return schema;
  }
}


