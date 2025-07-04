import {Component, EventEmitter, Output} from '@angular/core';
import {Button} from "primeng/button";
import {PrimeTemplate} from "primeng/api";
import {TableModule} from "primeng/table";

@Component({
  selector: 'app-transformation-rule-selection',
    imports: [
        Button,
        PrimeTemplate,
        TableModule
    ],
  templateUrl: './transformation-rule-selection.component.html',
  styleUrl: './transformation-rule-selection.component.css'
})
export class TransformationRuleSelectionComponent {
  @Output() closeDialog = new EventEmitter<void>();
  @Output() useItemEvent = new EventEmitter<any>();
  items: any[] = [{name: 'Rule1'},];

viewDetails(item: any) {
  // Implementiere die Logik, um Details des ausgewählten Elements anzuzeigen
  console.log('Viewing details for:', item);
}

  useItem(item: any) {
    this.useItemEvent.emit(item);
    this.closeDialog.emit();
  }
}
