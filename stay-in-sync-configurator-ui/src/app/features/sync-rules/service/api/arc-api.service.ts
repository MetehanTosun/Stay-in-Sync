import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ConfigurationResource } from '../../models/dto/configuration-resource.dto';

/**
 * An injectable singleton class which allows the communication with the backend
 * regarding the endpoint for ARCs.
 */
@Injectable({
  providedIn: 'root'
})
export class ArcAPIService {
  private readonly apiUrl = '/api/config/source-system/endpoint/request-configuration/';

  constructor(private http: HttpClient) { }

  //#region Read
  /**
   * Sends a GET request for all configuration resources.
   *
   * @returns a collection of all configuration resources
   */
  private getConfigurationResources(): Observable<ConfigurationResource[]> {
    return this.http.get<ConfigurationResource[]>(this.apiUrl)
  }

  /**
   * Retrieves JSON paths generated from configuration resources.
   */
  getJsonPaths(): Observable<{ [key: string]: string }> {
    return this.getConfigurationResources().pipe(
      map(configResources => this.generateJsonPaths(configResources))
    );
  }
  //#endregion

  //#region JSONpath Generation
  /**
   * Generates a mapping of JSON paths and their respective data types based on given configResources.
   *
   * @param configResources An array of ConfigurationResource objects containing metadata for generating JSON paths
   * @returns A mapping of JSON paths with their respective data type
   */
  private generateJsonPaths(configResources: ConfigurationResource[]): { [key: string]: string } {
    const jsonPaths: { [key: string]: string } = {};

    configResources.forEach(resource => {
      const pathPrefix = `${resource.sourceSystemName}.${resource.alias}`;
      const parsedInterfaces = this.parseInterfaces(resource.responseDts);

      // Generate paths based from the Root interface
      if (parsedInterfaces.interfaces['Root']) {
        parsedInterfaces.interfaces['Root'].forEach(prop => {
          const rootPropType = parsedInterfaces.propertyTypes['Root'][prop];

          // Check if property is an array (marked with [*])
          if (rootPropType.includes('[*]') && parsedInterfaces.interfaces[rootPropType.replace('[*]', '')]) {

            // Generate JSON paths for array properties
            parsedInterfaces.interfaces[rootPropType.replace('[*]', '')].forEach(arrayItemProp => {
              const arrayItemType = parsedInterfaces.propertyTypes[rootPropType.replace('[*]', '')][arrayItemProp];
              jsonPaths[`${pathPrefix}.${prop}[*].${arrayItemProp}`] = arrayItemType;
            });

          } else {
            // Generate JSON paths for non-array properties
            jsonPaths[`${pathPrefix}.${prop}`] = rootPropType;
          }
        });
      }

    });

    return jsonPaths;
  }

  /**
   * Parses the given responseDts string to extract interfaces and their properties.
   *
   * @param responseDts A string representation of TypeScript code describing the ARC tree.
   * @returns An object containing mappings of interfaces to their properties and property types.
   */
  private parseInterfaces(responseDts: string): {
    interfaces: { [key: string]: string[] };
    propertyTypes: { [key: string]: { [key: string]: string } };
  } {
    const INTERFACE_REGEX = /interface\s+(\w+)\s*\{([^{}]*)\}/g;
    const interfaces: { [key: string]: string[] } = {};
    const propertyTypes: { [key: string]: { [key: string]: string } } = {};

    let interfaceMatch;

    // For each regex match loop (for each found interface)
    while ((interfaceMatch = INTERFACE_REGEX.exec(responseDts)) !== null) {
      const interfaceName = interfaceMatch[1];
      const interfaceProperties = interfaceMatch[2];

      propertyTypes[interfaceName] = {};
      interfaces[interfaceName] = this.extractProperties(interfaceProperties, propertyTypes[interfaceName]);
    }

    return { interfaces, propertyTypes };
  }

  /**
   * Extracts properties from the given interfaceProperties string and updates the propertyTypes mapping.
   *
   * @param interfaceProperties A string containing the properties of the interface.
   * @param propertyTypes A mapping to store the properties and their respective types.
   * @returns An array of property names extracted from the interface.
   */
  private extractProperties(
    interfaceProperties: string,
    propertyTypes: { [key: string]: string }
  ): string[] {
    return interfaceProperties
      .split(/[;\n]/) // Split into individual properties
      .map(prop => prop.trim())
      .filter(prop => prop.length > 0)
      .map(prop => this.parseProperty(prop, propertyTypes))
      .filter(prop => prop.length > 0);
  }

  /**
   * Parses a single property definition and updates the propertyTypes mapping.
   * Marks array types with [*] directly in the propertyTypes mapping.
   *
   * @param prop A string representing a single property line within the interface.
   * @param propertyTypes A mapping to store the property name and its type.
   * @returns The name of the property if valid, otherwise an empty string.
   */
  private parseProperty(
    prop: string,
    propertyTypes: { [key: string]: string }
  ): string {
    const colonIndex = prop.indexOf(':'); // Separate property from type

    if (colonIndex > 0) {
      const propName = prop.substring(0, colonIndex).trim();
      let propType = prop.substring(colonIndex + 1).trim();

      // Mark array properties
      if (propType.endsWith('[]')) {
        const arrayType = propType.replace('[]', '');
        propType = `${arrayType}[*]`;
      }

      // Store the property type
      propertyTypes[propName] = propType;

      return propName;
    }
    return '';
  }
  //#endregion
}
