import {Component, OnDestroy, OnInit} from '@angular/core';
import { LogEntry } from '../../core/models/log.model';
import { LogService } from '../../core/services/log.service';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgClass, NgIf } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ActivatedRoute } from '@angular/router';
import {NodeMarkerService} from '../../core/services/node-marker.service';

@Component({
  selector: 'app-logs-panel',
  templateUrl: './logs-panel.component.html',
  imports: [
    FormsModule,
    DatePipe,
    NgClass,
    NgIf,
    TableModule
  ],
  standalone: true
})
export class LogsPanelComponent implements OnInit, OnDestroy {
  selectedNodeId?: string;

  private refreshIntervalId?: number;

  logs: LogEntry[] = [];
  loading = false;
  errorMessage = '';

  startTime = '';
  endTime = '';
  level = 'info';

  constructor(private logService: LogService, private route: ActivatedRoute, private nodeMarkerService: NodeMarkerService) {}

  ngOnInit() {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
    this.startTime = this.toDateTimeLocal(oneHourAgo);
    this.endTime = this.toDateTimeLocal(now);

    // Auf Query-Params hören
    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];
      this.fetchLogs();
    });

    // Alle 10 Sekunden endTime aktualisieren und Logs abrufen
    this.refreshIntervalId = window.setInterval(() => {
      this.endTime = this.toDateTimeLocal(new Date());
      this.fetchLogs();
    }, 10000);
  }

  ngOnDestroy() {
    // Timer bereinigen
    if (this.refreshIntervalId) {
      clearInterval(this.refreshIntervalId);
    }
  }

  fetchLogs() {
    this.loading = true;
    this.errorMessage = '';

    // Scroll-Position speichern
    const logContainer = document.querySelector('.log-filters');
    const scrollTop = logContainer ? logContainer.scrollTop : 0;

    const startNs = this.toNanoSeconds(new Date(this.startTime));
    const endNs = this.toNanoSeconds(new Date(this.endTime));

    this.logService.getLogs(startNs, endNs, this.level, this.selectedNodeId)
      .subscribe({
        next: logs => {
          this.logs = logs;

          const markedNodes: { [nodeId: string]: boolean } = {};
          logs.forEach(log => {
            if (log.level === 'error' && log.syncJobId) {
              markedNodes[log.syncJobId] = true;
            }
          });

          this.nodeMarkerService.updateMarkedNodes(markedNodes);
          this.loading = false;

          // Scroll-Position wiederherstellen
          if (logContainer) {
            logContainer.scrollTop = scrollTop;
          }
        },
        error: err => {
          console.error('Error fetching logs', err);
          this.errorMessage = 'Fehler beim Laden der Logs';
          this.loading = false;
        }
      });
  }

  onFilterChange() {
    this.fetchLogs();
  }

  buildFallbackMessage(log: LogEntry): string {
    const raw = (log as any).rawMessage || '';

    const component = this.extractValue(raw, 'component');
    const query = this.extractQuotedValue(raw, 'query');
    const returned = this.extractValue(raw, 'returned_lines');
    const duration = this.extractValue(raw, 'duration');
    const status = this.extractValue(raw, 'status');
    const caller = this.extractValue(raw, 'caller');

    if (component && query) {
      return `[${component}] Query ${query} returned ${returned || 0} lines (duration=${duration || '-'}, status=${status || '-'})`;
    }

    const parts: string[] = [];
    if (component) parts.push(`[${component}]`);
    if (caller) parts.push(`caller=${caller}`);

    return parts.length > 0 ? parts.join(' ') : '(unstructured log entry)';
  }

  private extractValue(text: string, key: string): string | null {
    const match = text.match(new RegExp(`${key}=([^\\s]+)`));
    return match ? match[1] : null;
  }

  private extractQuotedValue(text: string, key: string): string | null {
    const match = text.match(new RegExp(`${key}="([^"]+)"`));
    return match ? match[1] : null;
  }

  private toDateTimeLocal(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    const yyyy = date.getFullYear();
    const MM = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const mm = pad(date.getMinutes());
    return `${yyyy}-${MM}-${dd}T${hh}:${mm}`;
  }

  private toNanoSeconds(date: Date): number {
    return date.getTime() * 1_000_000; // ms → ns
  }
}
