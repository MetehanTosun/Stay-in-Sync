export interface LogEntry {
  syncJobId?: string;
  rawMessage?: string;
  service?: string;
  component?: string;
  level?: string;
  timestamp: string;
  message: string;
  nodeId?: string;
  labels: { [key: string]: string };
  stream?: string;
}
