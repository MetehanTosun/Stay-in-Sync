// node.model.ts
export interface Node {
  id: string;
  type: 'API' | 'ASS' | 'Syncnode' | 'EDC';
  status: 'active' | 'inactive' | 'error';
  connections: NodeConnection[];
  x ?: number;
  y ?: number;
}

export interface NodeConnection {
  targetNodeId: string;
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
