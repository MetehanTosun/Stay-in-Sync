/**
 * @file sync-job-page.component.ts
 * @description This component represents the Sync Job page in the application. It provides functionality
 * for displaying, creating, editing, deploying, and deleting Sync Jobs. It also handles navigation and
 * interaction with the Sync Job service.
 */

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

/**
 * @class SyncJobPageComponent
 * @description Angular component for managing Sync Jobs. Provides a table view of Sync Jobs and
 * functionality for creating, editing, deploying, and deleting Sync Jobs.
 */
@Component({
  selector: 'app-sync-job-page',
  imports: [
    Button,
    SyncJobCreationComponent,
    TableModule,
    Select,
    Tag,
    FormsModule,
    NgIf,
    NgIf,
  ],
  templateUrl: './sync-job-page.component.html',
  standalone: true,
  styleUrl: './sync-job-page.component.css'
})
export class SyncJobPageComponent implements OnInit {
  /**
   * @property {boolean} showCreateDialog - Controls the visibility of the Sync Job creation dialog.
   */
  showCreateDialog: boolean = false;

  /**
   * @property {any} selectedStatus - Represents the currently selected status filter for Sync Jobs.
   */
  public selectedStatus: any = null;

  /**
   * @property {SyncJob[]} items - List of Sync Jobs displayed in the table.
   */
  items: SyncJob[] = [];

  /**
   * @property {boolean} loading - Indicates whether data is being loaded.
   */
  loading: boolean = false;

  /**
   * @property {Array<{label: string, value: boolean}>} statuses - List of statuses for filtering Sync Jobs.
   */
  statuses = [
    { label: 'Active', value: false }, // false for "active"
    { label: 'In Simulation', value: true } // true for "In Simulation"
  ];

  /**
   * @property {number | undefined} selectedSyncJobId - ID of the currently selected Sync Job for editing.
   */
  selectedSyncJobId: number | undefined = undefined;

  /**
   * @property {any} deployingId - ID of the Sync Job currently being deployed.
   */
  deployingId: any;

  /**
   * @property {any} undeployingId - ID of the Sync Job currently being undeployed.
   */
  undeployingId: any;

  /**
   * Determines the severity level for a Sync Job based on its simulation status.
   * @param {boolean} isSimulation - Indicates whether the Sync Job is in simulation mode.
   * @returns {string} Severity level ('warning' for simulation, 'success' for active).
   */
  getSeverity(isSimulation: boolean): string {
    return isSimulation ? 'warning' : 'success';
  }

  /**
   * Opens the Sync Job creation dialog and navigates to the creation route.
   */
  openCreateDialog() {
    this.showCreateDialog = true;
    this.router.navigate(['sync-jobs/create']);
  }

  /**
   * @constructor
   * @param {SyncJobService} syncJobService - Service for managing Sync Jobs.
   * @param {HttpErrorService} httpErrorService - Service for handling HTTP errors.
   * @param {Router} router - Angular Router for navigation.
   */
  constructor(
    readonly syncJobService: SyncJobService,
    readonly httpErrorService: HttpErrorService,
    private router: Router,
  ) {}

  /**
   * Lifecycle hook that is called after the component is initialized.
   * Fetches all Sync Jobs and sets up navigation monitoring.
   */
  ngOnInit() {
    this.getAll();
    // Monitor navigation events and fetch Sync Jobs on return to the page
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd && event.url === '/sync-jobs') {
        this.getAll();
        this.selectedSyncJobId = undefined;
      }
    });
  }

  /**
   * Fetches all Sync Jobs from the service and updates the table data.
   */
  getAll() {
    console.log("GET ALL");
    this.syncJobService.getAll().subscribe({
      next: data => {
        console.log(data);
        this.items = data;
      },
      error: err => {
        console.log(err);
        this.httpErrorService.handleError(err);
      }
    });
  }

  /**
   * Opens the Sync Job editing dialog for the specified Sync Job.
   * @param {SyncJob} item - The Sync Job to edit.
   */
  edit(item: SyncJob) {
    // this.selectedSyncJobId = item.id;
    // this.showCreateDialog = true;
    this.router.navigate(['sync-jobs/', item.id]);
  }

  /**
   * Deletes the specified Sync Job using the service.
   * @param {SyncJob} item - The Sync Job to delete.
   */
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

  /**
   * Deploys the specified Sync Job by updating its deployed status.
   * @param {any} item - The Sync Job to deploy.
   */
  deploy(item: any) {
    this.deployingId = item.id;
    this.syncJobService.update(item.id, { ...item, deployed: true }).subscribe({});
    setTimeout(() => {
      this.deployingId = null;
      this.getAll();
    }, 5000); // 5 seconds
  }

  /**
   * Undeploys the specified Sync Job by updating its deployed status.
   * @param {any} item - The Sync Job to undeploy.
   */
  undeploy(item: any) {
    this.undeployingId = item.id;
    this.syncJobService.update(item.id, { ...item, deployed: false }).subscribe({});
    setTimeout(() => {
      this.undeployingId = null;
      this.getAll();
    }, 5000); // 5 seconds
  }
}
