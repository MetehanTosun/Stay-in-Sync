/**
 * Enum representing the different types of nodes in a transformation rule graph
 */
export enum NodeType {
  /**
   * Provider nodes represent data sources from ARCs / JSON paths
   */
  PROVIDER = 'PROVIDER',

  /**
   * Logic nodes represent operations
   */
  LOGIC = 'LOGIC',

  /**
   * Constant nodes represent static values
   */
  CONSTANT = 'CONSTANT',

  /**
   * Final nodes represent the end of the boolean condition / the output of the transformation graph
   */
  FINAL = 'FINAL',

  /**
   * Final nodes control the behavior of a transformation graphs logic
   */
  CONFIG = 'CONFIG'
}
