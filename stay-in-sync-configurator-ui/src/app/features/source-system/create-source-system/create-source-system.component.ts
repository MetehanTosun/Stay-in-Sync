import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms'; // NgForm importiert
// biome-ignore lint/style/useImportType: <explanation>
import { AasService, AasShell } from '../../../../app/aas.service'; // AasShell importiert, Pfad anpassen
import { Observable, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown'; // DropdownModule importiert

type SourceType = 'AAS' | 'REST';

interface SourceSystemData {
  name: string;
  sourceType: SourceType; // Hinzugefügt, um den Typ mitzusenden
  aasId?: string;
  endpoint?: string;
}

@Component({
  selector: 'app-create-source-system',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    DropdownModule
  ],
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css']
})
export class CreateSourceSystemComponent implements OnInit {
  @Output() saved = new EventEmitter<SourceSystemData>();
  @Output() cancelled = new EventEmitter<void>();

  selectedSourceType: SourceType = 'AAS'; // Initialwert
  source = {
    name: '',
    aasId: '', // Wird nur verwendet, wenn sourceType === 'AAS'
    endpoint: '' // Wird nur verwendet, wenn sourceType === 'REST'
  };

  sourceTypeOptions = [
    { label: 'AAS Instance', value: 'AAS' },
    { label: 'Generic REST API', value: 'REST' }
  ];

  aasList: AasShell[] = []; // Für p-dropdown
  isLoadingAas = false;

  constructor(private aasService: AasService) {
    console.log('CreateSourceSystemComponent_ctor: initial sourceType=', this.selectedSourceType);
  }

  ngOnInit(): void {
    console.log('CreateSourceSystemComponent ngOnInit');
    this.onTypeChange(); // Initialisiere basierend auf dem Standard-Typ
  }

  private loadAasList(): void {
    this.isLoadingAas = true;
    this.aasService.getAll().pipe(
      tap(list => {
        this.aasList = list;
        console.log('AAS list loaded for dropdown:', this.aasList);
        this.isLoadingAas = false;
      }),
      catchError(err => {
        console.error('AasService.getAll() failed:', err);
        this.aasList = [];
        this.isLoadingAas = false;
        return of([]); // Fehler behandeln und leeres Array zurückgeben
      })
    ).subscribe();
  }

  onTypeChange() {
    console.log('Type changed to:', this.selectedSourceType);
    if (this.selectedSourceType === 'AAS') {
      this.loadAasList();
      this.source.endpoint = ''; // Endpoint zurücksetzen
    } else {
      this.aasList = [];
      this.source.aasId = ''; // aasId zurücksetzen
    }
  }

  save() {
    if (this.selectedSourceType === 'AAS' && !this.source.aasId) {
      console.warn('AAS ID is required for AAS type.');
      return; // Zusätzliche Validierung, falls p-dropdown 'required' nicht greift
    }
    if (this.selectedSourceType === 'REST' && !this.source.endpoint) {
      console.warn('Endpoint is required for REST type.');
      return;
    }

    const dataToSave: SourceSystemData = {
      name: this.source.name,
      sourceType: this.selectedSourceType,
      aasId: this.selectedSourceType === 'AAS' ? this.source.aasId : undefined,
      endpoint: this.selectedSourceType === 'REST' ? this.source.endpoint : undefined
    };

    console.log('Saving new source system with data:', dataToSave);
    this.saved.emit(dataToSave);
  }

  cancel() {
    console.log('Create source system cancelled by user.');
    this.cancelled.emit();
  }
}