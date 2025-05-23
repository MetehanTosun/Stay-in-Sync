// src/app/features/source-system/source-system-base/create-source-system.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
// biome-ignore lint/style/useImportType: <explanation>
import { AasService } from '../../../../app/aas.service';
import { Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

type SourceType = 'AAS' | 'REST';

@Component({
  selector: 'app-create-source-system',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css']
})
export class CreateSourceSystemComponent implements OnInit {
  /** Welcher Typ von Source System soll angelegt werden? */
  sourceType: SourceType = 'AAS';

  /** Datenmodell für das neue Quell-System */
  source = {
    name: '',
    aasId: '',
    endpoint: ''
  };

  /** Liste aller AAS-Instanzen für das Dropdown */
  aasList$: Observable<{ id: string; name: string }[]> = of([]);

  constructor(private aasService: AasService) {
    console.log('CreateSourceSystemComponent_ctor: sourceType=', this.sourceType);
  }

  ngOnInit(): void {
    // Nur beim AAS-Typ laden wir die Liste
    if (this.sourceType === 'AAS') {
      this.aasList$ = this.aasService.getAll().pipe(
        tap(list => console.log('AAS list loaded:', list)),
        catchError(err => {
          console.error('AasService.getAll() failed:', err);
          return of([]);
        })
      );
    }
  }

  /** Wird aufgerufen, wenn der Nutzer den Typ wechselt */
  onTypeChange(type: SourceType) {
    this.sourceType = type;
    if (type === 'AAS') {
      this.aasList$ = this.aasService.getAll().pipe(
        tap(list => console.log('AAS list re-loaded:', list)),
        catchError(err => {
          console.error('AasService.getAll() failed:', err);
          return of([]);
        })
      );
    } else {
      this.aasList$ = of([]);
    }
  }

  save() {
    console.log('Saving new source system:', this.source);
    // TODO: hier echtes Save-API implementieren
  }
}