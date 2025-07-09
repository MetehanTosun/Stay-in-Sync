import {Component, EventEmitter, Input, numberAttribute, Output} from '@angular/core';
import {Button} from "primeng/button";
import {PrimeTemplate} from "primeng/api";
import {TableModule} from "primeng/table";
import {TransformationService} from '../../services/transformation.service';
import {Transformation} from '../../models/transformation.model';
import {ActivatedRoute} from '@angular/router';

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
  @Input({transform: numberAttribute}) transformationId: number | null = null;
  items: any[] = [{name: 'Rule1'},];
  selectedTransformation: Transformation = {};


  constructor(private transformationService: TransformationService) {
  }

viewDetails(item: any) {
  // Implementiere die Logik, um Details des ausgewählten Elements anzuzeigen
  console.log('Viewing details for:', item);
}



  useItem(item: any) {
    console.log('Using item:', item, this.transformationId);
    if (this.transformationId !== null) {
      this.transformationService.getById(this.transformationId).subscribe(transformation => {
        this.selectedTransformation = transformation;
        this.selectedTransformation.transformationRule = item;
        this.transformationService.update(transformation);
        this.closeDialog.emit();
      });
    }
  }

  createNewRule() {
    // Implementiere die Logik, um eine neue Regel zu erstellen
    console.log('Creating a new rule');
    //
  }
}
