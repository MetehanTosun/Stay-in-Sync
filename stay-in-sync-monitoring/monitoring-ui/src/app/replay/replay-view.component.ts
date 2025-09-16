// src/app/replay/replay-view.component.ts
import { CommonModule, JsonPipe, NgIf } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SnapshotDTO } from './models/snapshot.model';
import { TransformationScriptDTO } from './models/transformation-script.model';
import { ScriptService } from './script.service';
import { SnapshotService } from './snapshot.service';
import {LogService} from '../core/services/log.service';
import {LogEntry} from '../core/models/log.model';
import {PrimeTemplate} from 'primeng/api';
import {TableModule} from 'primeng/table';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';

@Component({
  selector: 'app-replay-view',
  standalone: true,
  imports: [CommonModule, NgIf, JsonPipe, PrimeTemplate, TableModule, Tabs, TabList, Tab, TabPanels, TabPanel],
  templateUrl: './replay-view.component.html',
  styleUrl: './replay-view.component.css',
})
export class ReplayViewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private snapshots = inject(SnapshotService);
  private scripts = inject(ScriptService);
  private logService = inject(LogService);



  // UI state
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  data = signal<SnapshotDTO | null>(null);

  scriptDisplay = '// loading TypeScript…';
  logs: LogEntry[] = [];

  ngOnInit(): void {
    const id = this.route.snapshot.queryParamMap.get('snapshotId');
    if (!id) {
      this.error.set('Missing snapshotId in URL.');
      this.loading.set(false);
      return;
    }

    this.snapshots.getById(id).subscribe({
      next: (snap) => {
        this.data.set(snap);

        // Fill right-side "SourceData"
        // (template already uses data()?.transformationResult?.sourceData | json)

        const transformationId = snap.transformationResult?.transformationId;
        if (transformationId == null) {
          this.scriptDisplay =
            '// Snapshot has no transformationId. Cannot load script.';
          this.loading.set(false);
          return;
        }

        // fetch the **TypeScript** code by transformationId
        this.scripts.getByTransformationId(transformationId).subscribe({
          next: (script: TransformationScriptDTO) => {
            this.scriptDisplay =
              script.typescriptCode || '// No TypeScript code available';
            this.loading.set(false);
          },
          error: (err) => {
            console.error('Failed to load script', err);
            this.scriptDisplay = '// Failed to load TypeScript code';
            this.loading.set(false);
          },
        });

        this.logService.getLogsByTransformations(
          [transformationId.toString()],
          this.toNanoSeconds(new Date(Date.now() - 24 * 60 * 60 * 1000)), // vor 24 Stunden
          this.toNanoSeconds(new Date()), // jetzt
          ""
        ).subscribe({
          next: (logs) => {
            this.logs = logs;
            console.log('Logs for transformation', transformationId, logs);
          },
          error: (err) => {
            console.error('Failed to load logs for transformation', transformationId, err);
          }
        });
      },
      error: (e) => {
        this.error.set(`Failed to load snapshot: ${e?.message ?? e}`);
        this.loading.set(false);
      },
    });
  }


  onReplayClick() {
    // stub for later
  }

  private toNanoSeconds(date: Date) {
    return date.getTime() * 1_000_000;
  }
}
