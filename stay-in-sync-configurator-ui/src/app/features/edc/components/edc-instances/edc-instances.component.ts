import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // For ngModel

// PrimeNG Modules needed for the table features
import { Table, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select'; // For p-select
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { RippleModule } from 'primeng/ripple'; // <-- ADD THIS
import { TooltipModule } from 'primeng/tooltip'; // <-- ADD THIS
// If you use <theme-switcher /> directly, you'd import it here.
// import { ThemeSwitcher } from './themeswitcher'; // Adjust path if you use it

// Your application-specific imports
import { EdcInstance } from './models/edc-instance.model';
import { EdcInstanceService } from './services/edc-instance.service';

@Component({
  selector: 'app-edc-instances',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule, // For ngModel in p-select filter
    TableModule,
    InputTextModule,
    SelectModule,
    TagModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    RippleModule,
    TooltipModule,
   // ThemeSwitcher,
  ],
  templateUrl: './edc-instances.component.html',
  styleUrl: './edc-instances.component.css',
  // providers: [EdcInstanceService] // EdcInstanceService is already providedIn: 'root'
})
export class EdcInstancesComponent implements OnInit {
  @ViewChild('dt2') dt2: Table | undefined; // For accessing table methods like filterGlobal

  edcInstances: EdcInstance[] = [];
  statuses: { label: string, value: string }[] = [];
  loading: boolean = true;

  constructor(private edcInstanceService: EdcInstanceService) {}

  ngOnInit(): void {
    this.edcInstanceService.getEdcInstancesLarge().then((data) => {
      this.edcInstances = data;
      this.loading = false;
    });

    this.statuses = [
      { label: 'Active', value: 'Active' },
      { label: 'Inactive', value: 'Inactive' },
      { label: 'Pending', value: 'Pending' },
    ];
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

  getSeverity(status: string): string {
    switch (status?.toLowerCase()) {
      case 'inactive':
        return 'danger';
      case 'active':
        return 'success';
      case 'pending':
        return 'warning';
      default:
        return 'info';
    }
  }

  editInstance(instance: EdcInstance): void {
    console.log('Edit instance:', instance);
  }

  deleteInstance(instance: EdcInstance): void {
    console.log('Delete instance:', instance);
  }
}
