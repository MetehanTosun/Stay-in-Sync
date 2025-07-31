export interface LogEntry {
  level?: string;
  timestamp: string;
  message: string;
  nodeId?: string;
  labels: { [key: string]: string };
  stream?: string;
}
