import { NodeType } from "..";

/**
 * The DTO representation the backend sends of the vflow graph
 */
export interface VFlowGraphDTO {
  nodes: VFlowNode[];
  edges: VFlowEdge[];
  errors: any[];
}

/**
 * The DTO representation the backend sends of a vflow node
 */
export interface VFlowNode {
  id: number;
  point: { x: number, y: number};
  type: NodeType;
  width: number;
  height: number;
  data: VFlowNodeData;
}

/**
 * The DTO representation the backend sends of a vflow node's data
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

/**
 * The DTO representation the backend sends of a vflow edge
 */
export interface VFlowEdge {
  id: string;
  source: string;
  target: string;
  targetHandle?: string;
  type?: string;
}
