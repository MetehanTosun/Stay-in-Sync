import {Component, OnInit, OnDestroy} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {DatePipe, NgClass, NgForOf, NgIf, NgStyle} from '@angular/common';
import {SnapshotModel} from '../../core/models/snapshot.model';
import {TransformationService} from '../../core/services/transformation.service';
import {Panel} from 'primeng/panel';
import {Message} from 'primeng/message';
import {PrimeTemplate} from 'primeng/api';
import {Subscription} from 'rxjs';
import {TransformationModelForSnapshotPanel} from '../../core/models/transformation.model';
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {SnapshotService} from '../../core/services/snapshot.service';

@Component({
  selector: 'app-error-snapshot-panel',
  templateUrl: './error-snapshot-panel.component.html',
  imports: [
    NgIf,
    NgForOf,
    Panel,
    Message,
    NgClass,
    PrimeTemplate,
    NgStyle,
    TableModule,
    Button,
    DatePipe
  ],
  styleUrls: ['./error-snapshot-panel.component.css']
})
export class ErrorSnapshotPanelComponent implements OnInit, OnDestroy {
  selectedNodeId?: string;
  transformations: TransformationModelForSnapshotPanel[] = [];
  transformationSnapshots = new Map<number, SnapshotModel[]>();

  private routeSub?: Subscription;
  private sseSub?: Subscription;
  private sse?: EventSource;

  constructor(
    private route: ActivatedRoute,
    private transformationService: TransformationService,
    private router: Router,
    private snapshotService: SnapshotService,
  ) {}

  ngOnInit() {
    this.routeSub = this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];
      if (this.selectedNodeId) {
        this.getTransformations();
        this.subscribeToSse();
      }
    });
  }

  ngOnDestroy() {
    this.routeSub?.unsubscribe();
    if (this.sse) {
      this.sse.close();
    }
  }

  getTransformations() {
    if (this.selectedNodeId) {
      this.transformationService
        .getTransformations(this.selectedNodeId)
        .subscribe(data => {
          this.transformations = data;
          this.loadSnapshotsForTransformations();
        });
    }
  }

  loadSnapshotsForTransformations() {
    this.transformationSnapshots.clear();
    this.transformations.forEach(transformation => {
      if (typeof transformation.id === 'number') {
        this.snapshotService
          .getLastFiveSnapshots(transformation.id.toString())
          .subscribe(snapshots => {
            this.transformationSnapshots.set(transformation.id!, snapshots);
          });
      }
    });
  }

  getSnapshotsForTransformation(transformationId: number | undefined): SnapshotModel[] {
    return this.transformationSnapshots.get(<number>transformationId) ?? [];
  }

  replaySnapshot(snapshotId: number) {
    this.router.navigate(['/replay'], { queryParams: { snapshotId } });
  }

  private subscribeToSse() {
    // SSE-Endpunkt vom Backend
    this.sse = new EventSource('/events/subscribe');

    // Transformation-Update Events
    this.sse.addEventListener('transformation-update', (event: any) => {
      const data = JSON.parse(event.data) as number[]; // Array von TransformationIds
      this.transformations.forEach(t => {
        if (data.includes(t.id!)) {
          t.error = true; // Error-Flag setzen
        }
      });
    });

    // Optional: Job-Update Events, falls du auch JobIds nutzen willst
    this.sse.addEventListener('job-update', (event: any) => {
      const jobIds = JSON.parse(event.data) as number[];
      console.log('Job-Updates:', jobIds);
    });

    this.sse.onerror = (err) => {
      console.error('SSE-Fehler', err);
      this.sse?.close();
    };
  }
}
