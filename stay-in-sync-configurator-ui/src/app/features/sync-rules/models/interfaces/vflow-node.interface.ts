import { ComponentNode } from "ngx-vflow";
import { NodeType } from "../enums/node-type.enum";
import { NodeMenuItem } from "./node-menu-item.interface";

/**
 * Base data interface for all node types
 */
export interface BaseNodeData {
  name: string;
  nodeType: NodeType;
}

/**
 * Data interface for constant nodes
 */
export interface ConstantNodeData extends BaseNodeData {
  value: unknown;
  outputType: string;
}

/**
 * Data interface for provider nodes
 */
export interface ProviderNodeData extends BaseNodeData {
  jsonPath: string;
  outputType: string;
}

/**
 * Data interface for logic nodes
 */
export interface LogicNodeData extends BaseNodeData {
  /**
   * Categorizes the logic node into certain groups (e.g. operator for numbers, strings, ...)
   */
  operatorType: string;

  /**
   * Contains the data types the inputs of the logic node uses.
   *
   * The index represents the order the input data types are used by the node from the top to bottom
   */
  inputTypes: string[];

  outputType: string;
}

/**
 * Data interface for config nodes
 */
export interface ConfigNodeData extends BaseNodeData {
  /**
   * While inputTypes is set to ["ANY"], it should only accept the outputs of provider nodes
   */
  inputTypes: ["ANY"];
  outputType: "BOOLEAN";
  changeDetectionMode: "AND" | "OR";
  changeDetectionActive: boolean;
  timeWindowMillis: number;
}

/**
 * Data interface for final nodes
 */
export interface FinalNodeData extends BaseNodeData {
  inputTypes: ["BOOLEAN"];
}

/**
 * Data interface for schema nodes
 */
export interface SchemaNodeData extends BaseNodeData {
  /**
   * Contains the JSON Schema stored as a string
   */
  value: string;
  /**
   * Optional output type metadata (if needed by other nodes)
   */
  outputType?: "JSON";
}

/**
 * Union type for all node data types
 */
export type VFlowNodeData =
  | ConstantNodeData
  | ProviderNodeData
  | LogicNodeData
  | ConfigNodeData
  | FinalNodeData
  | SchemaNodeData;

/**
 * Custom interface of this components vflow nodes
 */
export interface CustomVFlowNode extends ComponentNode<VFlowNodeData> {
  /**
   * Additional data contained within the node
   */
  data: VFlowNodeData;

  /**
   * A collection of all context menu actions available for the node
   */
  contextMenuItems: NodeMenuItem[];
}
