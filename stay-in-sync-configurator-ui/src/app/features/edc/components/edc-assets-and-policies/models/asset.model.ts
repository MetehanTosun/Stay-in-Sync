export interface EDCDataAddress {
  id?: string;
  jsonLDType: string;
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
  assetId: string;
  name?: string;            // Name des Assets aus asset:prop:name
  url: string;
  type: string;
  contentType: string;
  description?: string;
  targetEDCId: string;       // UUID aus dem Backend
  dataAddress: EDCDataAddress;
  properties?: EDCProperty;
}
