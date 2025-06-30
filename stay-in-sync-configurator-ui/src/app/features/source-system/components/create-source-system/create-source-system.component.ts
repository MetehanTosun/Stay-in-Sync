// src/app/features/source-system/components/create-source-system/create-source-system.component.ts

import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule }    from '@angular/common';
import { FormsModule }     from '@angular/forms';
import { ButtonModule }    from 'primeng/button';
import { DialogModule }    from 'primeng/dialog';
import { DropdownModule }  from 'primeng/dropdown';         // ← hier
import { AasService }      from '../../services/aas.service';
import { Observable, of }  from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

type SourceType = 'AAS' | 'REST';

@Component({
  selector: 'app-create-source-system',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    DialogModule,
    DropdownModule                           // ← hier
  ],
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css']
})
export class CreateSourceSystemComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  sourceType: SourceType = 'AAS';
  sourceTypeOptions = [
    { label: 'AAS',  value: 'AAS'  },
    { label: 'REST', value: 'REST' }
  ];

  source = { name: '', aasId: '', endpoint: '' };

  aasList$: Observable<{ id: string; name: string }[]> = of([]);

  constructor(private aasService: AasService) {}

  ngOnInit(): void {
    this.loadAasList();
  }

  onTypeChange(type: SourceType) {
    this.sourceType = type;
    this.loadAasList();
  }

  private loadAasList() {
    if (this.sourceType === 'AAS') {
      this.aasList$ = this.aasService.getAll().pipe(
        tap(list => console.log('AAS geladen:', list)),
        catchError(err => { console.error(err); return of([]); })
      );
    } else {
      this.aasList$ = of([]);
    }
  }

  open() {
    this.visible = true;
    this.visibleChange.emit(true);
  }

  cancel() {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  save() {
    console.log('Speichern:', this.source);
    // TODO: Save-Logik
    this.cancel();
  }
}