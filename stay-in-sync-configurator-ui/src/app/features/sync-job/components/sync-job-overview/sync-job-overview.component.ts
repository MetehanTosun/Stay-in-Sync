import { Component } from '@angular/core';
import {NgForOf} from '@angular/common';
import {SyncJob} from '../../models/sync-job.model';

@Component({
  selector: 'app-sync-job-overview',
  imports: [
    NgForOf
  ],
  templateUrl: './sync-job-overview.component.html',
  styleUrl: './sync-job-overview.component.css'
})
export class SyncJobOverviewComponent {
  syncJob: SyncJob = {};

}
