/**
 * Represents the result of a transformation execution.
 *
 * This interface is used both client-side (UI) and server-side to exchange
 * information about transformation runs, including identifiers, input/output
 * data, and possible error details.
 *
 * @property transformationId - Numeric identifier of the executed transformation.
 * @property jobId - Identifier of the job that triggered the transformation.
 * @property scriptId - Identifier of the transformation script used during execution.
 * @property validExecution - Indicates whether the transformation completed successfully.
 * @property sourceData - Input JSON data used by the transformation.
 * @property outputData - Output JSON data produced by the transformation.
 * @property errorInfo - Optional textual information about any error encountered.
 *
 * @author Mohammed-Ammar Hassnou
 */
export interface TransformationResultDTO {
  transformationId: number;
  jobId: string;
  scriptId: string;
  validExecution: boolean;
  sourceData: any | null;
  outputData: any | null;
  errorInfo?: string | null;
}

/**
 * Represents a snapshot of a transformation at a specific moment in time.
 *
 * A snapshot stores metadata such as its unique ID, creation timestamp, and
 * the associated {@link TransformationResultDTO}. It is typically used to
 * reproduce or debug past transformation states.
 *
 * @property snapshotId - Unique identifier of the snapshot.
 * @property createdAt - ISO timestamp of when the snapshot was created.
 * @property transformationResult - Result data captured at the time of the snapshot.
 *
 * @author Mohammed-Ammar Hassnou
 */
export interface SnapshotDTO {
  snapshotId?: string;
  createdAt?: string;
  transformationResult?: TransformationResultDTO;
}
