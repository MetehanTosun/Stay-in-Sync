import {Component, OnInit} from '@angular/core';
import {SyncJobService} from '../../services/sync-job.service';
import {HttpErrorService} from '../../../../core/services/http-error.service';
import {NavigationEnd, Router} from '@angular/router';
import {Button} from 'primeng/button';
import {SyncJobCreationComponent} from '../sync-job-creation/sync-job-creation.component';
import {TableModule} from 'primeng/table';
import {SyncJob} from '../../models/sync-job.model';
import {Select} from 'primeng/select';
import {Tag} from 'primeng/tag';
import {FormsModule} from '@angular/forms';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-sync-job-page',
  imports: [
    Button,
    SyncJobCreationComponent,
    TableModule,
    Select,
    Tag,
    FormsModule,
    NgIf
  ],
  templateUrl: './sync-job-page.component.html',
  standalone: true,
  styleUrl: './sync-job-page.component.css'
})
export class SyncJobPageComponent implements OnInit{
  showCreateDialog: boolean = false;
  public selectedStatus: any = null; // Ensure this is mutable
  items: SyncJob[] = [];
  loading: boolean = false;

statuses = [
  { label: 'Active', value: false }, // false für "active"
  { label: 'In Simulation', value: true } // true für "In Simulation"
];
  selectedSyncJobId: number | undefined = undefined;

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
 ) {}

ngOnInit() {
    this.getAll();
  // Überwache Navigationen und rufe getAll() bei Rückkehr auf
  this.router.events.subscribe(event => {
    if (event instanceof NavigationEnd && event.url === '/sync-jobs') {
      this.getAll();
      this.selectedSyncJobId = undefined;
    }
  });
  }


  getAll() {
    console.log("GET ALL")
    this.syncJobService.getAll().subscribe({
      next: data => {
        console.log(data)
        this.items = data;
      },
      error: err => {
        console.log(err)
        this.httpErrorService.handleError(err)
      }
    })
  }

  edit(item: SyncJob) {
    //TODO: Implement edit functionality
    this.selectedSyncJobId = item.id;
    console.log('Edit Sync Job:', item);
    this.showCreateDialog = true;
    // SyncJobCreationComponent mit der übergebenen SyncJob-ID öffnen
    // Annahme: SyncJobCreationComponent akzeptiert eine Input-Property 'syncJobId'
    // Beispiel: <app-sync-job-creation [syncJobId]="item.id"></app-sync-job-creation>
    // Hier müsste ggf. eine Property gesetzt werden, die an das Dialog-Template gebunden ist:
    this.router.navigate(['sync-jobs/edit', item.id]);
  }

  delete(item: SyncJob) {
    this.syncJobService.delete(item).subscribe({
      next: () => {
        this.getAll();
      },
      error: err => {
        console.error(err);
        this.httpErrorService.handleError(err);
      }
    });
  }
}
