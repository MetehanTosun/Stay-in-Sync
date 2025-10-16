export interface EDCDataAddress {
  id?: string;
  type: string;
  base_url: string;
  proxyPath: boolean;
  proxyQueryParams: boolean;
}

export interface EDCProperty {
  id?: string;
  description: string;
}

export interface Asset {
  id?: string;               // UUID aus dem Backend
  assetId: string;           // ID des Assets im EDC
  name: string;              // Name des Assets
  description?: string;      // Beschreibung des Assets
  contentType: string;       // Content-Type (z.B. application/json)
  type: string;              // Typ des Assets (z.B. HttpData)
  url: string;               // URL für den Zugriff auf das Asset
  targetEDCId: string;       // UUID der EDC-Instanz
  dataAddress: EDCDataAddress;
  properties?: EDCProperty[];
  queryParams?: string;
  headers?: { [key: string]: string };
}
