export interface SourceSystem{
  id?: number;
  name?: string;
  apiUrl?: string;
  description?: string;
  apiType?: string; // REST, AAS
  openAPI?: string;
  //systemAuthConfig?: SyncSystemAuthConfig;
}
