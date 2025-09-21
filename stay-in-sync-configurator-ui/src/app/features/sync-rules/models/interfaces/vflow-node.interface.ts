import { ComponentNode } from "ngx-vflow";
import { NodeType } from "../enums/node-type.enum";
import { NodeMenuItem } from "./node-menu-item.interface";

/**
 * The custom vflow node interface which is adding a data `VFlowNodeData` attribute
 */
export interface CustomVFlowNode extends ComponentNode<VFlowNodeData> {
  data: VFlowNodeData;
  contextMenuItems: NodeMenuItem[];
}

/**
 * The interface of a vflow node's data
 */
export interface VFlowNodeData {
  name: string;
  nodeType: NodeType;

  // For PROVIDER nodes
  arcId?: number;
  jsonPath?: string;

  // For CONSTANT nodes
  value?: any;

  // For LOGIC nodes
  operatorType?: string;
  inputTypes?: string[];
  outputType?: string;
  inputLimit?: number;
}
