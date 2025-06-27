import {Component, Input} from '@angular/core';
import {NgForOf} from '@angular/common';
import {SyncJob} from '../../models/sync-job.model';
import {Transformation} from '../../../transformation/models/transformation.model';

@Component({
  selector: 'app-sync-job-overview',
  imports: [
    NgForOf
  ],
  templateUrl: './sync-job-overview.component.html',
  styleUrl: './sync-job-overview.component.css'
})
export class SyncJobOverviewComponent {
  @Input() syncJob!: SyncJob;
  transformation : Transformation = {};

}
