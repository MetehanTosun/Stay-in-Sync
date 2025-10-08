/**
 * Interface of the necessary data of a configuration resource for this component (ARCs)
 */
export interface ConfigurationResource {
  alias: string,
  sourceSystemName: string,

  /**
   * String representation of TypeScript code describing the ARC tree
   */
  responseDts: string,
}
