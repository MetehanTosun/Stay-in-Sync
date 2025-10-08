/**
 * Interface of transformation (synchronization) rules
 */
export interface TransformationRule {
  id: number;
  name: string;
  description?: string;
  graphStatus: 'FINALIZED' | 'DRAFT';
}
