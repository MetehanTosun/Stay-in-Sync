/**
 * Enum representing the different types of nodes in a transformation rule graph
 */
export enum NodeType {
  /**
   * Provider nodes represent data sources from ARCs / JSON paths
   */
  PROVIDER = 'provider',

  /**
   * Logic nodes represent operations
   */
  LOGIC = 'logic',

  /**
   * Constant nodes represent static values
   */
  CONSTANT = 'constant',

  /**
   * Final nodes represent the end of the boolean condition / the output of the transformation graph
   */
  FINAL = 'final'
}
