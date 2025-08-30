import { Component, OnDestroy, OnInit } from '@angular/core';
import { LogEntry } from '../../core/models/log.model';
import { LogService } from '../../core/services/log.service';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgClass, NgIf } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ActivatedRoute } from '@angular/router';
import { NodeMarkerService } from '../../core/services/node-marker.service';
import { Button } from 'primeng/button';
import { TransformationService } from '../../core/services/transformation.service'; // <-- neu

@Component({
  selector: 'app-logs-panel',
  templateUrl: './logs-panel.component.html',
  imports: [
    FormsModule,
    DatePipe,
    NgClass,
    NgIf,
    TableModule,
    Button
  ],
  standalone: true
})
export class LogsPanelComponent implements OnInit, OnDestroy {
  selectedNodeId?: string;

  logs: LogEntry[] = [];
  loading = false;
  errorMessage = '';

  startTime = '';
  endTime = '';
  level = '';

  constructor(
    private logService: LogService,
    private route: ActivatedRoute,
    private nodeMarkerService: NodeMarkerService,
    private transformationService: TransformationService // <-- neu
  ) {}

  private intervalId?: number;

  ngOnInit() {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
    this.startTime = this.toDateTimeLocal(oneHourAgo);
    this.endTime = this.toDateTimeLocal(now);

    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];
      this.fetchLogs();
    });

    this.intervalId = window.setInterval(() => {
      this.checkForErrorLogs();
    }, 5000);
  }

  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  reloadLogs() {
    this.endTime = this.toDateTimeLocal(new Date());
    this.fetchLogs();
  }

  fetchLogs() {
    const startNs = this.toNanoSeconds(new Date(this.startTime));
    const endNs = this.toNanoSeconds(new Date(this.endTime));

    if (!this.selectedNodeId){
      this.logService.getLogs(startNs, endNs, this.level)
        .subscribe({
          next: logs => {
            this.logs = logs;
            this.loading = false;
          },
          error: err => {
            console.error('Error fetching logs', err);
            this.errorMessage = 'Fehler beim Laden der Logs';
            this.loading = false;
          }
        });
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    // 1. Alle TransformationIds für den SyncJob holen
    this.transformationService.getTransformations(this.selectedNodeId).subscribe({
      next: transformations => {
        if (!transformations || transformations.length === 0) {
          this.logs = [];
          this.loading = false;
          return;
        }
        const transformationIds = transformations
          .map(t => t.id)
          .filter((id): id is number => id !== undefined)
          .map(id => id.toString());

        // 2. Logs für alle TransformationIds abrufen
        this.logService.getLogsByTransformations(transformationIds, startNs, endNs, this.level).subscribe({
          next: logs => {
            this.logs = logs;
            this.loading = false;
          },
          error: err => {
            console.error('Error fetching logs', err);
            this.errorMessage = 'Fehler beim Laden der Logs';
            this.loading = false;
          }
        });
      },
      error: err => {
        console.error('Error fetching transformationIds', err);
        this.errorMessage = 'Fehler beim Laden der Transformationen';
        this.loading = false;
      }
    });
  }

  onFilterChange() {
    this.fetchLogs();
  }

  checkForErrorLogs() {
    this.logService.getErrorLogs(this.toNanoSeconds(new Date(this.startTime)), this.toNanoSeconds(new Date())).subscribe({
      next: errorIds => {
        const markedNodes: { [nodeId: string]: boolean } = {};
        errorIds.forEach(id => markedNodes[id] = true);
        this.nodeMarkerService.updateMarkedNodes(markedNodes);
      },
      error: err => console.error('Error fetching error sync job IDs', err)
    });
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
