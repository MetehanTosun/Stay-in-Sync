import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';


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

// passe die Pfade ggf. an deine Struktur an:
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
    InputTextModule,
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
  styleUrls: ['./edc-instances.component.css'],
  providers: [ConfirmationService],
})
export class EdcInstancesComponent implements OnInit {
  @ViewChild('dt2') dt2: Table | undefined;

  edcInstances: EdcInstance[] = [];
  allBpns: string[] = [];
  loading = true;

  // Dialog: Neu anlegen
  displayNewInstanceDialog = false;
  newInstance: EdcInstance = this.createEmptyInstance();

  // Dialog: Bearbeiten
  displayEditInstanceDialog = false;
  instanceToEdit: EdcInstance | null = null;

  constructor(
    private edcInstanceService: EdcInstanceService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.loadInstances();
  }

  private loadInstances(): void {
    this.loading = true;
    this.edcInstanceService.getAll().then((instances: EdcInstance[]) => {
      this.edcInstances = instances;
      const bpnSet: Set<string> = new Set(instances.map(i => i.bpn || '—'));
      this.allBpns = Array.from(bpnSet);
      this.loading = false;
    }).catch(err => {
      console.error('Fehler beim Laden der EDC-Instanzen:', err);
      this.loading = false;
    });
  }

  private createEmptyInstance(): EdcInstance {
    return {
      id: '',             // wird vom Backend gesetzt, bleibt hier leer
      name: '',
      url: '',
      protocolVersion: '',
      description: '',
      bpn: '',
      apiKey: '',         // write-only; wird nicht zurückgegeben
    };
  }

  // Filter / Tabelle
  onGlobalFilter(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.dt2?.filterGlobal(inputElement.value, 'contains');
  }

  clear(table: Table): void {
    table.clear();
  }

  // Neu anlegen
  openNewInstanceDialog(): void {
    this.newInstance = this.createEmptyInstance();
    this.displayNewInstanceDialog = true;
  }

  hideNewInstanceDialog(): void {
    this.displayNewInstanceDialog = false;
  }

  saveNewInstance(): void {
    const m = this.newInstance;
    if (!m.name || !m.url) {
      console.error('Name und URL sind Pflichtfelder.');
      return;
    }
    // WICHTIG: kein temp-ID-Trick mehr – direkt ans Backend
    this.edcInstanceService.create$(m).subscribe({
      next: (created) => {
        this.edcInstances = [created, ...this.edcInstances];
        this.hideNewInstanceDialog();
      },
      error: (err) => {
        console.error('Fehler beim Anlegen:', err);
      }
    });
  }

  // Bearbeiten
  editInstance(instance: EdcInstance): void {
    this.instanceToEdit = { ...instance };
    this.displayEditInstanceDialog = true;
  }

  hideEditInstanceDialog(): void {
    this.displayEditInstanceDialog = false;
    this.instanceToEdit = null;
  }

  saveEditedInstance(): void {
    const m = this.instanceToEdit;
    if (!m) return;
    if (!m.id) {
      console.error('ID fehlt beim Update.');
      return;
    }
    if (!m.name || !m.url) {
      console.error('Name und URL sind Pflichtfelder.');
      return;
    }
    this.edcInstanceService.update$(m.id, m).subscribe({
      next: (updated) => {
        this.edcInstances = this.edcInstances.map(x => x.id === updated.id ? updated : x);
        this.hideEditInstanceDialog();
      },
      error: (err) => {
        console.error('Fehler beim Aktualisieren:', err);
      }
    });
  }

  // Löschen
  deleteInstance(instance: EdcInstance): void {
    this.confirmationService.confirm({
      message: `Möchtest du "${instance.name}" wirklich löschen?`,
      header: 'Löschen bestätigen',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.edcInstanceService.delete$(instance.id).subscribe({
          next: () => {
            this.edcInstances = this.edcInstances.filter(i => i.id !== instance.id);
          },
          error: (err) => {
            console.error('Fehler beim Löschen:', err);
          }
        });
      },
    });
  }
}
