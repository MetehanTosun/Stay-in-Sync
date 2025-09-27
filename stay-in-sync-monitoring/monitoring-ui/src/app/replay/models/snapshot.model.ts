export interface TransformationResultDTO {
  transformationId: number;
  jobId: string;
  scriptId: string;
  validExecution: boolean;
  sourceData: any | null;
  outputData: any | null;
  errorInfo?: string | null;
}

export interface SnapshotDTO {
  snapshotId: string;
  createdAt: string;
  transformationResult: TransformationResultDTO;
}
