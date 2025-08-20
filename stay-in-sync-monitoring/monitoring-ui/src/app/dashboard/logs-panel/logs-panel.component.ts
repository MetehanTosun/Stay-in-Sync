import { Component, Input, OnChanges, SimpleChanges, OnInit } from '@angular/core';
import { LogEntry } from '../../core/models/log.model';
import { LogService } from '../../core/services/log.service';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgClass, NgForOf, NgIf } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-logs-panel',
  templateUrl: './logs-panel.component.html',
  imports: [
    FormsModule,
    DatePipe,
    NgClass,
    TableModule
  ]
})
export class LogsPanelComponent implements OnInit, OnChanges {
  selectedNodeId: string | null = null;

  logs: LogEntry[] = [];
  filteredLogs: LogEntry[] = [];

  startTime: string = '';
  endTime: string = '';
  level: string = 'info';
  stream: string = 'fluent-bit';

  constructor(private logService: LogService, private route: ActivatedRoute) {
    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];
      console.log('Selected Node ID from query params:', this.selectedNodeId);
    });
  }

  ngOnInit() {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
    this.startTime = this.toDateTimeLocal(oneHourAgo);
    this.endTime = this.toDateTimeLocal(now);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['selectedNodeId'] && this.selectedNodeId) {
      this.fetchLogs();
    }
  }

  fetchLogs() {
    this.logService.getLogs(this.selectedNodeId!, this.startTime, this.endTime, this.level).subscribe({
      next: (logs) => this.filteredLogs = logs,
      error: (err) => console.error('Error fetching logs', err)
    });
  }


  onFilterChange() {
    this.fetchLogs();
  }

  buildFallbackMessage(log: LogEntry): string {
    const raw = log.rawMessage || ''; // full log line if message is missing

    // Try to extract meaningful Loki fields
    const component = this.extractValue(raw, 'component');
    const query = this.extractQuotedValue(raw, 'query');
    const returned = this.extractValue(raw, 'returned_lines');
    const duration = this.extractValue(raw, 'duration');
    const status = this.extractValue(raw, 'status');
    const caller = this.extractValue(raw, 'caller');

    if (component && query) {
      return `[${component}] Query ${query} returned ${returned || 0} lines (duration=${duration || '-'}, status=${status || '-'})`;
    }

    // Fallback: return caller or component only
    const parts = [];

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
}
