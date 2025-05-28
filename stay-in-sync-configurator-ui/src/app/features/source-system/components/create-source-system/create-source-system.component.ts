// src/app/aas/aas-base/create-source-system/create-source-system.component.ts

// biome-ignore lint/style/useImportType: <explanation>
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';
// biome-ignore lint/style/useImportType: <explanation>
import { AasService } from '../../services/aas.service';

type SourceType = 'AAS' | 'REST';

@Component({
  selector: 'app-create-source-system',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
})
export class CreateSourceSystemComponent implements OnInit {
  /** Welcher Typ von Source System soll angelegt werden? */
  sourceType: SourceType = 'AAS';

  /** Datenmodell für das neue Quell-System */
  source = {
    name: '',
    /** AAS-Instanz-ID (falls AAS) */
    aasId: '',
    /** Endpoint URL (falls REST-API) */
    endpoint: '',
  };

  /** Liste aller AAS-Instanzen für das Dropdown */
  aasList$: Observable<{ id: string; name: string }[]> = of([]);

  constructor(private aasService: AasService) {}

  ngOnInit(): void {
    // Initial laden, falls Default-Typ AAS ist
    if (this.sourceType === 'AAS') {
      this.loadAasList();
    }
  }

  /** Wird aufgerufen, wenn der Nutzer den Typ wechselt */
  onTypeChange(type: SourceType) {
    this.sourceType = type;
    // AAS-Liste ggf. (neu) laden oder leeren
    this.aasList$ = type === 'AAS'
      ? this.aasService.getAll()
      : of([]);
    // Eingabefelder zurücksetzen
    this.source.aasId = '';
    this.source.endpoint = '';
  }

  private loadAasList() {
    this.aasList$ = this.aasService.getAll();
  }

  /** Speichern-Handler */
  save(): void {
    // Einfache Validierung
    if (!this.source.name) {
      console.warn('Name is required');
      return;
    }
    if (this.sourceType === 'AAS' && !this.source.aasId) {
      console.warn('AAS ID is required');
      return;
    }
    if (this.sourceType === 'REST' && !this.source.endpoint) {
      console.warn('Endpoint URL is required');
      return;
    }

    // Hier würde man den Backend-Call implementieren:
    console.log('Creating Source System:', {
      type: this.sourceType,
      ...this.source
    });
    // z.B. this.aasService.create(this.source).subscribe(...)
  }
}
