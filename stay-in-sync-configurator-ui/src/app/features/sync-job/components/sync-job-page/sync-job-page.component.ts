import { Component } from '@angular/core';
import {SyncJobService} from '../../services/sync-job.service';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {Router} from '@angular/router';
import {Button} from 'primeng/button';
import {SyncJobCreationComponent} from '../sync-job-creation/sync-job-creation.component';
import {TableModule} from 'primeng/table';
import {SyncJob} from '../../models/sync-job.model';
import {Select} from 'primeng/select';
import {Tag} from 'primeng/tag';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-sync-job-page',
  imports: [
    Button,
    SyncJobCreationComponent,
    TableModule,
    Select,
    Tag,
    FormsModule
  ],
  templateUrl: './sync-job-page.component.html',
  standalone: true,
  styleUrl: './sync-job-page.component.css'
})
export class SyncJobPageComponent{
  showCreateDialog: boolean = false;
  public selectedStatus: any = null; // Ensure this is mutable
  items: SyncJob[] = [
    {
      name: 'Beispiel-Sync-Job',
      description: 'Dies ist ein Beispiel-Sync-Job.',
      isSimulation: false,
    },
    {
      name: 'Test-Sync-Job',
      description: 'Dies ist ein Test-Sync-Job. Er läuft in Simulation. Es werden keine Daten ins Zielsystem geschrieben',
      isSimulation: true,
    }
  ];
  loading: boolean = false;

statuses = [
  { label: 'Active', value: false }, // false für "active"
  { label: 'In Simulation', value: true } // true für "In Simulation"
];

getSeverity(isSimulation: boolean): string {
  return isSimulation ? 'warning' : 'success'; // Gelb für Simulation, Grün für Active
}


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

  edit(item: any) {
    //TODO: Implement edit functionality
  }

  delete(item: any) {
    //TODO: Implement delete functionality
  }
}
