import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {TransformationService} from '../../../transformation/services/transformation.service';
import {SyncJobService} from '../../services/sync-job.service';
import {SyncJob} from '../../models/sync-job.model';
import {
  TransformationBySyncJobTableComponent
} from '../../../transformation/components/transformation-by-sync-job-table/transformation-by-sync-job-table.component';
import {Inplace} from 'primeng/inplace';
import {FormsModule} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Skeleton} from 'primeng/skeleton';

@Component({
  selector: 'app-sync-job-details-page',
  standalone: true,
  imports: [
    TransformationBySyncJobTableComponent,
    Inplace,
    InputTextModule,
    FormsModule,
    Button,
    Skeleton
  ],
  templateUrl: './sync-job-details-page.component.html',
  styleUrl: './sync-job-details-page.component.css'
})
export class SyncJobDetailsPageComponent implements OnInit {
  @ViewChild('inplace') inplace!: Inplace;


  id!: number;

  syncJob?: SyncJob;
  constructor(private readonly route: ActivatedRoute, private readonly transformationService: TransformationService, private readonly syncJobService: SyncJobService) {}

  ngOnInit(): void {
    const id = Number.parseInt(this.route.snapshot.paramMap.get('id')!);

    if (!id) {
      throw new Error('ID parameter is required');
    }

    this.id = id;
    this.loadSyncJobDetails(this.id);
  }

  private loadSyncJobDetails(id: number): void {
     this.syncJobService.getById(id).subscribe({
       next: data => {
         console.log(data);
         this.syncJob= data;
       },
       error: err => {
         console.log(err);
       }
     });
  }

  closeCallback() {

  }
}
