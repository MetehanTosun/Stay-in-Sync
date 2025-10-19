import { NodeType, ValidationError, VFlowNodeData } from "..";

/**
 * The DTO representation the backend sends of the vflow graph
 */
export interface VFlowGraphDTO {
  nodes: VFlowNodeDTO[];
  edges: VFlowEdgeDTO[];
  errors?: ValidationError[];
}

/**
 * The DTO representation the backend sends of a vflow node
 */
export interface VFlowNodeDTO {
  id: string;
  point: { x: number, y: number };
  type: NodeType;
  width?: number;
  height?: number;

  /**
   * Additional data contained within the node
   */
  data: VFlowNodeData;
}

/**
 * The DTO representation the backend sends of a vflow edge
 */
export interface VFlowEdgeDTO {
  id: string;

  /**
   * ID of the source node
   */
  source: string;

  /**
   * ID of the target node
   */
  target: string;

  /**
   * Handle of the target node (used when the target node has multiple possible inputs)
   */
  targetHandle?: string;
}
