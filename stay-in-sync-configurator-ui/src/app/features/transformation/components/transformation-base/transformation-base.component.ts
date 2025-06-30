import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Transformation} from '../../models/transformation.model';
import {Button} from 'primeng/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import {min} from 'rxjs';
import {TransformationStateService} from '../../services/ransformation.state.service';
import {TransformationService} from '../../services/transformation.service';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-transformation-base',
  imports: [
    TableModule,
    Button,
    FormsModule,
    ToggleButtonModule,
    Dialog,
    ReactiveFormsModule,
    InputText,
    NgIf
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

  displayCreateDialog: boolean = false;

  newTransformation: Transformation = {};

  constructor(private stateService: TransformationStateService, private transformationService : TransformationService) {}

  ngOnInit() {
    const loaded = this.stateService.transformations;
    this.transformations = loaded && loaded.length > 0 ? loaded : [
    ];
  }

  edit(rowData: any) {
    // Implement edit functionality here
    //TODO:backend
    console.log('Edit transformation:', rowData);
  }

  delete(rowData: any) {
    this.transformationService.delete(rowData).subscribe();
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

  createTransformationShell() {
    console.log('Creating new transformation shell');
    this.transformationService.create(this.newTransformation).subscribe({
      next: (transformation: Transformation) => {
        this.transformations.push(transformation);
        this.newTransformation = {};
        this.displayCreateDialog = false
    }
      , error: (error: any) => {
        console.error('Fehler beim Erstellen der Transformation:', error);
      }
    });
  }

  openCreateDialog() {
    this.displayCreateDialog = true;
  }

  cancel() {
    this.displayCreateDialog = false;
  }

  addRule(rowData: any) {

  }

  addScript(rowData: any) {

  }

  showMissingFieldsMessage() {
    alert('Bitte füllen Sie alle erforderlichen Felder aus.');
  }
}
