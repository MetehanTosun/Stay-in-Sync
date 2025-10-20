/**
 * Metadata interface of logic operators
 */
export interface LogicOperatorMetadata {
  operatorName: string;
  description: string;

  /**
   * The category the logic node belongs to (e.g. operator for numbers, strings, ...)
   */
  category: string;

  inputTypes: string[];
  outputType: string;
}

/**
 * Interface for operators grouped by their category
 */
export type GroupedOperators = Record<string, LogicOperatorMetadata[]>;
