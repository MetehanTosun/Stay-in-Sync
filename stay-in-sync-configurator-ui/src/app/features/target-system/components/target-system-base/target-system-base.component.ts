import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';

@Component({
  standalone: true,
  selector: 'app-target-system-base',
  template: `
    <p-card header="Target Systems">
      <p>Hier entsteht das Target-System-Management (CRUD und Endpoints) analog zum Source-System.</p>
    </p-card>
  `,
  imports: [CommonModule, CardModule]
})
export class TargetSystemBaseComponent {}


