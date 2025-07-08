import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Transformation} from '../../models/transformation.model';
import {Button} from 'primeng/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import {min} from 'rxjs';
import {
  TransformationTempStoreService
} from '../../services/transformation.tempstore.service';
import {TransformationService} from '../../services/transformation.service';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {NgIf} from '@angular/common';
import {Router} from '@angular/router';
import {
  TransformationScriptSelectionComponent
} from '../transformation-script-selection/transformation-script-selection.component';
import {
  TransformationRuleSelectionComponent
} from '../transformation-rule-selection/transformation-rule-selection.component';

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
    NgIf,
    TransformationScriptSelectionComponent,
    TransformationRuleSelectionComponent
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
  selectedTransformation: Transformation ={};

  displayCreateDialog: boolean = false;
  displayRuleSelectionDialog: boolean = false;
  displayScriptSelectionDialog: boolean = false;

  newTransformation: Transformation = {};

  constructor(private transformationService : TransformationService, private router: Router, private tempStore: TransformationTempStoreService) {}

  ngOnInit() {
    this.loadTransformationsFromBackend();
  }

  loadTransformationsFromBackend() {
    this.transformationService.getAll().subscribe(
        (transformations: Transformation[]) => {
            this.transformations = transformations;
            console.log('Transformations from backend loaded:', this.transformations);
        },
        (error: any) => {
            console.error('Error loading transformations from backend:', error);
        }
    )

  }

  edit(rowData: any) {
    // Implement edit functionality here
    //TODO:backend
    console.log('Edit transformation:', rowData);
  }

  delete(rowData: any) {
    this.transformationService.delete(rowData).subscribe({
      next: () => this.loadTransformationsFromBackend(),
      error: (error: any) => console.error('Error deleting transformation:', error)
    });
    console.log('Delete transformation:', rowData);
  }

  add(rowData: any) {
    rowData.added = true;
    this.tempStore.addTransformation(rowData);
    this.addedTransformations = this.tempStore.getTransformations();
    this.transformationChanged();
  }

  remove(rowData: any) {
    rowData.added = false;
    this.tempStore.removeTransformation(rowData);
    this.addedTransformations = this.tempStore.getTransformations();
    this.transformationChanged();
  }

  /**
   * Sendet ein Event an die Parent-Komponente, um Änderungen mitzuteilen.
   */
  transformationChanged() {
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

  selectedRule: any; // Typ ggf. anpassen
  private _transformationId: number | null = null;

  get transformationId(): number | null {
    return this._transformationId;
  }

  set transformationId(value: number | null) {
    this._transformationId = value;
  }



  addRule(rowData: any) {
    this.selectedTransformation = rowData;
    this.displayRuleSelectionDialog = true;
    this.router.navigate([`/sync-jobs/create/rule/${rowData.id}`]);
    console.log('Selected transformation for rule:', rowData.id);
    this.transformationId = rowData.id;
  }

  onUseRuleEvent(event: any) {
    console.log('Selected rule:', event);
    this.selectedRule = event;
    //this.transformationService.update()
  }

  addScript(rowData: any) {
    this.selectedTransformation = rowData;
    this.displayScriptSelectionDialog = true;
    this.router.navigate([`/sync-jobs/create/script/${rowData.id}`]);
    this.transformationId = rowData.id;
  }

  showMissingFieldsMessage() {
    alert('Bitte füllen Sie alle erforderlichen Felder aus.');
  }

  cancelRuleSelectionDialog() {
    this.displayRuleSelectionDialog = false;
    this.router.navigate([`/sync-jobs/create`]);
  }
  cancelCreateTransformationDialog() {
    this.displayCreateDialog = false;
  }

  cancelScriptSelectionDialog() {
    this.displayScriptSelectionDialog = false;
    this.router.navigate([`/sync-jobs/create`]);
  }
}
