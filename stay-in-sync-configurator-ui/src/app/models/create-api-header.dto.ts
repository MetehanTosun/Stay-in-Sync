import { ApiRequestHeaderType } from "./api-header.dto";

export interface CreateApiHeaderDto {
    headerType: ApiRequestHeaderType;
    headerName: string;
  }