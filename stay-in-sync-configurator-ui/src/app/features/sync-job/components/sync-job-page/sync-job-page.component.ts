import { Component } from '@angular/core';
import {SyncJobService} from '../../services/sync-job.service';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {Router} from '@angular/router';
import {Button} from 'primeng/button';
import {SyncJobCreationComponent} from '../sync-job-creation/sync-job-creation.component';

@Component({
  selector: 'app-sync-job-page',
  imports: [
    Button,
    SyncJobCreationComponent
  ],
  templateUrl: './sync-job-page.component.html',
  standalone: true,
  styleUrl: './sync-job-page.component.css'
})
export class SyncJobPageComponent{
  showCreateDialog: boolean = false;


  openCreateDialog() {
    this.showCreateDialog = true;
    this.router.navigate(['sync-jobs/create']);
  }


 constructor(
   readonly syncJobService: SyncJobService,
   readonly httpErrorService: HttpErrorService,
   private router: Router,
 ) {
   this.getAll();
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
