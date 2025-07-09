import {Component, Input, OnInit} from '@angular/core';
import {NgForOf} from '@angular/common';
import {SyncJob} from '../../models/sync-job.model';
import {Transformation} from '../../../transformation/models/transformation.model';
import {Card} from 'primeng/card';
import {Tag} from 'primeng/tag';

@Component({
  selector: 'app-sync-job-overview',
  imports: [
    NgForOf,
    Card,
    Tag
  ],
  templateUrl: './sync-job-overview.component.html',
  styleUrl: './sync-job-overview.component.css'
})
export class SyncJobOverviewComponent implements OnInit {
  @Input() syncJob!: SyncJob;


  ngOnInit() {
    console.log(this.syncJob.sourceSystems)
  }

}
