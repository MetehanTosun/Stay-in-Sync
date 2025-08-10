/**
 * Metadata interface of logic operators
 */
export interface LogicOperator {
  operatorName: string;
  description: string;
  category: string;
  inputTypes: string[];
  outputType: string;
}
