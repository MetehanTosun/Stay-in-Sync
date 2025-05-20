// node.model.ts
export interface Node {
  id: string;
  type: 'API' | 'ASS' | 'Syncnode' | 'EDC';
  status: 'aktiv' | 'inaktiv' | 'fehlerhaft';
  connections: NodeConnection[];
}

export interface NodeConnection {
  targetNodeId: string;
  status: 'aktiv' | 'inaktiv' | 'fehlerhaft';
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
