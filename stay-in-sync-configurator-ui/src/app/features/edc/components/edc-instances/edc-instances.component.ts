import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // For ngModel

// PrimeNG
import { Table, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
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
    PasswordModule,
  ],
  templateUrl: './edc-instances.component.html',
  styleUrl: './edc-instances.component.css',
  providers: [ConfirmationService],
})
export class EdcInstancesComponent implements OnInit {
  @ViewChild('dt2') dt2: Table | undefined;

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
    private confirmationService: ConfirmationService
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
      this.loading = false;
    }
  });
}



  private createEmptyInstance(): EdcInstance {
    return {
      id: '',
      name: '',
      url: '',
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
  if (this.newInstance.name && this.newInstance.url && this.newInstance.bpn) {
    this.edcInstanceService.createEdcInstance(this.newInstance).subscribe({
      next: (created) => {
        this.edcInstances = [...this.edcInstances, created];
        this.hideNewInstanceDialog();
      },
      error: (err) => {
        console.error('Fehler beim Speichern einer neuen Instanz', err);
      }
    });
  } else {
    console.error('Name, URL, and BPN are required.');
  }
}


  editInstance(instance: EdcInstance): void {
    this.instanceToEdit = { ...instance };
    this.displayEditInstanceDialog = true;
  }

  hideEditInstanceDialog(): void {
    this.displayEditInstanceDialog = false;
    this.instanceToEdit = null; // Clear the instance being edited
  }

  saveEditedInstance(): void {
  if (this.instanceToEdit && this.instanceToEdit.name && this.instanceToEdit.url && this.instanceToEdit.bpn) {
    this.edcInstanceService.updateEdcInstance(this.instanceToEdit.id, this.instanceToEdit).subscribe({
      next: (updated) => {
        this.edcInstances = this.edcInstances.map(instance =>
          instance.id === updated.id ? updated : instance
        );
        this.hideEditInstanceDialog();
      },
      error: (err) => {
        console.error('Fehler beim Aktualisieren der Instanz', err);
      }
    });
  } else {
    console.error('Name, URL, and BPN are required for edited instance.');
  }
}

deleteInstance(instance: EdcInstance): void {
  this.confirmationService.confirm({
    message: `Are you sure you want to delete the instance "${instance.name}"?`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    acceptButtonStyleClass: 'p-button-danger',
    rejectButtonStyleClass: 'p-button-text',
    accept: () => {
      this.edcInstanceService.deleteEdcInstance(instance.id).subscribe({
        next: () => {
          this.edcInstances = this.edcInstances.filter(i => i.id !== instance.id);
        },
        error: (err) => {
          console.error('Fehler beim Löschen der Instanz', err);
        }
      });
    },
  });
}
}