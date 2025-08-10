/**
 * Metadata interface of transformation rules
 */
export interface TransformationRule {
  id: number;
  name: string;
  description: string;
  graphStatus: 'FINALIZED' | 'DRAFT';

  /**
   * The ID of the parent transformation this rule belongs to.
   */
  transformationId: number; // TODO-s check if needed
}
