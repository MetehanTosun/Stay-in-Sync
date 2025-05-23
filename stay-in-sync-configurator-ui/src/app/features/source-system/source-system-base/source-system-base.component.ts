import { Component } from '@angular/core';
import { CommonModule, AsyncPipe, NgForOf, NgIf } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
// biome-ignore lint/style/useImportType: <explanation>
import { Observable } from 'rxjs';

// biome-ignore lint/style/useImportType: <explanation>
import { AasService } from '../../../aas.service';  // Passe den Pfad ggf. an

@Component({
  selector: 'app-source-system-base',
  standalone: true,
  imports: [
    CommonModule,
    NgIf,
    NgForOf,
    AsyncPipe,
    RouterModule,
    ButtonModule
  ],
  templateUrl: './source-system-base.component.html',
  styleUrls: ['./source-system-base.component.css']
})
export class SourceSystemBaseComponent {
  /** Stream aller registrierten Source Systems (insb. AAS Instanzen) */
  sourceSystems$: Observable<any[]>;

  constructor(private aasService: AasService) {
    // Direkt beim Start die Liste aller Systeme abrufen
    this.sourceSystems$ = this.aasService.getAll();
  }
}