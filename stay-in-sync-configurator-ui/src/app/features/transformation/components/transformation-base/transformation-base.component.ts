import {Component, EventEmitter, Output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Transformation} from '../../models/transformation.model';
import {Button} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import {min} from 'rxjs';

@Component({
  selector: 'app-transformation-base',
  imports: [
    TableModule,
    Button,
    FormsModule,
    ToggleButtonModule
  ],
  templateUrl: './transformation-base.component.html',
  styleUrl: './transformation-base.component.css'
})
export class TransformationBaseComponent {
  @Output() transformationsChanged = new EventEmitter<Transformation[]>();

  transformations: Transformation[] = [{
    name: 'Beispieltransformation',
    transformationRule: 'Rule1',
    transformationScript: 'Script1',
  }];
  addedTransformations: Transformation[] = [];
  checked: boolean = false;

  edit(rowData: any) {
    // Implement edit functionality here
    console.log('Edit transformation:', rowData);
  }

  delete(rowData: any) {
    // Implement delete functionality here
    console.log('Delete transformation:', rowData);
  }

  add(rowData: any) {
    // Transformation hinzufügen und Event an Parent senden
    rowData.added = true;
    if (!this.transformations.some(t => t.name === rowData.name)) {
      this.transformations.push(rowData);
    }
    this.transformationChanged();
  }

  remove(rowData: any) {
    rowData.added = false;
    this.addedTransformations = this.addedTransformations.filter(t => t.name !== rowData.name);
    this.transformationChanged();
  }

  /**
   * Sendet ein Event an die Parent-Komponente, um Änderungen mitzuteilen.
   */
  transformationChanged() {
    this.transformationsChanged.emit(this.addedTransformations)
    ;
  }

  protected readonly min = min;
}
