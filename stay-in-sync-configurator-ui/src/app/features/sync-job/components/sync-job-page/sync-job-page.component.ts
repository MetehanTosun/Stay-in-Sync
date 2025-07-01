import { Component } from '@angular/core';
import {SyncJobService} from '../../services/sync-job.service';
import {HttpErrorService} from '../../../../core/services/http-error.service';

@Component({
  selector: 'app-sync-job-page',
  imports: [],
  templateUrl: './sync-job-page.component.html',
  standalone: true,
  styleUrl: './sync-job-page.component.css'
})
export class SyncJobPageComponent {


  constructor(readonly syncJobService: SyncJobService, readonly httpErrorService: HttpErrorService) {
    this.getAll()
  }


  getAll() {
    console.log("GET ALL")
    this.syncJobService.getAll().subscribe({
      next: data => {
        console.log("data")
      },
      error: err => {
        console.log(err)
        this.httpErrorService.handleError(err)
      }
    })
  }
}
