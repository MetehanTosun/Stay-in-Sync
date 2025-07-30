import * as d3 from 'd3';

export interface Node {
  id: string;
  type: 'SourceSystem' | 'ASS' | 'SyncNode' | 'TargetSystem';
  label: string ;
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


