export interface Asset {
  id: string;             // UUID aus dem DTO
  assetId: string;
  url: string;
  type: string;
  contentType: string;
  description?: string;   // optional
  targetEDCId: string;    // UUID aus dem DTO
}
