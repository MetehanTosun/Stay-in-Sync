import { Component, OnDestroy, OnInit } from '@angular/core';
import { LogEntry } from '../../core/models/log.model';
import { LogService } from '../../core/services/log.service';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgClass, NgIf } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ActivatedRoute } from '@angular/router';
import { Button } from 'primeng/button';
import { TransformationService } from '../../core/services/transformation.service';
import {Select} from 'primeng/select';

@Component({
  selector: 'app-logs-panel',
  templateUrl: './logs-panel.component.html',
  imports: [
    FormsModule,
    DatePipe,
    NgClass,
    NgIf,
    TableModule,
    Button,
    Select
  ],
  standalone: true
})
export class LogsPanelComponent implements OnInit, OnDestroy {
  // Node ID passed via query params
  selectedNodeId?: string;

  // Log entries to display in the table
  logs: LogEntry[] = [];

  // Loading and error handling state
  loading = false;
  errorMessage = '';

  // Time filters (ISO datetime strings for input fields)
  startTime = '';
  endTime = '';

  // Log level filter
  level = '';

  // Transformation IDs related to the selected node
  transformationIds: string[] = [];

  // Dropdown options for log levels
  levels = [
    { label: 'Info', value: 'info' },
    { label: 'Warn', value: 'warn' },
    { label: 'Error', value: 'error' },
    { label: 'Debug', value: 'debug' },
    { label: 'Trace', value: 'trace' }
  ];

  // Dropdown options for services
  services = [
    { label: 'monitoring-backend', value: 'monitoring-backend' },
    { label: 'core-sync-node', value: 'core-sync-node' },
    { label: 'core-polling-node', value: 'core-polling-node' },
    { label: 'core-management', value: 'core-management' },
    { label: 'docker', value: 'docker' }
  ];

  private readonly intervalId?: number;

  // Currently selected transformation ID (for filtering logs)
  selectedTransformationId: string = '';

  // Currently selected service (for filtering logs)
  selectedService: string = '';

  constructor(
    private readonly logService: LogService,
    private readonly route: ActivatedRoute,
    private readonly transformationService: TransformationService
  ) {}

  /**
   * Initializes default time range (last hour) and subscribes to query params.
   * Automatically fetches logs when node ID changes.
   */
  ngOnInit() {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
    this.startTime = this.toDateTimeLocal(oneHourAgo);
    this.endTime = this.toDateTimeLocal(now);

    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];
      this.fetchLogs();
    });
  }

  /**
   * Clears any polling interval if set.
   */
  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  /**
   * Reloads logs with updated end time.
   */
  reloadLogs() {
    this.endTime = this.toDateTimeLocal(new Date());
    this.fetchLogs();
  }

  /**
   * Fetches logs depending on the context:
   * - Polling node → logs by service
   * - No node selected → logs by service or all logs
   * - Transformation node → logs by transformation IDs
   */
  fetchLogs() {
    const startNs = this.toNanoSeconds(new Date(this.startTime));
    const endNs = this.toNanoSeconds(new Date(this.endTime));
    const effectiveLevel = this.level || '';

    // Case 1: Polling node
    if (this.selectedNodeId && this.selectedNodeId.startsWith('POLL')) {
      this.selectedService = 'core-polling-node';
      this.loading = true;
      this.errorMessage = '';

      this.logService.getLogsByService(this.selectedService, startNs, endNs, effectiveLevel).subscribe({
        next: logs => {
          this.logs = logs;
          this.loading = false;
        },
        error: err => {
          console.error('Error fetching logs', err);
          this.errorMessage = 'Error loading logs';
          this.loading = false;
        }
      });
      return;
    }

    // Case 2: No node selected
    if (!this.selectedNodeId) {
      this.loading = true;
      this.errorMessage = '';

      if (this.selectedService && this.selectedService !== '') {
        // Logs filtered by service
        this.logService.getLogsByService(this.selectedService, startNs, endNs, effectiveLevel)
          .subscribe({
            next: logs => {
              this.logs = logs;
              this.loading = false;
            },
            error: err => {
              console.error('Error fetching logs', err);
              this.errorMessage = 'Error loading logs';
              this.loading = false;
            }
          });
      } else {
        // Fallback: all logs
        this.logService.getLogs(startNs, endNs, this.level)
          .subscribe({
            next: logs => {
              this.logs = logs;
              this.loading = false;
            },
            error: err => {
              console.error('Error fetching logs', err);
              this.errorMessage = 'Error loading logs';
              this.loading = false;
            }
          });
      }
      return;
    }

    // Case 3: Transformation node
    this.loading = true;
    this.errorMessage = '';

    // Step 1: Fetch transformation IDs for the node
    this.transformationService.getTransformations(this.selectedNodeId).subscribe({
      next: transformations => {
        if (!transformations || transformations.length === 0) {
          this.logs = [];
          this.loading = false;
          return;
        }

        this.transformationIds = transformations
          .map(t => t.id)
          .filter((id): id is number => id !== undefined)
          .map(id => id.toString());

        // Step 2: Fetch logs for transformations
        this.logService.getLogsByTransformations(this.transformationIds, startNs, endNs, effectiveLevel).subscribe({
          next: logs => {
            this.logs = logs;
            this.loading = false;

            // If a specific transformation is selected, filter results
            if (this.selectedTransformationId !== '') {
              this.logs = this.logs.filter(log => log.transformationId?.toString() === this.selectedTransformationId);
            }
          },
          error: err => {
            console.error('Error fetching logs', err);
            this.errorMessage = 'Error loading logs';
            this.loading = false;
          }
        });
      },
      error: err => {
        console.error('Error fetching transformations', err);
        this.errorMessage = 'Error loading transformations';
        this.loading = false;
      }
    });
  }

  /**
   * Triggers a log reload when filters are changed.
   */
  onFilterChange() {
    this.fetchLogs();
  }

  /**
   * Handles changes to the selected service.
   *
   * If the currently selected node ID starts with 'POLL', it resets the node ID
   * and the selected transformation ID to ensure proper filtering behavior.
   * After making these adjustments, it triggers a log fetch to update the displayed logs.
   */
  onServiceChange() {
    if (this.selectedNodeId?.startsWith('POLL')) {
      // Reset the selected node ID and transformation ID
      this.selectedNodeId = undefined;
      this.selectedTransformationId = '';
    }
    // Fetch logs with the updated filters
    this.fetchLogs();
  }



  /**
   * Builds a fallback message for unstructured log entries
   * by extracting common fields (component, query, etc.).
   */
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

  /**
   * Extracts a value of the form key=value from a raw log string.
   */
  private extractValue(text: string, key: string): string | null {
    const match = new RegExp(`${key}=(S+)`).exec(text);
    return match ? match[1] : null;
  }

  /**
   * Extracts a quoted value of the form key="..." from a raw log string.
   */
  private extractQuotedValue(text: string, key: string): string | null {
    const match = new RegExp(`${key}="([^"]+)"`).exec(text);
    return match ? match[1] : null;
  }

  /**
   * Converts a Date to a datetime-local string for form inputs.
   */
  private toDateTimeLocal(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    const yyyy = date.getFullYear();
    const MM = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const mm = pad(date.getMinutes());
    return `${yyyy}-${MM}-${dd}T${hh}:${mm}`;
  }

  /**
   * Converts a Date to nanoseconds (for backend queries).
   */
  private toNanoSeconds(date: Date): number {
    return date.getTime() * 1_000_000; // ms → ns
  }
}
