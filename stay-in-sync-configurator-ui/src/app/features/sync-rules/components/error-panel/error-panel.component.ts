import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges } from '@angular/core';
import { ValidationError } from '../../models/interfaces/validation-error.interface';

@Component({
  selector: 'app-error-panel',
  imports: [CommonModule],
  templateUrl: './error-panel.component.html',
  styleUrl: './error-panel.component.css'
})
export class ErrorPanelComponent {
  @Input() errors: ValidationError[] = [];
  open = false;

  togglePanel() {
    this.open = !this.open;
  }
}
