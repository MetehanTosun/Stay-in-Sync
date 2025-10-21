import { Component, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // For ngModel

// PrimeNG
import { Table, TableModule, TableRowSelectEvent } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { PasswordModule } from 'primeng/password';

import { EdcInstance } from './models/edc-instance.model';
import { EdcInstanceService } from './services/edc-instance.service';


@Component({
  selector: 'app-edc-instances',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    InputTextModule,
    InputTextarea,
    TagModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    RippleModule,
    TooltipModule,
    DialogModule,
    ConfirmDialogModule,
    ToastModule,
    PasswordModule,
  ],
  templateUrl: './edc-instances.component.html',
  styleUrl: './edc-instances.component.css',
  providers: [ConfirmationService, MessageService],
})
export class EdcInstancesComponent implements OnInit {
  @ViewChild('dt2') dt2: Table | undefined;

  @Output() instanceSelected = new EventEmitter<EdcInstance>();

  edcInstances: EdcInstance[] = [];
  loading: boolean = true;

  // Dialog related properties
  displayNewInstanceDialog: boolean = false;
  newInstance: EdcInstance = this.createEmptyInstance(); // To hold form data

  // Edit instance dialog properties
  displayEditInstanceDialog: boolean = false;
  instanceToEdit: EdcInstance | null = null; // Instance being edited

  constructor(
    private edcInstanceService: EdcInstanceService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
  this.loading = true;
  this.edcInstanceService.getEdcInstances().subscribe({
    next: (data: EdcInstance[]) => {
      this.edcInstances = data;
      this.loading = false;
    },
    error: (err) => {
      console.error('Fehler beim Laden der EDC-Instanzen', err);
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Could not load EDC instances.' });
      this.loading = false;
    }
  });
}



  private createEmptyInstance(): EdcInstance {
    return {
      id: null, // Use null for new instances
      name: '',
      controlPlaneManagementUrl: '',
      protocolVersion: '',
      description: '',
      bpn: '',
      apiKey: '',
    };
  }

  onGlobalFilter(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    if (this.dt2) {
      this.dt2.filterGlobal(inputElement.value, 'contains');
    }
  }

  clear(table: Table): void {
    table.clear();
  }

  openNewInstanceDialog(): void {
    this.newInstance = this.createEmptyInstance();
    this.displayNewInstanceDialog = true;
  }

  hideNewInstanceDialog(): void {
    this.displayNewInstanceDialog = false;
  }

saveNewInstance(): void {
  if (this.newInstance.name && this.newInstance.controlPlaneManagementUrl && this.newInstance.bpn) {
    this.edcInstanceService.createEdcInstance(this.newInstance).subscribe({
      next: (created) => {
        this.edcInstances = [...this.edcInstances, created];
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Instance created successfully.' });
        this.hideNewInstanceDialog();
      },
      error: (err) => {
        console.error('Fehler beim Speichern einer neuen Instanz', err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to create instance.' });
      }
    });
  } else {
    this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'Name, Management URL, and BPN are required.' });
  }
}


  editInstance(instance: EdcInstance): void {
    // Make a deep copy of the instance
    this.instanceToEdit = { 
      ...instance
    };
    console.log(`Editing instance with ID: ${this.instanceToEdit.id || 'new'}`);
    this.displayEditInstanceDialog = true;
  }

  hideEditInstanceDialog(): void {
    this.displayEditInstanceDialog = false;
    this.instanceToEdit = null; // Clear the instance being edited
  }

  saveEditedInstance(): void {
  if (this.instanceToEdit && this.instanceToEdit.name && this.instanceToEdit.controlPlaneManagementUrl && this.instanceToEdit.bpn) {
    try {
      // Make sure we have an ID to update, or handle as new instance if not
      const idToUse = this.instanceToEdit.id !== undefined && this.instanceToEdit.id !== null 
        ? this.instanceToEdit.id 
        : null;
      
      console.log(`Updating instance with ID: ${idToUse}`);
      
      this.edcInstanceService.updateEdcInstance(idToUse, this.instanceToEdit).subscribe({
        next: (updated) => {
          if (idToUse) {
            // Update existing instance in the list
            this.edcInstances = this.edcInstances.map(instance =>
              instance.id === updated.id ? updated : instance
            );
          } else {
            // Add as new instance if it was created instead of updated
            this.edcInstances = [...this.edcInstances, updated];
          }
          
          this.messageService.add({ 
            severity: 'success', 
            summary: 'Success', 
            detail: idToUse ? 'Instance updated successfully.' : 'New instance created successfully.' 
          });
          this.hideEditInstanceDialog();
        },
        error: (err) => {
          console.error('Fehler beim Aktualisieren der Instanz', err);
          let errorDetail = 'Failed to update instance.';
          
          if (err.status === 404) {
            errorDetail = 'Instance not found. It might have been deleted.';
          } else if (err.message === 'Invalid ID format') {
            errorDetail = 'Invalid ID format. Please try again.';
          }
          
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorDetail });
        }
      });
    } catch (error) {
      console.error('Error preparing instance update:', error);
      this.messageService.add({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'An error occurred while preparing to update the instance.' 
      });
    }
  } else {
    this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'Name, Management URL, and BPN are required.' });
  }
}

deleteInstance(instance: EdcInstance): void {
  // Cannot delete an instance without an ID
  if (instance.id === null || instance.id === undefined) {
    this.messageService.add({ 
      severity: 'warn', 
      summary: 'Warning', 
      detail: 'Cannot delete an instance without an ID.' 
    });
    return;
  }
  
  this.confirmationService.confirm({
    message: `Are you sure you want to delete the instance "${instance.name}"?`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    acceptButtonStyleClass: 'p-button-danger',
    rejectButtonStyleClass: 'p-button-text',
    accept: () => {
      try {
        this.edcInstanceService.deleteEdcInstance(instance.id!.toString()).subscribe({
          next: () => {
            this.edcInstances = this.edcInstances.filter(i => i.id !== instance.id);
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Instance deleted successfully.' });
          },
          error: (err) => {
            console.error('Fehler beim Löschen der Instanz', err);
            let errorDetail = 'Failed to delete instance.';
            if (err.status === 404) {
              errorDetail = 'Instance not found. It might have been already deleted.';
            } else if (err.status === 500) {
              errorDetail = 'Server error while deleting instance. It might be referenced by other entities.';
            }
            this.messageService.add({ severity: 'error', summary: 'Error', detail: errorDetail });
          }
        });
      } catch (error) {
        console.error('Error preparing instance deletion:', error);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Invalid ID format.' });
      }
    },
  });
}

  onInstanceRowSelect(event: TableRowSelectEvent) {
    this.instanceSelected.emit(event.data);
  }
}