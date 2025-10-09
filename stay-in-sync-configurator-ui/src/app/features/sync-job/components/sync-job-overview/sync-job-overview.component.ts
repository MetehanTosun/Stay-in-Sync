import {Component, Input} from '@angular/core';
import {NgForOf, NgIf} from '@angular/common';
import {SyncJob} from '../../models/sync-job.model';
import {Card} from 'primeng/card';
import {Transformation} from '../../../transformation/models/transformation.model';


@Component({
  selector: 'app-sync-job-overview',
  imports: [
    NgForOf,
    Card,
    NgIf
  ],
  templateUrl: './sync-job-overview.component.html',
  styleUrl: './sync-job-overview.component.css'
})
/**
 * Component for displaying an overview of a sync job.
 * It manages the sync job data and its associated transformations.
 */
export class SyncJobOverviewComponent {
  /**
   * The private field to store the sync job data.
   * @private
   */
  private _syncJob!: SyncJob;

  /**
   * Array of transformations associated with the sync job.
   */
  transformations: Transformation[] = [];

  /**
   * Setter for the sync job input.
   * Updates the private `_syncJob` field and extracts transformations from the sync job.
   *
   * @param value - The sync job to set.
   */
  @Input() set syncJob(value: SyncJob) {
    this._syncJob = value;
    this.transformations = value?.transformations ?? [];
  }

  /**
   * Getter for the sync job.
   *
   * @returns The current sync job.
   */
  get syncJob(): SyncJob {
    return this._syncJob;
  }
}

