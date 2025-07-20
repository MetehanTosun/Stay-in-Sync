import {Component, Input, OnInit} from '@angular/core';
import {NgForOf, NgIf} from '@angular/common';
import {SyncJob} from '../../models/sync-job.model';
import {Card} from 'primeng/card';
import {Tag} from 'primeng/tag';
import {Transformation} from '../../../transformation/models/transformation.model';
import {TransformationService} from '../../../transformation/services/transformation.service';
import {SourceSystemApiRequestConfiguration} from '../../../script-editor/models/arc.models';

@Component({
  selector: 'app-sync-job-overview',
  imports: [
    NgForOf,
    Card,
    Tag,
    NgIf
  ],
  templateUrl: './sync-job-overview.component.html',
  styleUrl: './sync-job-overview.component.css'
})
export class SyncJobOverviewComponent implements OnInit {
private _syncJob!: SyncJob;
sourceSystemApiRequestConfigurations: SourceSystemApiRequestConfiguration[] = [];
transformations: Transformation[] = [];

@Input() set syncJob(value: SyncJob) {
  this._syncJob = value;
  this.transformations = value?.transformations ?? [];
  this.updateSourceSystemApiRequestConfigurations();

}
get syncJob(): SyncJob {
  return this._syncJob;
}

constructor(private transformationService: TransformationService) {
}

  ngOnInit() {
    console.log(this.syncJob.sourceSystems)
  }

 updateSourceSystemApiRequestConfigurations(): void {
   this.sourceSystemApiRequestConfigurations = this.transformations
     .flatMap(transformation => transformation.sourceSystemApiRequestConfigurations || [])
     .reduce<SourceSystemApiRequestConfiguration[]>((acc, config) => {
       if (!acc.some(existingConfig => existingConfig.id === config.id)) {
         acc.push(config);
       }
       return acc;
     }, []);
 }

}
