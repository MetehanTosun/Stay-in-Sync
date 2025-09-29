import { NodeType, VFlowNodeData } from "..";

/**
 * The DTO representation the backend sends of the vflow graph
 */
export interface VFlowGraphDTO {
  nodes: VFlowNodeDTO[];
  edges: VFlowEdgeDTO[];
  errors?: any[];
}

/**
 * The DTO representation the backend sends of a vflow node
 */
export interface VFlowNodeDTO {
  id: string;
  point: { x: number, y: number};
  type: NodeType;
  width?: number;
  height?: number;
  data: VFlowNodeData;
}

/**
 * The DTO representation the backend sends of a vflow edge
 */
export interface VFlowEdgeDTO {
  id: string;
  source: string;
  target: string;
  targetHandle?: string;
  type?: string;
}
