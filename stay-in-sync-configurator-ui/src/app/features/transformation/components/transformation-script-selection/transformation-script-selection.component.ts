import {Component, EventEmitter, Output} from '@angular/core';
import {Button} from "primeng/button";
import {PrimeTemplate} from "primeng/api";
import {TableModule} from "primeng/table";

@Component({
  selector: 'app-transformation-script-selection',
    imports: [
        Button,
        PrimeTemplate,
        TableModule
    ],
  templateUrl: './transformation-script-selection.component.html',
  styleUrl: './transformation-script-selection.component.css'
})
export class TransformationScriptSelectionComponent {
  @Output() closeDialog = new EventEmitter<void>();
  items: any[] = [];

  viewDetails(item: any) {
    // Implementiere die Logik, um Details des ausgewählten Elements anzuzeigen
    console.log('Viewing details for:', item);
  }

  useItem(item: any) {
    // Implementiere die Logik, um das ausgewählte Element zu verwenden
    console.log('Using item:', item);
    this.closeDialog.emit();
  }
}
