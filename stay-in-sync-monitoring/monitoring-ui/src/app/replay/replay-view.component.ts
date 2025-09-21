// src/app/replay/replay-view.component.ts
import { CommonModule, JsonPipe, NgIf } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SnapshotDTO } from './models/snapshot.model';
import { TransformationScriptDTO } from './models/transformation-script.model';
import { ScriptService } from './script.service';
import { SnapshotService } from './snapshot.service';

@Component({
  selector: 'app-replay-view',
  standalone: true,
  imports: [CommonModule, NgIf, JsonPipe],
  templateUrl: './replay-view.component.html',
  styleUrl: './replay-view.component.css',
})
export class ReplayViewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private snapshots = inject(SnapshotService);
  private scripts = inject(ScriptService);

  // tabs: 'source' | 'vars' | 'term'
  activeTab: 'source' | 'vars' | 'term' = 'source';

  // UI state
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  data = signal<SnapshotDTO | null>(null);

  scriptDisplay = '// loading TypeScript…';

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
      },
      error: (e) => {
        this.error.set(`Failed to load snapshot: ${e?.message ?? e}`);
        this.loading.set(false);
      },
    });
  }

  setTab(tab: 'source' | 'vars' | 'term') {
    this.activeTab = tab;
  }

  onReplayClick() {
    // stub for later
  }
}
