import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ConfigurationResourceDTO } from '../../models/dto/configuration-resource.dto';

/**
 * An injectable singleton class which allows the communication with the backend
 * regarding the endpoint to receive a payload.
 */
@Injectable({
  providedIn: 'root'
})
export class ArcAPIService {
  private readonly apiUrl = '/api/config/source-system/endpoint/request-configuration/';

  constructor(private http: HttpClient) { }

  //#region Read Operations
  /**
   * Sends a GET request to read a json paths payload.
   *
   * @param jsonPath
   * @returns the json paths payload
   */
  getArcPayload(jsonPath: string): Observable<unknown> {
    return this.http.get<unknown>(`${this.apiUrl}/`);
  }

  getConfigurationResources(): Observable<ConfigurationResourceDTO[]> {
    return this.http.get<ConfigurationResourceDTO[]>(this.apiUrl)
  }

  getJsonPaths(): Observable<{ [key: string]: string }> {
    return this.getConfigurationResources().pipe(
      map(configResources => {
        const jsonPaths: { [key: string]: string } = {};

        configResources.forEach(resource => {
          if (resource.responseDts && resource.sourceSystemName && resource.alias) {
            const pathPrefix = `${resource.sourceSystemName}.${resource.alias}`;

            // Extract interface definitions
            const interfaceRegex = /interface\s+(\w+)\s*\{([^{}]*)\}/g;
            let interfaceMatch;

            const interfaces: { [key: string]: string[] } = {};
            const arrayProperties: { [key: string]: string } = {};
            const propertyTypes: { [key: string]: { [key: string]: string } } = {};

            // Parse all interfaces
            while ((interfaceMatch = interfaceRegex.exec(resource.responseDts)) !== null) {
              const interfaceName = interfaceMatch[1];
              const interfaceContent = interfaceMatch[2];

              propertyTypes[interfaceName] = {};

              // Extract properties from interface
              const properties = interfaceContent.split(/[;\n]/)
                .map(prop => prop.trim())
                .filter(prop => prop.length > 0)
                .map(prop => {
                  const colonIndex = prop.indexOf(':');
                  if (colonIndex > 0) {
                    const propName = prop.substring(0, colonIndex).trim();
                    const propType = prop.substring(colonIndex + 1).trim();

                    // Store the property type
                    propertyTypes[interfaceName][propName] = propType;

                    // Check if it's an array
                    if (propType.endsWith('[]')) {
                      const arrayType = propType.replace('[]', '');
                      arrayProperties[propName] = arrayType;
                    }
                    return propName;
                  }
                  return prop;
                })
                .filter(prop => prop.length > 0);

              interfaces[interfaceName] = properties;
            }

            // Generate paths based on the Root interface structure
            if (interfaces['Root']) {
              interfaces['Root'].forEach(prop => {
                const rootPropType = propertyTypes['Root'][prop];

                // Check if property is an array
                if (arrayProperties[prop] && interfaces[arrayProperties[prop]]) {
                  interfaces[arrayProperties[prop]].forEach(arrayItemProp => {
                    const arrayItemType = propertyTypes[arrayProperties[prop]][arrayItemProp];
                    jsonPaths[`${pathPrefix}.${prop}[*].${arrayItemProp}`] = arrayItemType;
                  });
                } else {
                  jsonPaths[`${pathPrefix}.${prop}`] = rootPropType;
                }
              });
            }
          }
        });

        return jsonPaths;
      })
    );
  }
  //#endregion
}
