import { Component } from '@angular/core';
import {TableModule} from 'primeng/table';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Tag} from 'primeng/tag';
import {InputText} from 'primeng/inputtext';
import {NgOptimizedImage} from '@angular/common';
import {ButtonDirective} from 'primeng/button';

@Component({
  selector: 'app-config-base',
  imports: [
    TableModule,
    IconField,
    InputIcon,
    Tag,
    InputText,
    NgOptimizedImage,
    ButtonDirective
  ],
  templateUrl: './config-base.component.html',
  styleUrl: './config-base.component.css'
})
export class ConfigBaseComponent {
  customers: any[] = [];
  selectedCustomers: any;
 private _status: any;

 getSeverity(status: any): any {
   this._status = status;
   return undefined;
 }

  protected readonly HTMLInputElement = HTMLInputElement;

  editConfig(customer: any) {
    // Hier können Sie die Logik zum Bearbeiten der Konfiguration hinzufügen
    console.log('Bearbeite Konfiguration für:', customer);
  }

  activateConfig(customer: any) {
    // Hier können Sie die Logik zum Aktivieren der Konfiguration hinzufügen
    console.log('Aktiviere Konfiguration für:', customer);
  }

  dryRun(customer: any) {
    // Hier können Sie die Logik für den Dry Run hinzufügen
    console.log('Führe Dry Run für:', customer);
  }
}
