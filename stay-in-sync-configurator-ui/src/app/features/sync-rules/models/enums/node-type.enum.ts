import { MessageService } from "primeng/api";
import { ConfigNodeComponent, ConstantNodeComponent, FinalNodeComponent, LogicNodeComponent, ProviderNodeComponent, SchemaNodeComponent } from "../../components";

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
  ,
  /**
   * Schema nodes containing a JSON schema
   */
  SCHEMA = 'SCHEMA'
}

/**
 * Returns the code component class of a given node type
 *
 * @param nodeType The type of the node
 * @param messageService The service to display error messages
 * @returns The corresponding component class
 */
export function getNodeComponent(nodeType: NodeType, messageService: MessageService) {
  switch (nodeType) {
    case NodeType.CONSTANT: return ConstantNodeComponent;
    case NodeType.PROVIDER: return ProviderNodeComponent;
    case NodeType.LOGIC: return LogicNodeComponent;
    case NodeType.SCHEMA: return SchemaNodeComponent;
    case NodeType.CONFIG: return ConfigNodeComponent;
    case NodeType.FINAL: return FinalNodeComponent;
    default:
      messageService.add({
        severity: 'error',
        summary: 'Unknown NodeType',
        detail: 'An unknown node type was attempted to be accessed. \n Please check the logs or the console.'
      });
      throw Error("Unknown NodeType");
  }
}

