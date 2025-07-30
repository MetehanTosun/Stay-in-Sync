import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { LogEntry } from '../../core/models/log.model';
import { LogService } from '../../core/services/log.service';
import { FormsModule } from '@angular/forms';
import { NgForOf, NgIf } from '@angular/common';

@Component({
  selector: 'app-logs-panel',
  templateUrl: './logs-panel.component.html',
  imports: [
    FormsModule,
    NgIf,
    NgForOf
  ],
  styleUrls: ['./logs-panel.component.css']
})
export class LogsPanelComponent implements OnInit, OnChanges {
  @Input() selectedNodeId: string | null = null;
  logs: LogEntry[] = [];
  filteredLogs: LogEntry[] = [];
  startTime: string = '';
  endTime: string = '';
  level: string = '';

  constructor(private logService: LogService) {}

  ngOnInit() {
    this.fetchLogs();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (
      changes['selectedNodeId'] ||
      changes['startTime'] ||
      changes['endTime'] ||
      changes['level']
    ) {
      this.fetchLogs();
    }
  }

  fetchLogs() {
    console.log(`Fetching logs for Node: ${this.selectedNodeId}, Start: ${this.startTime}, End: ${this.endTime}, Level: ${this.level}`);
    if (this.selectedNodeId && this.startTime && this.endTime && this.level) {
      this.logService.getFilteredLogs(this.selectedNodeId, this.startTime, this.endTime, this.level)
        .subscribe((logs: LogEntry[]) => {
          this.logs = logs;
          this.filteredLogs = logs;
        });
    }
  }
}
