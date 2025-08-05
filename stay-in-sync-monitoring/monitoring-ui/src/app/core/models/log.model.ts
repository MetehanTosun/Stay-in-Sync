export interface LogEntry {
  rawMessage?: string;
  caller?: string;
  component?: string;
  level?: string;
  timestamp: string;
  message: string;
  nodeId?: string;
  labels: { [key: string]: string };
  stream?: string;
}
