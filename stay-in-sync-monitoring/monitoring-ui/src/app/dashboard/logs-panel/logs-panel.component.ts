import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { LogEntry } from '../../../log.model';
import { LogService } from '../log.service';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-logs-panel',
  templateUrl: './logs-panel.component.html',
  styleUrl: './logs-panel.component.css',
  imports: [
    NgIf,
    NgForOf
  ]
})
export class LogsPanelComponent implements OnInit, OnChanges {
  @Input() selectedNodeId: string | null = null;
  logs: LogEntry[] = [];
  filteredLogs: LogEntry[] = [];

  constructor(private logService: LogService) {}

  ngOnInit() {
    this.logService.getLogs().subscribe((logs: LogEntry[]) => {
      this.logs = logs;
      this.filterLogs();
    });
  }

  ngOnChanges() {
    this.filterLogs();
  }

  filterLogs() {
    this.filteredLogs = this.selectedNodeId
      ? this.logs.filter((l) => l.nodeId === this.selectedNodeId)
      : this.logs;
  }
}
