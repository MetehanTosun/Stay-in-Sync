import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe, NgClass, NgForOf, NgIf, NgStyle } from '@angular/common';
import { TransformationService } from '../../core/services/transformation.service';
import { Panel } from 'primeng/panel';
import { Message } from 'primeng/message';
import { PrimeTemplate } from 'primeng/api';
import { Subscription } from 'rxjs';
import { TransformationModelForSnapshotPanel } from '../../core/models/transformation.model';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';
import { SnapshotService } from '../../core/services/snapshot.service';
import {SnapshotDTO} from '../../core/models/snapshot.model';

/**
 * ErrorSnapshotPanelComponent
 *
 * This Angular component displays snapshots of transformations
 * for a selected node that encountered errors. It:
 * - Loads transformations for a given node.
 * - Fetches the last 5 snapshots for each transformation.
 * - Subscribes to SSE (Server-Sent Events) to react to live transformation/job updates.
 * - Allows replaying a specific snapshot via navigation.
 */
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
    DatePipe,
  ],
  styleUrls: ['./error-snapshot-panel.component.css'],
})
export class ErrorSnapshotPanelComponent implements OnInit, OnDestroy {
  /**
   * ID of the currently selected node (from query params).
   */
  selectedNodeId?: string;

  /**
   * List of transformations associated with the selected node.
   */
  transformations: TransformationModelForSnapshotPanel[] = [];

  /**
   * A map of transformation IDs to their last 5 snapshots.
   */
  transformationSnapshots = new Map<number, SnapshotDTO[]>();

  private routeSub?: Subscription;
  private sse?: EventSource;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly transformationService: TransformationService,
    private readonly router: Router,
    private readonly snapshotService: SnapshotService
  ) {}

  /**
   * Lifecycle hook: called after component initialization.
   * - Subscribes to query params to detect selected node changes.
   * - If a node is selected, fetches transformations and subscribes to SSE.
   */
  ngOnInit() {
    this.routeSub = this.route.queryParams.subscribe((params) => {
      this.selectedNodeId = params['input'];
      if (this.selectedNodeId) {
        this.getTransformations();
        this.subscribeToSse();
      }
    });
  }

  /**
   * Lifecycle hook: called before component is destroyed.
   * - Unsubscribes from route changes.
   * - Closes the SSE connection if active.
   */
  ngOnDestroy() {
    this.routeSub?.unsubscribe();
    if (this.sse) {
      this.sse.close();
    }
  }

  /**
   * Fetches transformations for the currently selected node
   * and then loads snapshots for them.
   */
  getTransformations() {
    if (this.selectedNodeId && !(this.selectedNodeId.startsWith("POLL"))) {
      this.transformationService
        .getTransformations(this.selectedNodeId)
        .subscribe((data) => {
          this.transformations = data;
          this.loadSnapshotsForTransformations();
        });
    }
  }

  /**
   * Loads the last five snapshots for each transformation
   * and stores them in the transformationSnapshots map.
   */
  loadSnapshotsForTransformations() {
    this.transformationSnapshots.clear();
    for (const transformation of this.transformations) {
      if (typeof transformation.id === 'number') {
        this.snapshotService
          .getLastFiveSnapshots(transformation.id.toString())
          .subscribe((snapshots) => {
            this.transformationSnapshots.set(transformation.id!, snapshots);
          });
      }
    }
  }

  /**
   * Retrieves snapshots for a given transformation ID.
   *
   * @param transformationId The transformation ID.
   * @returns The last 5 snapshots or an empty array.
   */
  getSnapshotsForTransformation(
    transformationId: number | undefined
  ): SnapshotDTO[] {
    return this.transformationSnapshots.get(<number>transformationId) ?? [];
  }

  /**
   * Navigates to the replay view for a given snapshot.
   *
   * @param snapshotId ID of the snapshot to replay.
   */
  replaySnapshot(snapshotId: number) {
    this.router.navigate(['/replay'], { queryParams: { snapshotId } });
  }

  /**
   * Subscribes to the backend SSE endpoint to receive real-time updates:
   * - `transformation-update`: Marks transformations as having an error.
   * - `job-update`: Logs job updates (currently informational only).
   *
   * Automatically closes on error.
   */
  private subscribeToSse() {
    this.sse = new EventSource('/events/subscribe');

    // Transformation update events
    this.sse.addEventListener('transformation-update', (event: any) => {
      const data = JSON.parse(event.data) as number[]; // Array of transformation IDs
      for (const t of this.transformations) {
        if (data.includes(t.id!)) {
          t.error = true; // Mark transformation as error
        }
      }
    });

    // Job update events (optional usage)
    this.sse.addEventListener('job-update', (event: any) => {
      const jobIds = JSON.parse(event.data) as number[];
      console.log('Job updates received:', jobIds);
    });

    this.sse.onerror = (err) => {
      console.error('SSE error:', err);
      this.sse?.close();
    };
  }
}
