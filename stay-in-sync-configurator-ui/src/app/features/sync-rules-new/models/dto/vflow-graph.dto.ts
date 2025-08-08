import { NodeType } from "..";

export interface VFlowGraphDTO {
  nodes: VFlowNode[];
  edges: VFlowEdge[];
  errors: any[];
}

export interface VFlowNode {
  id: number;
  point: { x: number, y: number};
  type: NodeType;
  width: number;
  height: number;
  data: VFlowNodeData;
}

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
  category?: string;
  inputNodes?: { id: number; orderIndex: number }[];

  outputType?: string;
  inputTypes?: string[];
  inputLimit?: number;
}

export interface VFlowEdge {
  id: string;
  source: string;
  target: string;
  targetHandle?: string;
  type?: string;
}
