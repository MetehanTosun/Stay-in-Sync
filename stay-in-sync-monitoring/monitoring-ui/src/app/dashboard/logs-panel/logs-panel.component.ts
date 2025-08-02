import { Component, Input, OnChanges, SimpleChanges, OnInit } from '@angular/core';
import { LogEntry } from '../../core/models/log.model';
import { LogService } from '../../core/services/log.service';
import { FormsModule } from '@angular/forms';
import {DatePipe, NgClass, NgForOf, NgIf} from '@angular/common';
import {TableModule} from 'primeng/table';
import {ActivatedRoute} from '@angular/router';

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
  stream: 'stdout' | 'stderr' = 'stderr';


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
    const startNs = new Date(this.startTime).getTime() * 1_000_000;
    const endNs = new Date(this.endTime).getTime() * 1_000_000;

    const nodeId = this.selectedNodeId || '';
    const level = this.level || '';

    this.logService.getLogs(
      this.stream,
      level,
      nodeId,
      startNs.toString(),
      endNs.toString()
    ).subscribe({
      next: (logs: any[]) => {
        this.logs = logs.map((entry: any) => {
          const rawMessage = entry.message;

          // Level extrahieren
          const levelMatch = rawMessage.match(/level=([a-zA-Z]+)/);
          const level = levelMatch ? levelMatch[1].toLowerCase() : '';

          // Eigentliche Log-Nachricht extrahieren
          const msgMatch = rawMessage.match(/msg="([^"]+)"/);
          const message = msgMatch ? msgMatch[1] : rawMessage;

          // Fallback für stream
          const stream = entry.stream || '';

          return {
            timestamp: entry.timestamp,
            message,
            stream,
            level,
          } as LogEntry;
        });

        this.filteredLogs = this.logs.sort(
          (a, b) => Number(new Date(b.timestamp)) - Number(new Date(a.timestamp))
        );
        console.log('Logs erfolgreich abgerufen:', this.filteredLogs);
      },
      error: (err) => {
        console.error('Fehler beim Abrufen der Logs', err);
      }
    });
  }




  onFilterChange() {
    this.fetchLogs();
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
