import { Component } from '@angular/core';
import { RouterModule } from '@angular/router'; // RouterModule importieren
import { ButtonModule } from 'primeng/button'; // PrimeNG ButtonModule importieren

@Component({
  selector: 'app-source-system-base',
  standalone: true,
  imports: [RouterModule, ButtonModule], // RouterModule hinzufügen
  templateUrl: './source-system-base.component.html',
  styleUrls: ['./source-system-base.component.css']
})
export class SourceSystemBaseComponent {}
