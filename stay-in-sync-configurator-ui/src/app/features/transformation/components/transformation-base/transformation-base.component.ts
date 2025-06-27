import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Transformation} from '../../models/transformation.model';
import {Button} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import {min} from 'rxjs';
import {TransformationStateService} from '../../services/ransformation.state.service';

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
export class TransformationBaseComponent implements OnInit {
  @Output() transformationsChanged = new EventEmitter<Transformation[]>();

  transformations: Transformation[] = [{
    name: 'Beispieltransformation',
    transformationRule: 'Rule1',
    transformationScript: 'Script1',
  }];

  addedTransformations: Transformation[] = [];

  constructor(private stateService: TransformationStateService) {}

  ngOnInit() {
    const loaded = this.stateService.transformations;
    this.transformations = loaded && loaded.length > 0 ? loaded : [
      { name: 'Beispieltransformation 1', transformationRule: 'Rule1', transformationScript: 'Script1' },
      { name: 'Beispieltransformation 2', transformationRule: 'Rule2', transformationScript: 'Script2' },
      { name: 'Beispieltransformation 3', transformationRule: 'Rule3', transformationScript: 'Script3' },
      { name: 'Beispieltransformation 4', transformationRule: 'Rule4', transformationScript: 'Script4' },
      { name: 'Beispieltransformation 5', transformationRule: 'Rule5', transformationScript: 'Script5' },
      { name: 'Beispieltransformation 6', transformationRule: 'Rule6', transformationScript: 'Script6' },
      { name: 'Beispieltransformation 7', transformationRule: 'Rule7', transformationScript: 'Script7' },
      { name: 'Beispieltransformation 8', transformationRule: 'Rule8', transformationScript: 'Script8' },
      { name: 'Beispieltransformation 9', transformationRule: 'Rule9', transformationScript: 'Script9' },
      { name: 'Beispieltransformation 10', transformationRule: 'Rule10', transformationScript: 'Script10' },
      { name: 'Beispieltransformation 11', transformationRule: 'Rule11', transformationScript: 'Script11' },
      { name: 'Beispieltransformation 12', transformationRule: 'Rule12', transformationScript: 'Script12' }
    ];
  }

  edit(rowData: any) {
    // Implement edit functionality here
    //TODO:backend
    console.log('Edit transformation:', rowData);
  }

  delete(rowData: any) {
    // Implement delete functionality here
    //TODO:backend
    console.log('Delete transformation:', rowData);
  }

  add(rowData: any) {
    rowData.added = true;
    if (!this.addedTransformations.some(t => t.name === rowData.name)) {
      this.addedTransformations.push(rowData);
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
    this.stateService.transformations = this.transformations;
    console.log('Transformationen geändert:', this.addedTransformations);
    this.transformationsChanged.emit(this.addedTransformations);
  }

  protected readonly min = min;
}
