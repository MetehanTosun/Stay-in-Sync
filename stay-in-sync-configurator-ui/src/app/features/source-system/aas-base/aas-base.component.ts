import { Component } from '@angular/core';
import { RouterModule } from '@angular/router'; // RouterModule importieren
import { ButtonModule } from 'primeng/button'; // PrimeNG ButtonModule importieren

@Component({
  selector: 'app-ass-base',
  standalone: true,
  imports: [RouterModule, ButtonModule], // RouterModule hinzufügen
  templateUrl: './aas-base.component.html',
  styleUrls: ['./aas-base.component.css']
})
export class AasBaseComponent {}
