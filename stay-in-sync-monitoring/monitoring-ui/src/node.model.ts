// node.model.ts
import * as d3 from 'd3';

export interface Node {
  id: string;
  type: 'API' | 'ASS' | 'Syncnode' | 'EDC';
  status: 'active' | 'inactive' | 'error';
  connections: NodeConnection[];
  x ?: number;
  y ?: number;
  fx ?: number;
  fy ?: number;
}

export interface NodeConnection extends d3.SimulationLinkDatum<Node>{
  source: Node | string;
  target: Node | string;
  status: 'active' | 'inactive' | 'error';
}

// system-load.model.ts
export interface SystemLoad {
  month: string;
  loadPercent: number;
}

// log.model.ts
export interface LogEntry {
  timestamp: string;
  message: string;
  nodeId?: string;
}
